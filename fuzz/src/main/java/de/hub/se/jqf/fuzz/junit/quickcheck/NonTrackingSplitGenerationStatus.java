package de.hub.se.jqf.fuzz.junit.quickcheck;

import edu.berkeley.cs.jqf.fuzz.junit.quickcheck.NonTrackingGenerationStatus;

public class NonTrackingSplitGenerationStatus extends NonTrackingGenerationStatus {

    private final SplitSourceOfRandomness random;

    public NonTrackingSplitGenerationStatus(SplitSourceOfRandomness random) {
        super(random);
        this.random = random;
    }

    @Override
    public int size() {
        return random.nextInt(MEAN_SIZE, true);
    }
}
