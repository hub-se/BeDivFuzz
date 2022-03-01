package de.hub.se.jqf.fuzz.junit.quickcheck;

import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import edu.berkeley.cs.jqf.fuzz.guidance.StreamBackedRandom;
import edu.berkeley.cs.jqf.fuzz.junit.quickcheck.FastSourceOfRandomness;

import javax.xml.transform.Source;
import java.util.Collection;
import java.util.Random;

public class SplitSourceOfRandomness extends FastSourceOfRandomness {

    protected SourceOfRandomness secondaryRandom;

    public SplitSourceOfRandomness(StreamBackedRandom primaryDelegate, StreamBackedRandom secondaryDelegate) {
        super(primaryDelegate);
        secondaryRandom = new FastSourceOfRandomness(secondaryDelegate);
    }

    public SourceOfRandomness getSecondarySource() {
        return secondaryRandom;
    }

    public byte nextByte(byte min, byte max, boolean usePrimary) {
        return usePrimary ? super.nextByte(min, max) : secondaryRandom.nextByte(min, max);
    }

    public short nextShort(short min, short max, boolean usePrimary) {
        return usePrimary ? super.nextShort(min, max) : secondaryRandom.nextShort(min, max);

    }

    public char nextChar(char min, char max, boolean usePrimary) {
        return usePrimary ? super.nextChar(min, max) : secondaryRandom.nextChar(min, max);
    }

    public int nextInt(boolean usePrimary) {
        return usePrimary ? super.nextInt() : secondaryRandom.nextInt();
    }

    public int nextInt(int n, boolean usePrimary) {
        return usePrimary ? super.nextInt(n) : secondaryRandom.nextInt(n);
    }

    public int nextInt(int min, int max, boolean usePrimary) {
        return usePrimary ? super.nextInt(min, max) : secondaryRandom.nextInt(min, max);
    }

    public boolean nextBoolean(boolean usePrimary) {
        return usePrimary ? super.nextBoolean() : secondaryRandom.nextBoolean();
    }

    public long nextLong(long min, long max, boolean usePrimary) {
        return usePrimary ? super.nextLong(min, max) : secondaryRandom.nextLong(min, max);
    }

    public <T> T choose(Collection<T> items, boolean usePrimary) {
        return usePrimary ? super.choose(items) : secondaryRandom.choose(items);
    }

    public <T> T choose(T[] items, boolean usePrimary) {
        return usePrimary ? super.choose(items) : secondaryRandom.choose(items);
    }

}

