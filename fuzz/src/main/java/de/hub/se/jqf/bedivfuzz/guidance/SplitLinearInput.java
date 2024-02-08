package de.hub.se.jqf.bedivfuzz.guidance;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Random;

public class SplitLinearInput extends SplitInput {
    /** Max input size to generate. */
    protected static final int MAX_INPUT_SIZE = Integer.getInteger("jqf.ei.MAX_INPUT_SIZE", 10240);

    /** Whether to generate EOFs when we run out of bytes in the input, instead of randomly generating new bytes. **/
    protected static final boolean GENERATE_EOF_WHEN_OUT = Boolean.getBoolean("jqf.ei.GENERATE_EOF_WHEN_OUT");

    /** Mean number of mutations to perform in each round. */
    protected static final double MEAN_MUTATION_COUNT = 8.0;

    /** Mean number of contiguous bytes to mutate in each mutation. */
    protected static final double MEAN_MUTATION_SIZE = 4.0; // Bytes

    /** Parameter for epsilon-greedy exploration vs. exploitation trade-off. */
    protected static final double EPSILON = 0.2;

    /** Whether the last performed mutation was on the structural or value parameters (exploration or exploitation)*/
    enum MutationType {HAVOC, STRUCTURE, VALUE}
    protected MutationType lastMutationType = MutationType.HAVOC;

    // Global random instance for new input values.
    protected static Random random = new Random();

    /** A list of structural byte parameters (0-255) ordered by their index. */
    protected ArrayList<Integer> structuralParameters;

    /** A list of value byte parameters (0-255) ordered by their index. */
    protected ArrayList<Integer> valueParameters;

    /** The number of structural bytes requested so far. */
    protected int requestedStructure = 0;

    /** The number of value bytes requested so far. */
    protected int requestedValue = 0;

    /** Structural mutation score: number of performed mutations / rewarded mutations. */
    protected int structureScore = 0;
    protected int structureCount = 0;

    /* Value mutation score: number of performed mutations / rewarded mutations. */
    protected int valueScore = 0;
    protected int valueCount = 0;

    public SplitLinearInput() {
        super();
        this.structuralParameters = new ArrayList<>();
        this.valueParameters = new ArrayList<>();
    }

    public SplitLinearInput(SplitLinearInput other) {
        super(other);
        this.structuralParameters = new ArrayList<>(other.structuralParameters);
        this.valueParameters = new ArrayList<>(other.valueParameters);
    }


    @Override
    public int getOrGenerateFreshStructure(Integer key, Random random) {
        // Otherwise, make sure we are requesting just beyond the end-of-list
        // assert (key == values.size());
        if (key != requestedStructure) {
            throw new IllegalStateException(String.format("Structural parameters are out of order. " +
                    "Size = %d, Key = %d", structuralParameters.size(), key));
        }

        int val = getOrGenerateFresh(key, random, structuralParameters);
        if (val != -1) {
            requestedStructure++;
        }
        return val;
    }

    @Override
    public int getOrGenerateFreshValue(Integer key, Random random) {
        // Otherwise, make sure we are requesting just beyond the end-of-list
        // assert (key == values.size());
        if (key != requestedValue) {
            throw new IllegalStateException(String.format("Value parameters are out of order. " +
                    "Size = %d, Key = %d", valueParameters.size(), key));
        }

        int val = getOrGenerateFresh(key, random, valueParameters);
        if (val != -1) {
            requestedValue++;
        }
        return val;
    }

    /**
     * Returns the next parameter from the (structural or value) source
     * @param key the index of the requested parameter
     * @param random the random source for producing fresh values
     * @param source the source to get the parameter from
     * @return the next (or a fresh) parameter from the provided source
     */
    public int getOrGenerateFresh(Integer key, Random random, ArrayList<Integer> source) {
        // Don't generate over the limit
        if (requestedValue + requestedStructure >= MAX_INPUT_SIZE) {
            return -1;
        }

        // If it exists in the list, return it
        if (key < source.size()) {
            return source.get(key);
        }

        // Handle end of stream
        if (GENERATE_EOF_WHEN_OUT) {
            return -1;
        } else {
            // Just generate a random input
            int val = random.nextInt(256);
            source.add(val);
            // infoLog("Generating fresh byte at key=%d, total requested=%d", key, requested);
            return val;
        }
    }

    @Override
    public int size() {
        return structuralParameters.size() + valueParameters.size();
    }

    /**
     * Truncates the both parameter lists to remove values that were never actually requested.
     *
     * <p>Although this operation mutates the underlying object, the effect should
     * not be externally visible (at least as long as the test executions are
     * deterministic).</p>
     */
    @Override
    public void gc() {
        // Remove elements beyond "requested". Note that an input may be produced by requesting
        // only structural (or value) parameters. In this case, the other parameter list is empty.
        if (requestedStructure > 0) {
            structuralParameters = new ArrayList<>(structuralParameters.subList(0, requestedStructure));
            structuralParameters.trimToSize();
        }
        if (requestedValue > 0) {
            valueParameters = new ArrayList<>(valueParameters.subList(0, requestedValue));
            valueParameters.trimToSize();
        }
    }

    @Override
    public int hashCode() {
        gc();
        return Objects.hash(structuralParameters, valueParameters);
    }

    public void addScore(int score) {
        if (lastMutationType == MutationType.STRUCTURE) {
            structureScore += score;
        } else if (lastMutationType == MutationType.VALUE) {
            valueScore += score;
        }
    }

    public double getStructureScore() {
        return (structureCount == 0) ? 0 : ((double) structureScore) / structureCount;
    }

    public double getValueScore() {
        return (valueCount == 0) ? 0 : ((double)valueScore) / valueCount;
    }

    protected MutationType chooseMutationType(Random random) {
        if (requestedValue > 0) {
            double avgStructureScore = getStructureScore();
            double avgValueScore = getValueScore();

            // With probability epsilon (or if both scores are tied), perform random mutation type
            if ((random.nextDouble() < EPSILON) || (avgStructureScore == avgValueScore)) {
                return random.nextBoolean() ? MutationType.STRUCTURE : MutationType.VALUE;
            } else {
                // otherwise, choose most promising mutation
                return (avgStructureScore > avgValueScore) ? MutationType.STRUCTURE : MutationType.VALUE;
            }
        } else {
            // can only perform structural mutation (exploration)
            return MutationType.STRUCTURE;
        }
    }

    public SplitLinearInput fuzzHavoc(Random random) {
        lastMutationType = MutationType.HAVOC;
        SplitLinearInput newInput = new SplitLinearInput(this);
        newInput.desc += ",havoc";
        if (!structuralParameters.isEmpty()) fuzz(random, newInput.structuralParameters);
        if (!valueParameters.isEmpty()) fuzz(random, newInput.valueParameters);
        return newInput;
    }

    public SplitLinearInput fuzz(Random random) {
        lastMutationType = chooseMutationType(random);
        if (lastMutationType == MutationType.STRUCTURE) {
            structureCount++;
            return fuzzStructure(random);
        } else if (lastMutationType == MutationType.VALUE) {
            valueCount++;
            return fuzzValues(random);
        } else {
            throw new IllegalStateException("Expected mutation type STRUCTURE or VALUE, but was: " + lastMutationType);
        }
    }

    public SplitLinearInput fuzzStructure(Random random) {
        // Clone this input to create initial version of new child
        SplitLinearInput newInput = new SplitLinearInput(this);
        newInput.desc += ",structure";
        fuzz(random, newInput.structuralParameters);
        return newInput;
    }

    public SplitLinearInput fuzzValues(Random random) {
        // Clone this input to create initial version of new child
        SplitLinearInput newInput = new SplitLinearInput(this);
        newInput.desc += ",value";
        fuzz(random, newInput.valueParameters);
        return newInput;
    }

    public void fuzz(Random random, ArrayList<Integer> values) {
        // Stack a bunch of mutations
        int numMutations = sampleGeometric(random, MEAN_MUTATION_COUNT);

        boolean setToZero = random.nextDouble() < 0.1; // one out of 10 times

        for (int mutation = 1; mutation <= numMutations; mutation++) {

            // Select a random offset and size
            int offset = random.nextInt(values.size());
            int mutationSize = sampleGeometric(random, MEAN_MUTATION_SIZE);

            // desc += String.format(":%d@%d", mutationSize, idx);

            // Mutate a contiguous set of bytes from offset
            for (int i = offset; i < offset + mutationSize; i++) {
                // Don't go past end of list
                if (i >= values.size()) {
                    break;
                }

                // Otherwise, apply a random mutation
                int mutatedValue = setToZero ? 0 : random.nextInt(256);
                values.set(i, mutatedValue);
            }
        }
    }
}
