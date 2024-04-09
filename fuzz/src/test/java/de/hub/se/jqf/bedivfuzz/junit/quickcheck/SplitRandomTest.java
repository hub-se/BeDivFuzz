package de.hub.se.jqf.bedivfuzz.junit.quickcheck;

import de.hub.se.jqf.bedivfuzz.junit.quickcheck.generator.SplitBinaryTreeGenerator;
import de.hub.se.jqf.bedivfuzz.junit.quickcheck.tracking.SplitChoicePublishingSourceOfRandomness;
import org.junit.Before;
import org.junit.Test;

import java.util.Random;

public class SplitRandomTest {
    private RandomInput randomInput;
    private DelegatingSplitSourceOfRandomness random;

    private RandomInput trackingInput;
    private SplitChoicePublishingSourceOfRandomness trackingRandom;

    private SplitBinaryTreeGenerator generator = new SplitBinaryTreeGenerator();

    @Before
    public void setupSourceOfRandomness() {
        Random r = new Random(0);
        randomInput = new RandomInput();
        SeedingStreamBackedRandom fileRandom = new SeedingStreamBackedRandom(randomInput.toInputStream(r));
        random = new DelegatingSplitSourceOfRandomness(fileRandom);

        Random r2 = new Random(0);
        trackingInput = new RandomInput();
        SeedingStreamBackedRandom fileRandom2 = new SeedingStreamBackedRandom(trackingInput.toInputStream(r2));
        trackingRandom = new SplitChoicePublishingSourceOfRandomness(fileRandom2);
    }

    @Test
    // Makes sure that using SplitTrackingSourceOfRandomness does not mess with the original input values.
    public void testInputValues() {
        generator.generate(random, null);
        generator.generate(trackingRandom, null);
        assert(randomInput.values.equals(trackingInput.values));
    }
}
