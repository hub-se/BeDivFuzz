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

    // Global random instance for new input values
    protected static Random random = new Random();

    /** A list of structural byte parameters (0-255) ordered by their index. */
    protected ArrayList<Integer> structuralParameters;

    /** A list of value byte parameters (0-255) ordered by their index. */
    protected ArrayList<Integer> valueParameters;

    /** The number of structural bytes requested so far */
    protected int requestedStructure = 0;

    /** The number of value bytes requested so far */
    protected int requestedValue = 0;

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

    public boolean structureMutable() {
        return structuralParameters.size() > 0;
    }

    public boolean valueMutable() {
        return valueParameters.size() > 0;
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

    public SplitLinearInput fuzzHavoc(Random random) {
        SplitLinearInput newInput = new SplitLinearInput(this);
        newInput.desc += ",havoc";
        if (structuralParameters.size() > 0) fuzz(random, newInput.structuralParameters);
        if (valueParameters.size() > 0) fuzz(random, newInput.valueParameters);
        return newInput;
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
