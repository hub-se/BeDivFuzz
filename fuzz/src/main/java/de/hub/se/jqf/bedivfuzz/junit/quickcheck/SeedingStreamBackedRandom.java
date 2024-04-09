package de.hub.se.jqf.bedivfuzz.junit.quickcheck;

import edu.berkeley.cs.jqf.fuzz.guidance.StreamBackedRandom;

import java.io.InputStream;

/**
 * A StreamBackedRandom to share between multiple {@link com.pholser.junit.quickcheck.random.SourceOfRandomness} instances.
 * Swallows calls to nextLong() which are used for seeding until all instances have been initialized (set by initialize()).
 */
public class SeedingStreamBackedRandom extends StreamBackedRandom {
    private static final long serialVersionUID = 886690719613465660L;
    private boolean initialized = false;

    public SeedingStreamBackedRandom(InputStream source) {
        super(source);
    }

    public void initialize() {
        initialized = true;
    }

    @Override
    public long nextLong() {
        if (!initialized) {
            return 0;
        }
        return super.nextLong();
    }
}
