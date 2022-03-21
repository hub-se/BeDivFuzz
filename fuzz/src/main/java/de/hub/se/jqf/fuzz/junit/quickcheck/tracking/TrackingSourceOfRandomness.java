package de.hub.se.jqf.fuzz.junit.quickcheck.tracking;

import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import edu.berkeley.cs.jqf.fuzz.guidance.StreamBackedRandom;
import edu.berkeley.cs.jqf.fuzz.junit.quickcheck.FastSourceOfRandomness;

import java.util.*;

public class TrackingSourceOfRandomness extends FastSourceOfRandomness {

    // Choice-type sequence signature as simple method counts (indices correspond to the order of method declaration)
   //protected int[] choiceMethodCounts;

    // The actual choice type-sequence
    protected List<ChoiceType> choiceMethodSequence;

    public TrackingSourceOfRandomness(StreamBackedRandom delegate) {
        super(delegate);
        //choiceMethodCounts = new int[10];
        choiceMethodSequence = new LinkedList<>();
    }

    public TrackingSourceOfRandomness(StreamBackedRandom delegate, List<ChoiceType> sequence) {
        super(delegate);
        //assert(counts.length == 10): "Invalid choice count array length.";
        //choiceMethodCounts = counts;
        choiceMethodSequence = sequence;
    }

    public List<ChoiceType> getChoiceSequence() {
        return choiceMethodSequence;
    }

    /*
    public int[] getChoiceCounts() {
        return choiceMethodCounts;
    }

     */

    @Override
    public byte nextByte(byte min, byte max) {
        //choiceMethodCounts[0] += 1;
        choiceMethodSequence.add(ChoiceType.BYTE);
        return super.nextByte(min, max);
    }

    @Override
    public short nextShort(short min, short max) {
        //choiceMethodCounts[1] += 1;
        choiceMethodSequence.add(ChoiceType.SHORT);
        return super.nextShort(min, max);
    }

    @Override
    public char nextChar(char min, char max) {
        //choiceMethodCounts[2] += 1;
        choiceMethodSequence.add(ChoiceType.CHAR);
        return super.nextChar(min, max);
    }

    @Override
    public int nextInt() {
        //choiceMethodCounts[3] += 1;
        choiceMethodSequence.add(ChoiceType.INT);
        return super.nextInt();
    }

    @Override
    public int nextInt(int n) {
        //choiceMethodCounts[4] += 1;
        choiceMethodSequence.add(ChoiceType.INT);
        return super.nextInt(n);
    }

    @Override
    public int nextInt(int min, int max) {
        //choiceMethodCounts[5] += 1;
        choiceMethodSequence.add(ChoiceType.INT);
        return super.nextInt(min, max);
    }

    @Override
    public boolean nextBoolean() {
        //choiceMethodCounts[6] += 1;
        choiceMethodSequence.add(ChoiceType.BOOL);
        return super.nextBoolean();
    }

    @Override
    public long nextLong(long min, long max) {
        //choiceMethodCounts[7] += 1;
        choiceMethodSequence.add(ChoiceType.LONG);
        return super.nextLong(min, max);
    }

    @Override
    public <T> T choose(Collection<T> items) {
        //choiceMethodCounts[8] += 1;
        choiceMethodSequence.add(ChoiceType.CHOOSE);
        return super.choose(items);
    }

    @Override
    public <T> T choose(T[] items) {
        //choiceMethodCounts[9] += 1;
        choiceMethodSequence.add(ChoiceType.CHOOSE);
        return super.choose(items);
    }


}
