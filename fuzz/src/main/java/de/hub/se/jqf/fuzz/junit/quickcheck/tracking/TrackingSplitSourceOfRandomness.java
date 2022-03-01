package de.hub.se.jqf.fuzz.junit.quickcheck.tracking;

import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import de.hub.se.jqf.fuzz.junit.quickcheck.SplitSourceOfRandomness;
import edu.berkeley.cs.jqf.fuzz.guidance.StreamBackedRandom;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class TrackingSplitSourceOfRandomness extends SplitSourceOfRandomness {

    protected List<ChoiceType> choiceMethodSequence;
    protected TrackingSourceOfRandomness secondaryRandom;

    public TrackingSplitSourceOfRandomness(StreamBackedRandom primaryDelegate, StreamBackedRandom secondaryDelegate) {
        super(primaryDelegate, secondaryDelegate);
        choiceMethodSequence = new LinkedList<>();
        secondaryRandom = new TrackingSourceOfRandomness(secondaryDelegate, choiceMethodSequence);
    }

    public List<ChoiceType> getChoiceSequence() {
        return choiceMethodSequence;
    }

    @Override
    public SourceOfRandomness getSecondarySource() {
        return secondaryRandom;
    }

    @Override
    public byte nextByte(byte min, byte max, boolean usePrimary) {
        if (usePrimary) {
            choiceMethodSequence.add(ChoiceType.BYTE);
            return super.nextByte(min, max);
        }
        else {
            return secondaryRandom.nextByte(min, max);
        }
    }

    @Override
    public short nextShort(short min, short max, boolean usePrimary) {
        if (usePrimary) {
            choiceMethodSequence.add(ChoiceType.SHORT);
            return super.nextShort(min, max);
        }
        else {
            return secondaryRandom.nextShort(min, max);
        }
    }

    @Override
    public char nextChar(char min, char max, boolean usePrimary) {
        if (usePrimary) {
            choiceMethodSequence.add(ChoiceType.CHAR);
            return super.nextChar(min, max);
        }
        else {
            return secondaryRandom.nextChar(min, max);
        }
    }

    @Override
    public int nextInt(boolean usePrimary) {
        if (usePrimary) {
            choiceMethodSequence.add(ChoiceType.INT);
            return super.nextInt();
        }
        else {
            return secondaryRandom.nextInt();
        }
    }

    @Override
    public int nextInt(int n, boolean usePrimary) {
        if (usePrimary) {
            choiceMethodSequence.add(ChoiceType.INT);
            return super.nextInt(n);
        }
        else {
            return secondaryRandom.nextInt(n);
        }
    }

    @Override
    public int nextInt(int min, int max, boolean usePrimary) {
        if (usePrimary) {
            choiceMethodSequence.add(ChoiceType.INT);
            return super.nextInt(min, max);
        }
        else {
            return secondaryRandom.nextInt(min, max);
        }
    }

    @Override
    public boolean nextBoolean(boolean usePrimary) {
        if (usePrimary) {
            choiceMethodSequence.add(ChoiceType.BOOL);
            return super.nextBoolean();
        }
        else {
            return secondaryRandom.nextBoolean();
        }
    }

    @Override
    public long nextLong(long min, long max, boolean usePrimary) {
        if (usePrimary) {
            choiceMethodSequence.add(ChoiceType.LONG);
            return super.nextLong(min, max);
        }
        else {
            return secondaryRandom.nextLong(min, max);
        }
    }

    @Override
    public <T> T choose(Collection<T> items, boolean usePrimary) {
        if (usePrimary) {
            choiceMethodSequence.add(ChoiceType.CHOOSE);
            return super.choose(items);
        }
        else {
            return secondaryRandom.choose(items);
        }
    }

    @Override
    public <T> T choose(T[] items, boolean usePrimary) {
        if (usePrimary) {
            choiceMethodSequence.add(ChoiceType.CHOOSE);
            return super.choose(items);
        }
        else {
            return secondaryRandom.choose(items);
        }
    }

}
