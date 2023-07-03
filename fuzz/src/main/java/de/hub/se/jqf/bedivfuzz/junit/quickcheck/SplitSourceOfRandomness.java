package de.hub.se.jqf.bedivfuzz.junit.quickcheck;

import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import edu.berkeley.cs.jqf.fuzz.guidance.StreamBackedRandom;
import edu.berkeley.cs.jqf.fuzz.junit.quickcheck.FastSourceOfRandomness;

/**
 * A source of randomness used by {@linkplain SplitGenerator} SplitGenerators to generate
 * random inputs. Backed by two random sources that are responsible for producing
 * either structural or value random data of the input.
 */
public class SplitSourceOfRandomness {

    /** The source of randomness that produces structural random data. */
    public final SourceOfRandomness structure;

    /** The source of randomness that produces value random data. */
    public final SourceOfRandomness value;

    /**
     * Creates a new split source of randomness.
     *
     * @param structuralDelegate a file-backed random number generator for structural decisions
     * @param valueDelegate a file-backed random number generator for value decisions
     */
    public SplitSourceOfRandomness(StreamBackedRandom structuralDelegate, StreamBackedRandom valueDelegate) {
        structure = new FastSourceOfRandomness(structuralDelegate);
        value = new FastSourceOfRandomness(valueDelegate);
    }

}

