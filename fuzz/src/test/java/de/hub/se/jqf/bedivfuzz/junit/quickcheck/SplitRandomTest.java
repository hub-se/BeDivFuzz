package de.hub.se.jqf.bedivfuzz.junit.quickcheck;

import de.hub.se.jqf.bedivfuzz.junit.quickcheck.generator.SplitBinaryTreeGenerator;
import de.hub.se.jqf.bedivfuzz.junit.quickcheck.tracking.SplitTrackingSourceOfRandomness;
import org.junit.Before;
import org.junit.Test;

import java.util.Random;

public class SplitRandomTest {
    private RandomInput randomInput;
    private SplitSourceOfRandomness random;

    private RandomInput trackingInput;
    private SplitTrackingSourceOfRandomness trackingRandom;

    private SplitBinaryTreeGenerator generator = new SplitBinaryTreeGenerator();

    @Before
    public void setupSourceOfRandomness() {
        Random r = new Random(24);
        randomInput = new RandomInput();
        random = new SplitSourceOfRandomness(randomInput.toInputStream(r));

        Random r2 = new Random(24);
        trackingInput = new RandomInput();
        trackingRandom = new SplitTrackingSourceOfRandomness(trackingInput.toInputStream(r2));
    }

    @Test
    // Makes sure that using SplitTrackingSourceOfRandomness does not mess with the original input values.
    public void testInputValues() {
        generator.generate(random, null);
        generator.generate(trackingRandom, null);

        assert(randomInput.values.equals(trackingInput.values));
    }
}
