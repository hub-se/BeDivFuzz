package de.hub.se.bedivfuzz.guidance;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import de.hub.se.bedivfuzz.junit.quickcheck.tracing.SplitTracingSourceOfRandomness;
import de.hub.se.bedivfuzz.junit.quickcheck.tracing.Choice;
import edu.berkeley.cs.jqf.fuzz.ei.ZestGuidance;
import edu.berkeley.cs.jqf.fuzz.guidance.GuidanceException;
import edu.berkeley.cs.jqf.fuzz.guidance.Result;
import edu.berkeley.cs.jqf.fuzz.junit.quickcheck.NonTrackingGenerationStatus;
import edu.berkeley.cs.jqf.fuzz.util.IOUtils;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;

import java.io.*;
import java.time.Duration;
import java.util.*;
import java.util.function.BiConsumer;

/**
 * A guidance that adaptively performs structure-preserving and structure-changing mutations.
 *
 */
public class BeDivFuzzGuidance extends ZestGuidance implements SplitGeneratorGuidance {

    /** The set of all saved input structure hashes. */
    protected IntHashSet savedInputStructures = new IntHashSet();

    /** Set of hashes of all paths generated so far. */
    protected IntHashSet uniquePaths = new IntHashSet();

    /** The mutation types that can be performed on the choice sequence. */
    protected enum Mutation {HAVOC, STRUCTURE, VALUE};

    /** The callback responsible for tracing the choice types for each saved input. */
    protected BiConsumer<SplitTracingSourceOfRandomness, GenerationStatus> choiceTracer;

    /** The epsilon-greedy tradeoff between exploration and exploitation. */
    protected final double EPSILON = Double.parseDouble(System.getProperty("jqf.guidance.bedivfuzz.epsilon", "0.2"));

    /** The havoc mutation probability. */
    protected final double HAVOC_RATE = Double.parseDouble(System.getProperty("jqf.guidance.bedivfuzz.havoc_rate", "0.1"));

    /** Save only inputs that increase coverage and have a novel input structure. */
    protected final boolean STRUCTURAL_FEEDBACK = Boolean.getBoolean("jqf.guidance.bedivfuzz.STRUCTURAL_FEEDBACK");

    /** The header for the status screen. */
    protected final String header;

    public BeDivFuzzGuidance(String testName, Duration duration, Long trials, File outputDirectory, Random sourceOfRandomness) throws IOException {
        super(testName, duration, trials, outputDirectory, sourceOfRandomness);
        StringBuilder sb = new StringBuilder("BeDivFuzz: Behavioral Diversity Fuzzing\n");
        sb.append("[epsilon: " + EPSILON + ", havoc: " + HAVOC_RATE + ", structural feedback: " + STRUCTURAL_FEEDBACK + "]\n");
        sb.append("--------------------------\n");
        header = sb.toString();
    }

    public BeDivFuzzGuidance(String testName, Duration duration, Long trials, File outputDirectory, File[] seedInputFiles, Random sourceOfRandomness) throws IOException {
        this(testName, duration, trials, outputDirectory, sourceOfRandomness);
        if (seedInputFiles != null) {
            throw new GuidanceException("BeDivFuzz does not support seed input files");
        }
    }

    public BeDivFuzzGuidance(String testName, Duration duration, Long trials, File outputDirectory, File seedInputDir, Random sourceOfRandomness) throws IOException {
        this(testName, duration, trials, outputDirectory, IOUtils.resolveInputFileOrDirectory(seedInputDir), sourceOfRandomness);
    }

    @Override
    public void registerChoiceTracer(BiConsumer<SplitTracingSourceOfRandomness, GenerationStatus> tracer) {
        this.choiceTracer = tracer;
    }

    @Override
    protected String getTitle() {
        return header;
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
                ChoiceTracedInput parent = (ChoiceTracedInput) savedInputs.get(currentParentInputIdx);
                console.printf("Explore/Exploit:      %.2f/%.2f\n", parent.getStructureScore(), parent.getValueScore());
                if (STRUCTURAL_FEEDBACK) {
                    console.printf("Uniq input structs:   %d\n", savedInputStructures.size());
                }
            }
        }
    }

    @Override
    protected Input<?> createFreshInput() {
        return new BaseInput();
    }

    @Override
    public void handleResult(Result result, Throwable error) throws GuidanceException {
        if (uniquePaths.add(runCoverage.hashCode()) && !savedInputs.isEmpty()) {
            ChoiceTracedInput currentParent = (ChoiceTracedInput) savedInputs.get(currentParentInputIdx);
            currentParent.incrementScore();
            if (result == Result.SUCCESS) {
                currentParent.incrementScore();
            }
        }
        super.handleResult(result, error);
    }

    @Override
    protected List<String> checkSavingCriteriaSatisfied(Result result) {
        List<String> reasonsToSave = super.checkSavingCriteriaSatisfied(result);

        // Set valid flag for favoring check in saveCurrentInput
        if (STRUCTURAL_FEEDBACK && !reasonsToSave.isEmpty() && result == Result.SUCCESS) {
            ((BaseInput) currentInput).setValid();
        }

        return reasonsToSave;
    }

    @Override
    protected void saveCurrentInput(IntHashSet responsibilities, String why) throws IOException {
        BaseInput currentBaseInput = (BaseInput) currentInput;

        // Trace choices of input to save
        ChoiceTracedInput trackingInput = new ChoiceTracedInput(currentBaseInput);
        currentInput = trackingInput;

        SplitTracingSourceOfRandomness random = new SplitTracingSourceOfRandomness(
                createParameterStream(),
                trackingInput.structureChoices,
                trackingInput.valueChoices
        );

        GenerationStatus genStatus = new NonTrackingGenerationStatus(random.getStructureDelegate());
        choiceTracer.accept(random, genStatus);

        // BeDivFuzz-Structure: Favor structurally novel+valid inputs
        if (STRUCTURAL_FEEDBACK && currentBaseInput.valid) {
            int structuralHashCode = ((ChoiceTracedInput) currentInput).structuralHashCode();
            if (savedInputStructures.add(structuralHashCode)) {
                currentInput.setFavored();
            }
        }

        // Save tracking input
        super.saveCurrentInput(responsibilities, why);
    }


    public class BaseInput extends LinearInput {
        protected boolean valid;

        public BaseInput() {
            super();
        }

        public BaseInput(BaseInput other) {
            super(other);
        }

        public ArrayList<Integer> getValues() {
            return values;
        }

        public void setValue(int index, int value) {
            values.set(index, value);
        }

        public void setValid() {
           valid = true;
        }

        @Override
        public Input fuzz(Random random) {
            // Clone this input to create initial version of new child
            BaseInput newInput = new BaseInput(this);

            // Stack a bunch of mutations
            int numMutations = sampleGeometric(random, MEAN_MUTATION_COUNT);

            boolean setToZero = random.nextDouble() < 0.1; // one out of 10 times

            for (int mutation = 1; mutation <= numMutations; mutation++) {

                // Select a random offset and size
                int offset = random.nextInt(newInput.values.size());
                int mutationSize = sampleGeometric(random, MEAN_MUTATION_SIZE);

                // Mutate a contiguous set of bytes from offset
                for (int i = offset; i < offset + mutationSize; i++) {
                    // Don't go past end of list
                    if (i >= newInput.values.size()) {
                        break;
                    }

                    // Otherwise, apply a random mutation
                    int mutatedValue = setToZero ? 0 : random.nextInt(256);
                    newInput.values.set(i, mutatedValue);
                }
            }

            return newInput;
        }
    }


    public class ChoiceTracedInput extends BaseInput {
        protected final List<Choice> structureChoices = new ArrayList<>();
        protected final List<Choice> valueChoices = new ArrayList<>();

        /** Whether the last performed mutation was on the structural or value parameters (exploration or exploitation). */
        protected Mutation lastMutationType = Mutation.HAVOC;

        /** Structural mutation score: number of performed mutations / rewarded mutations. */
        protected int structureScore = 0;
        protected int structureCount = 0;

        /* Value mutation score: number of performed mutations / rewarded mutations. */
        protected int valueScore = 0;
        protected int valueCount = 0;

        public ChoiceTracedInput(BaseInput baseInput) {
            this.values = baseInput.getValues();
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
            return (valueCount == 0) ? 0 : ((double) valueScore) / valueCount;
        }

        protected Mutation chooseMutationType(Random random) {
            double structureScore = getStructureScore();
            double valueScore = getValueScore();

            // With probability epsilon (or if both scores are tied), perform random mutation type
            if ((random.nextDouble() < EPSILON) || (structureScore == valueScore)) {
                return random.nextBoolean() ? Mutation.STRUCTURE : Mutation.VALUE;
            } else {
                // otherwise, choose most promising mutation
                return (structureScore > valueScore) ? Mutation.STRUCTURE : Mutation.VALUE;
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

        protected Input fuzzTargeted(Mutation mutationType, Random random) {
            // Clone this input to create initial version of new child
            BaseInput newInput = new BaseInput(this);

            // Stack a bunch of mutations
            int numMutations = sampleGeometric(random, MEAN_MUTATION_COUNT);

            List<Choice> choices;
            if (mutationType == Mutation.STRUCTURE) {
                choices = structureChoices;
                structureCount++;
            } else {
                choices = valueChoices;
                valueCount++;
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
                    newInput.setValue(baseIdx, mutatedValue);
                } else {
                    // Don't go over bound of choice
                    int mutationSize = Math.min(sampleGeometric(random, MEAN_MUTATION_SIZE), size);
                    for (int offset = 0; offset < mutationSize; offset++) {
                        int mutatedValue = setToZero ? 0 : random.nextInt(256);
                        newInput.setValue(baseIdx + offset, mutatedValue);
                    }
                }
            }
            return newInput;
        }

        protected int structuralHashCode() {
            return structureChoices.hashCode();
        }

        protected void validateChoiceSequence() {
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
