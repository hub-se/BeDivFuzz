package de.hub.se.jqf.bedivfuzz.guidance.baseline;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import de.hub.se.jqf.bedivfuzz.guidance.BeDivFuzzGuidance;
import de.hub.se.jqf.bedivfuzz.guidance.SplitGeneratorGuidance;
import de.hub.se.jqf.bedivfuzz.junit.quickcheck.tracking.SplitTrackingSourceOfRandomness;
import de.hub.se.jqf.bedivfuzz.junit.quickcheck.tracking.Choice;
import edu.berkeley.cs.jqf.fuzz.ei.ZestGuidance;
import edu.berkeley.cs.jqf.fuzz.guidance.GuidanceException;
import edu.berkeley.cs.jqf.fuzz.guidance.Result;
import edu.berkeley.cs.jqf.fuzz.junit.quickcheck.NonTrackingGenerationStatus;
import edu.berkeley.cs.jqf.fuzz.util.IOUtils;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.*;
import java.util.function.BiConsumer;

public class BeDivFuzzBaselineGuidance extends ZestGuidance implements SplitGeneratorGuidance {

    /** The set of all saved input structure hashes. */
    protected IntHashSet savedInputStructures = new IntHashSet();

    /** The mutation types that can be performed on the choice sequence. */
    protected enum Mutation {HAVOC, STRUCTURE, VALUE};

    /** The callback responsible for tracing the choice types for each saved input. */
    protected BiConsumer<SplitTrackingSourceOfRandomness, GenerationStatus> choiceTracer;

    /** The epsilon-greedy tradeoff between exploration and exploitation. */
    protected final double EPSILON = Double.parseDouble(System.getProperty("jqf.guidance.bedivfuzz.epsilon", "0.2"));

    /** Save only inputs that increase coverage and have a novel input structure. */
    protected final boolean STUCTURAL_FUZZING = Boolean.getBoolean("jqf.guidance.bedivfuzz.STRUCTUAL_FUZZING");

    /** Whether to save only valid inputs **/
    protected final boolean SAVE_ONLY_VALID = Boolean.getBoolean("jqf.guidance.bedivfuzz.SAVE_ONLY_VALID");

    public BeDivFuzzBaselineGuidance(String testName, Duration duration, Long trials, File outputDirectory, Random sourceOfRandomness) throws IOException {
        super(testName, duration, trials, outputDirectory, sourceOfRandomness);
        this.COUNT_UNIQUE_PATHS = true;
    }

    public BeDivFuzzBaselineGuidance(String testName, Duration duration, Long trials, File outputDirectory, File[] seedInputFiles, Random sourceOfRandomness) throws IOException {
        this(testName, duration, trials, outputDirectory, sourceOfRandomness);
        if (seedInputFiles != null) {
            for (File seedInputFile : seedInputFiles) {
                seedInputs.add(new SeedInput(seedInputFile));
            }
        }
    }

    public BeDivFuzzBaselineGuidance(String testName, Duration duration, Long trials, File outputDirectory, File seedInputDir, Random sourceOfRandomness) throws IOException {
        this(testName, duration, trials, outputDirectory, IOUtils.resolveInputFileOrDirectory(seedInputDir), sourceOfRandomness);
    }

    @Override
    public void registerChoiceTracer(BiConsumer<SplitTrackingSourceOfRandomness, GenerationStatus> tracer) {
        this.choiceTracer = tracer;
    }

    @Override
    protected String getTitle() {
        if (STUCTURAL_FUZZING)
            return "BeDivFuzz-Structure: Behavioral Diversity Fuzzing with Input Structure Feedback\n" +
                "--------------------------\n";
        else {

            return "BeDivFuzz-Simple: Behavioral Diversity Fuzzing\n" +
                    "--------------------------\n";
        }
    }

    @Override
    protected void displayStats(boolean force) {
        Date now = new Date();
        long intervalMilliseconds = now.getTime() - lastRefreshTime.getTime();
        intervalMilliseconds = Math.max(1, intervalMilliseconds);
        if (intervalMilliseconds < STATS_REFRESH_TIME_PERIOD && !force) {
            return;
        }
        super.displayStats(force);
        if (console != null && !QUIET_MODE) {
            if (!savedInputs.isEmpty()) {
                TrackingInput parent = (TrackingInput) savedInputs.get(currentParentInputIdx);
                console.printf("  Explore/Exploit:    %.2f/%.2f\n", parent.getStructureScore(), parent.getValueScore());
            }
            if (STUCTURAL_FUZZING) {
                console.printf("  Input structures:   %,d\n", savedInputStructures.size());
            }
        }
    }

    @Override
    public InputStream getInput() throws GuidanceException {
        conditionallySynchronize(multiThreaded, () -> {
            // Clear coverage stats for this run
            runCoverage.clear();
            if (TRACK_SEMANTIC_COVERAGE) semanticRunCoverage.clear();

            // Choose an input to execute based on state of queues
            if (!seedInputs.isEmpty()) {
                // First, if we have some specific seeds, use those
                currentInput = seedInputs.removeFirst();

                // Hopefully, the seeds will lead to new coverage and be added to saved inputs

            } else if (savedInputs.isEmpty()) {
                // Make fresh input using either list or maps
                // infoLog("Spawning new input from thin air");
                currentInput = createFreshInput();
            } else {
                // The number of children to produce is determined by how much of the coverage
                // pool this parent input hits
                Input currentParentInput = savedInputs.get(currentParentInputIdx);
                int targetNumChildren = getTargetChildrenForParent(currentParentInput);
                if (numChildrenGeneratedForCurrentParentInput >= targetNumChildren) {
                    // Select the next saved input to fuzz
                    currentParentInputIdx = (currentParentInputIdx + 1) % savedInputs.size();

                    // Count cycles
                    if (currentParentInputIdx == 0) {
                        completeCycle();
                    }

                    numChildrenGeneratedForCurrentParentInput = 0;
                }
                Input parent = savedInputs.get(currentParentInputIdx);

                // Fuzz it to get a new input
                // infoLog("Mutating input: %s", parent.desc);
                currentInput = parent.fuzz(random);
                numChildrenGeneratedForCurrentParentInput++;

                // Write it to disk for debugging
                try {
                    writeCurrentInputToFile(currentInputFile);
                } catch (IOException ignore) {
                }

                // Start time-counting for timeout handling
                this.runStart = new Date();
                this.branchCount = 0;
            }
        });

        return createParameterStream();
    }


    // Return a list of saving criteria that have been satisfied for a non-failure input
    @Override
    protected List<String> checkSavingCriteriaSatisfied(Result result) {
        // Update hit counts and unique paths metrics
        boolean valid = result == Result.SUCCESS;
        boolean checkUniquePath = COUNT_UNIQUE_PATHS || MEASURE_BEHAVIORAL_DIVERSITY || LOG_UNIQUE_PATH_INPUTS;
        if (checkUniquePath && uniquePaths.add(runCoverage.hashCode())) {
            if(MEASURE_BEHAVIORAL_DIVERSITY) {
                if (TRACK_SEMANTIC_COVERAGE) {
                    branchHitCounter.incrementBranchCounts(semanticRunCoverage);
                } else {
                    branchHitCounter.incrementBranchCounts(runCoverage);
                }
            }

            if (LOG_UNIQUE_PATH_INPUTS) {
                String saveFileName = String.format("id_%09d", uniquePaths.size());
                File saveFile = new File(uniquePathInputsDirectory, saveFileName);
                GuidanceException.wrap(() -> writeCurrentInputToFile(saveFile));
            }

            if (valid) {
                uniqueValidPaths.add(runCoverage.hashCode());
            }

            // Update score for last performed mutation
            if (!savedInputs.isEmpty()) {
                TrackingInput currentParent = (TrackingInput) savedInputs.get(currentParentInputIdx);
                currentParent.incrementScore();
            }
        }

        // Coverage before
        int nonZeroBefore = totalCoverage.getNonZeroCount();
        int validNonZeroBefore = validCoverage.getNonZeroCount();

        // Update total coverage
        boolean coverageBitsUpdated = totalCoverage.updateBits(runCoverage);
        if (TRACK_SEMANTIC_COVERAGE) semanticTotalCoverage.updateBits(semanticRunCoverage);

        // BeDivFuzz-simple searches for valid coverage-increasing inputs
        // BeDivFuzz-structure also requires coverage-increasing inputs to have a different structure
        if (valid) {
            if (STUCTURAL_FUZZING) {
                TrackingInput tracedInput = traceCurrentInput();
                int structuralHash = tracedInput.structuralHashCode();
                if (!savedInputStructures.contains(structuralHash)) {
                    validCoverage.updateBits(runCoverage);
                }
            } else {
                validCoverage.updateBits(runCoverage);
            }
        } else if (SAVE_ONLY_VALID) {
            return List.of();
        }

        // Coverage after
        int nonZeroAfter = totalCoverage.getNonZeroCount();
        if (nonZeroAfter > maxCoverage) {
            maxCoverage = nonZeroAfter;
        }
        int validNonZeroAfter = validCoverage.getNonZeroCount();

        // Possibly save input
        List<String> reasonsToSave = new ArrayList<>();

        if (!DISABLE_SAVE_NEW_COUNTS && coverageBitsUpdated) {
            reasonsToSave.add("+count");
        }

        // Save if new total coverage found
        if (nonZeroAfter > nonZeroBefore) {
            reasonsToSave.add("+cov");
        }

        // Save if new total coverage found
        if (validNonZeroAfter > validNonZeroBefore) {
            reasonsToSave.add("+valid");
        }

        return reasonsToSave;
    }

    @Override
    protected void completeCycle() {
    }

    @Override
    protected void saveCurrentInput(IntHashSet responsibilities, String why) throws IOException {
        // Guidance decides to save the input, ensure choice types are traced
        if (! (currentInput instanceof TrackingInput)) {
            traceCurrentInput();
        }

        // Let Zest handle the input-saving logic on the traced input
        super.saveCurrentInput(responsibilities, why);

        // Now decide if we want to favor the input
        boolean valid = why.endsWith("valid");
        if (STUCTURAL_FUZZING) {
            // BeDivFuzz-structure favors valid coverage-increasing inputs with novel structure
            boolean favor = valid && savedInputStructures.add(((TrackingInput) currentInput).structuralHashCode());
            currentInput.setFavored(favor);
        } else {
            // BeDivFuzz-simple favors valid coverage-increasing inputs
            currentInput.setFavored(valid);
        }
    }

    // Identifies structural and value choices of current input
    private TrackingInput traceCurrentInput() {
        TrackingInput tracedInput = new TrackingInput((LinearInput) currentInput);
        currentInput = tracedInput;

        SplitTrackingSourceOfRandomness random = new SplitTrackingSourceOfRandomness(
                createParameterStream(),
                tracedInput.structureChoices,
                tracedInput.valueChoices
        );

        GenerationStatus genStatus = new NonTrackingGenerationStatus(random.getStructureDelegate());
        choiceTracer.accept(random, genStatus);
        return tracedInput;
    }


    public class TrackingInput extends LinearInput {
        protected final List<Choice> structureChoices = new ArrayList<>();
        protected final List<Choice> valueChoices = new ArrayList<>();

        /** Whether the last performed mutation was on the structural or value parameters (exploration or exploitation)*/
        protected Mutation lastMutationType = Mutation.HAVOC;

        /** Structural mutation score: number of performed mutations / rewarded mutations. */
        protected int structureScore = 0;
        protected int structureCount = 0;

        /* Value mutation score: number of performed mutations / rewarded mutations. */
        protected int valueScore = 0;
        protected int valueCount = 0;

        public TrackingInput(LinearInput baseInput) {
            this.values = baseInput.values;
        }

        protected void incrementScore() {
            if (lastMutationType == Mutation.STRUCTURE) {
                structureScore++;
            } else if (lastMutationType == Mutation.VALUE) {
                valueScore ++;
            }
        }

        protected double getStructureScore() {
            return (structureCount == 0) ? 0 : ((double) structureScore) / structureCount;
        }

        protected double getValueScore() {
            return (valueCount == 0) ? 0 : ((double)valueScore) / valueCount;
        }

        protected Mutation chooseMutationType(Random random) {
            double avgStructureScore = (structureCount == 0) ? 0 : ((double) structureScore) / structureCount;
            double avgValueScore = (valueCount == 0) ? 0 : ((double)valueScore) / valueCount;

            // With probability epsilon (or if both scores are tied), perform random mutation type
            if ((random.nextDouble() < EPSILON) || (avgStructureScore == avgValueScore)) {
                return random.nextBoolean() ? Mutation.STRUCTURE : Mutation.VALUE;
            } else {
                // otherwise, choose most promising mutation
                return (avgStructureScore > avgValueScore) ? Mutation.STRUCTURE : Mutation.VALUE;
            }
        }

        @Override
        public Input fuzz(Random random) {
            if (structureChoices.isEmpty() || valueChoices.isEmpty())  {
                lastMutationType = Mutation.HAVOC;
                return super.fuzz(random);
            } else {
                lastMutationType = chooseMutationType(random);
                return fuzzTargeted(lastMutationType, random);
            }
        }

        protected Input fuzzTargeted(Mutation mutationType, Random random) {
            // Clone this input to create initial version of new child
            LinearInput newInput = new LinearInput(this);

            // Stack a bunch of mutations
            int numMutations = sampleGeometric(random, MEAN_MUTATION_COUNT);

            List<Choice> choices;
            if (mutationType == Mutation.STRUCTURE) {
                choices = structureChoices;
                structureCount++;
                newInput.desc += ",structure:" + numMutations;
            } else {
                choices = valueChoices;
                valueCount++;
                newInput.desc += ",value:" + numMutations;
            }

            boolean setToZero = random.nextDouble() < 0.1; // one out of 10 times

            for (int mutation = 1; mutation <= numMutations; mutation++) {

                // Select a random offset and size
                Choice choice = choices.get(random.nextInt(choices.size()));
                int baseIdx = choice.getOffset();
                int size = choice.getSize();

                /**
                 * For boolean choices, only the lowest bit is actually used (see {@link StreamBackedRandom#next(int bits)}.
                 * Thus, when mutating this choice we actually have to flip that bit, otherwise there is a 50% chance
                 * we end up with the same choice after mutation.
                 */
                if (size == -1) {
                    int mutatedValue = values.get(baseIdx) ^ 1;
                    newInput.values.set(baseIdx, mutatedValue);
                } else {
                    // Don't go over bound of choice
                    int mutationSize = Math.min(sampleGeometric(random, MEAN_MUTATION_SIZE), size);
                    for (int offset = 0; offset < mutationSize; offset++) {
                        int mutatedValue = setToZero ? 0 : random.nextInt(256);
                        newInput.values.set(baseIdx + offset, mutatedValue);
                    }
                }
            }
            return newInput;
        }

        protected int structuralHashCode() {
            return structureChoices.hashCode();
        }
    }
}
