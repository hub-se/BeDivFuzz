package de.hub.se.jqf.bedivfuzz.guidance;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import de.hub.se.jqf.bedivfuzz.junit.quickcheck.tracking.SplitTrackingSourceOfRandomness;
import de.hub.se.jqf.bedivfuzz.junit.quickcheck.tracking.Choice;
import edu.berkeley.cs.jqf.fuzz.ei.ZestGuidance;
import edu.berkeley.cs.jqf.fuzz.guidance.Result;
import edu.berkeley.cs.jqf.fuzz.junit.quickcheck.NonTrackingGenerationStatus;
import edu.berkeley.cs.jqf.fuzz.util.IOUtils;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.function.BiConsumer;

public class BeDivFuzzGuidance extends ZestGuidance {

    /** The mutation types that can be performed on the choice sequence. */
    private enum Mutation {HAVOC, STRUCTURE, VALUE};

    /** The callback responsible for tracing the choice types for each saved input. */
    private BiConsumer<SplitTrackingSourceOfRandomness, GenerationStatus> choiceTracer;

    /** The epsilon-greedy tradeoff between exploration and exploitation. */
    protected final double EPSILON = Double.parseDouble(System.getProperty("jqf.guidance.bedivfuzz.epsilon", "0.2"));

    /** The havoc mutation probability. */
    protected final double HAVOC_RATE = Double.parseDouble(System.getProperty("jqf.guidance.bedivfuzz.havoc_rate", "0.1"));

    public BeDivFuzzGuidance(String testName, Duration duration, Long trials, File outputDirectory, Random sourceOfRandomness) throws IOException {
        super(testName, duration, trials, outputDirectory, sourceOfRandomness);
        this.COUNT_UNIQUE_PATHS = true;
    }

    public BeDivFuzzGuidance(String testName, Duration duration, Long trials, File outputDirectory, File[] seedInputFiles, Random sourceOfRandomness) throws IOException {
        this(testName, duration, trials, outputDirectory, sourceOfRandomness);
        if (seedInputFiles != null) {
            for (File seedInputFile : seedInputFiles) {
                seedInputs.add(new SeedInput(seedInputFile));
            }
        }
    }

    public BeDivFuzzGuidance(String testName, Duration duration, Long trials, File outputDirectory, File seedInputDir, Random sourceOfRandomness) throws IOException {
        this(testName, duration, trials, outputDirectory, IOUtils.resolveInputFileOrDirectory(seedInputDir), sourceOfRandomness);
    }

    public void registerChoiceTracer(BiConsumer<SplitTrackingSourceOfRandomness, GenerationStatus> tracer) {
        this.choiceTracer = tracer;
    }

    @Override
    protected String getTitle() {
        return "BeDivFuzz: Behavioral Diversity Fuzzing\n" +
                "--------------------------\n";
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
        }
    }

    /*
    @Override
    public InputStream getInput() throws GuidanceException {
        if (!savedInputs.isEmpty()) {
            // The number of children to produce is determined by how much of the coverage
            // pool this parent input hits
            Input currentParentInput = savedInputs.get(currentParentInputIdx);
            int targetNumChildren = getTargetChildrenForParent(currentParentInput);
            if (numChildrenGeneratedForCurrentParentInput >= targetNumChildren) {
                int numUniquePathsAfter = uniquePaths.size();

                // Unfavor inputs that do not produce new paths
                if (currentParentInput.isFavored() && numUniquePathsAfter <= numUniquePathsBefore) {
                    currentParentInput.setFavored(false);
                    numInputsUnfavored++;
                }

                numUniquePathsBefore = numUniquePathsAfter;
            }
        }
        return super.getInput();
    }
     */

    @Override
    protected List<String> checkSavingCriteriaSatisfied(Result result) {
        int uniquePathsBefore = uniquePaths.size();
        List<String> reasonstoSave = super.checkSavingCriteriaSatisfied(result);
        int uniquePathsAfter = uniquePaths.size();
        if ((uniquePathsAfter > uniquePathsBefore) && !savedInputs.isEmpty()) {
            TrackingInput currentParent = (TrackingInput) savedInputs.get(currentParentInputIdx);
            currentParent.incrementScore();
        }
        return reasonstoSave;
    }

    @Override
    protected void saveCurrentInput(IntHashSet responsibilities, String why) throws IOException {

        // Trace choices of input to save
        TrackingInput trackingInput = new TrackingInput((LinearInput) currentInput);
        currentInput = trackingInput;

        SplitTrackingSourceOfRandomness random = new SplitTrackingSourceOfRandomness(
                createParameterStream(),
                trackingInput.structureChoices,
                trackingInput.valueChoices
        );

        GenerationStatus genStatus = new NonTrackingGenerationStatus(random.getStructureDelegate());
        choiceTracer.accept(random, genStatus);

        // Save tracking input
        super.saveCurrentInput(responsibilities, why);
    }


    public class TrackingInput extends LinearInput {
        private final List<Choice> structureChoices = new ArrayList<>();
        private final List<Choice> valueChoices = new ArrayList<>();

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

        public void incrementScore() {
            if (lastMutationType == Mutation.STRUCTURE) {
                structureScore++;
            } else if (lastMutationType == Mutation.VALUE) {
                valueScore ++;
            }
        }

        public double getStructureScore() {
            return (structureCount == 0) ? 0 : ((double) structureScore) / structureCount;
        }

        public double getValueScore() {
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
            if (structureChoices.isEmpty() || valueChoices.isEmpty() || random.nextDouble() < HAVOC_RATE)  {
                lastMutationType = Mutation.HAVOC;
                return super.fuzz(random);
            } else {
                lastMutationType = chooseMutationType(random);
                return fuzzTargeted(lastMutationType, random);
            }
        }

        public Input fuzzTargeted(Mutation mutationType, Random random) {
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

        public void validateChoiceSequence() {
            int structureOffset = 0;
            if (!structureChoices.isEmpty()) {
                Choice lastChoice = structureChoices.get(structureChoices.size() - 1);
                structureOffset = lastChoice.getOffset() + Math.abs(lastChoice.getSize());
            }

            if (structureOffset != requested) {
                int valueOffset = 0;
                if (!valueChoices.isEmpty()) {
                    Choice lastChoice = valueChoices.get(valueChoices.size() - 1);
                    valueOffset = lastChoice.getOffset() + Math.abs(lastChoice.getSize());
                }

                if (valueOffset != requested) {
                    throw new IllegalStateException(String.format("Choice sequence not aligned with requests: " +
                            "requested = %d, offset = %d", requested, Math.max(structureOffset, valueOffset)));
                }
            }
        }
    }

}
