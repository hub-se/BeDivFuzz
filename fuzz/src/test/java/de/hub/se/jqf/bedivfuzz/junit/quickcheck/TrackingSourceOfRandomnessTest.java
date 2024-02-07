package de.hub.se.jqf.bedivfuzz.junit.quickcheck;

import de.hub.se.jqf.bedivfuzz.junit.quickcheck.tracking.TrackingSourceOfRandomness;
import edu.berkeley.cs.jqf.fuzz.guidance.StreamBackedRandom;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;

public class TrackingSourceOfRandomnessTest {

    private static Random r;
    private LinearInput input;
    private TrackingSourceOfRandomness trackingRandom;

    @Before
    public void setupSourceOfRandomness() {
        r = new Random(24);
        input = new LinearInput();
        StreamBackedRandom random = new StreamBackedRandom(createParameterStream(), Long.BYTES);
        trackingRandom = new TrackingSourceOfRandomness(random);
    }

    @Before
    public void createInput() {
        input = new LinearInput();
    }

    public static class LinearInput {
        int requested = 0;

        public int getOrGenerateFresh(Integer key, Random random) {
            requested++;
            return random.nextInt();
        }
    }

    protected InputStream createParameterStream() {
        return new InputStream() {
            int bytesRead = 0;

            @Override
            public int read() throws IOException {
                return input.getOrGenerateFresh(bytesRead++, r);
            }
        };
    }

    @Test
    public void testByteIndices() {
        trackingRandom.nextByte(Byte.MIN_VALUE, Byte.MAX_VALUE);
        trackingRandom.nextByte((byte) 0, Byte.MAX_VALUE);
        trackingRandom.nextByte(Byte.MIN_VALUE, (byte) 0);
        trackingRandom.nextByte((byte) -1, (byte) 1);
        trackingRandom.nextBytes(1);

        byte[] arr = new byte[5];
        trackingRandom.nextBytes(arr);
        assertEquals(trackingRandom.getCurrentChoiceOffset(), input.requested);
    }

    @Test
    public void testDoubleIndices() {
        trackingRandom.nextDouble();
        assertEquals(trackingRandom.getCurrentChoiceOffset(), input.requested);
        trackingRandom.nextDouble(Double.MIN_VALUE, Double.MAX_VALUE);
        assertEquals(trackingRandom.getCurrentChoiceOffset(), input.requested);
        trackingRandom.nextDouble(0.0, Double.MAX_VALUE);
        assertEquals(trackingRandom.getCurrentChoiceOffset(), input.requested);
        trackingRandom.nextDouble(Double.MIN_VALUE, 1.0);
        assertEquals(trackingRandom.getCurrentChoiceOffset(), input.requested);
        trackingRandom.nextDouble(-1.0, 1.0);
        assertEquals(trackingRandom.getCurrentChoiceOffset(), input.requested);
    }

    @Test
    public void testFloatIndices() {
        trackingRandom.nextFloat();
        trackingRandom.nextFloat(Float.MIN_VALUE, Float.MAX_VALUE);
        trackingRandom.nextFloat(0.0f, Float.MAX_VALUE);
        trackingRandom.nextFloat(Float.MIN_VALUE, 1.0f);
        trackingRandom.nextFloat(-1.0f, 1.0f);
        assertEquals(trackingRandom.getCurrentChoiceOffset(), input.requested);
    }

    @Test
    public void testShortIndices() {
        trackingRandom.nextShort(Short.MIN_VALUE, Short.MAX_VALUE);
        trackingRandom.nextShort((short) 0, Short.MAX_VALUE);
        trackingRandom.nextShort(Short.MIN_VALUE, (short) 1.0);
        trackingRandom.nextShort((short) -1.0, (short) 1.0);
        assertEquals(trackingRandom.getCurrentChoiceOffset(), input.requested);
    }

    @Test
    public void testCharIndices() {
        trackingRandom.nextChar(Character.MIN_VALUE, Character.MAX_VALUE);
        trackingRandom.nextChar((char) 0, Character.MAX_VALUE);
        trackingRandom.nextChar(Character.MIN_VALUE, (char) 1.0);
        trackingRandom.nextChar((char) 1.0, (char) 10.0);
        assertEquals(trackingRandom.getCurrentChoiceOffset(), input.requested);
    }

    @Test
    public void testIntegerIndices() {
        trackingRandom.nextInt();
        trackingRandom.nextInt(10);
        trackingRandom.nextInt(Integer.MIN_VALUE, Integer.MAX_VALUE);
        trackingRandom.nextInt(0, Integer.MAX_VALUE);
        trackingRandom.nextInt(Integer.MIN_VALUE, 1);
        trackingRandom.nextInt(-1, 1);
        assertEquals(trackingRandom.getCurrentChoiceOffset(), input.requested);
    }

    @Test
    public void testBooleanIndices() {
        trackingRandom.nextBoolean();
        assertEquals(trackingRandom.getCurrentChoiceOffset(), input.requested);
    }

    @Test
    public void testLongIndices() {
        trackingRandom.nextLong(Long.MIN_VALUE, Long.MAX_VALUE);
        trackingRandom.nextLong(0, Long.MAX_VALUE);
        trackingRandom.nextLong(Long.MIN_VALUE, 1);
        trackingRandom.nextLong(-1, 1);
        assertEquals(trackingRandom.getCurrentChoiceOffset(), input.requested);
    }

    @Test
    public void testBigIntegerIndices() {
        trackingRandom.nextBigInteger(0);
        trackingRandom.nextBigInteger(1);
        trackingRandom.nextBigInteger(10);
        assertEquals(trackingRandom.getCurrentChoiceOffset(), input.requested);
    }

    @Test
    public void testChooseIndices() {
        trackingRandom.choose(List.of(1));
        trackingRandom.choose(List.of(1, 2, 3));
        trackingRandom.choose(List.of(1).toArray());
        trackingRandom.choose(List.of(1, 2, 3).toArray());
        assertEquals(trackingRandom.getCurrentChoiceOffset(), input.requested);
    }

}
