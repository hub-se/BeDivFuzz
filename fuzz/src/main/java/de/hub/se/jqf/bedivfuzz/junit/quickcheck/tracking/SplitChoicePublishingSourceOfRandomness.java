package de.hub.se.jqf.bedivfuzz.junit.quickcheck.tracking;

import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import de.hub.se.jqf.bedivfuzz.junit.quickcheck.SeedingStreamBackedRandom;
import de.hub.se.jqf.bedivfuzz.junit.quickcheck.SplitRandom;
import edu.berkeley.cs.jqf.fuzz.junit.quickcheck.FastSourceOfRandomness;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SplitChoicePublishingSourceOfRandomness extends FastSourceOfRandomness implements SplitRandom {
    private final List<Choice> structuralIndices;
    private final List<Choice> valueIndices;
    private final ChoiceTrackingState trackingState;

    private final ChoicePublishingSourceOfRandomness structureDelegate;
    private final ChoicePublishingSourceOfRandomness valueDelegate;

    public SplitChoicePublishingSourceOfRandomness(
            SeedingStreamBackedRandom delegate,
            List<Choice> structuralChoiceIndices,
            List<Choice> valueChoiceIndices
    ) {
        super(delegate);
        this.structuralIndices = structuralChoiceIndices;
        this.valueIndices = valueChoiceIndices;
        this.trackingState = new ChoiceTrackingState(delegate);

        this.structureDelegate = new ChoicePublishingSourceOfRandomness(structuralChoiceIndices, trackingState);
        this.valueDelegate = new ChoicePublishingSourceOfRandomness(valueChoiceIndices, trackingState);
        delegate.initialize();
    }

    // Exposed for testing
    public SplitChoicePublishingSourceOfRandomness(SeedingStreamBackedRandom delegate) {
        this(delegate, new ArrayList<>(), new ArrayList<>());
    }

    public SourceOfRandomness getStructureDelegate() {
        return structureDelegate;
    }

    public SourceOfRandomness getValueDelegate() {
        return valueDelegate;
    }

    public int getCurrentChoiceOffset() {
        return trackingState.getChoiceOffset();
    }

    @Override
    public byte nextStructureByte(byte min, byte max) {
        byte choice = nextByte(min, max);
        trackingState.appendChoiceIndex(structuralIndices);
        return choice;
    }

    @Override
    public void nextStructureBytes(byte[] bytes) {
        nextBytes(bytes);
        trackingState.appendChoiceIndex(structuralIndices);
    }

    @Override
    public double nextStructureDouble() {
        double choice = nextDouble();
        trackingState.appendChoiceIndex(structuralIndices);
        return choice;
    }

    @Override
    public double nextStructureDouble(double min, double max) {
        double choice = nextDouble(min, max);
        trackingState.appendChoiceIndex(structuralIndices);
        return choice;
    }

    @Override
    public float nextStructureFloat() {
        float choice = nextFloat();
        trackingState.appendChoiceIndex(structuralIndices);
        return choice;
    }

    @Override
    public float nextStructureFloat(float min, float max) {
        float choice = nextFloat(min, max);
        trackingState.appendChoiceIndex(structuralIndices);
        return choice;
    }

    @Override
    public short nextStructureShort(short min, short max) {
        short choice = nextShort(min, max);
        trackingState.appendChoiceIndex(structuralIndices);
        return choice;
    }

    @Override
    public char nextStructureChar(char min, char max) {
        char choice = nextChar(min, max);
        trackingState.appendChoiceIndex(structuralIndices);
        return choice;
    }

    @Override
    public int nextStructureInt() {
        int choice = nextInt();
        trackingState.appendChoiceIndex(structuralIndices);
        return choice;
    }

    @Override
    public int nextStructureInt(int n) {
        int choice = nextInt(n);
        trackingState.appendChoiceIndex(structuralIndices);
        return choice;
    }

    @Override
    public int nextStructureInt(int min, int max) {
        int choice = nextInt(min, max);
        trackingState.appendChoiceIndex(structuralIndices);
        return choice;
    }

    @Override
    public boolean nextStructureBoolean() {
        boolean choice = nextBoolean();
        trackingState.appendChoiceIndex(structuralIndices, -1);
        return choice;
    }

    @Override
    public long nextStructureLong() {
        long choice = nextLong();
        trackingState.appendChoiceIndex(structuralIndices);
        return choice;
    }

    @Override
    public long nextStructureLong(long min, long max) {
        long choice = nextLong(min, max);
        trackingState.appendChoiceIndex(structuralIndices);
        return choice;
    }

    @Override
    public <T> T chooseStructure(Collection<T> items) {
        T choice = choose(items);
        trackingState.appendChoiceIndex(structuralIndices);
        return choice;
    }

    @Override
    public <T> T chooseStructure(T[] items) {
        T choice = choose(items);
        trackingState.appendChoiceIndex(structuralIndices);
        return choice;
    }

    /**
     * Value random choices.
     */

    @Override
    public byte nextValueByte(byte min, byte max) {
        byte choice = nextByte(min, max);
        trackingState.appendChoiceIndex(valueIndices);
        return choice;
    }

    @Override
    public byte[] nextValueBytes(int count) {
        byte[] choice = nextBytes(count);
        trackingState.appendChoiceIndex(valueIndices);
        return choice;
    }

    @Override
    public double nextValueDouble() {
        double choice = nextDouble();
        trackingState.appendChoiceIndex(valueIndices);
        return choice;
    }

    @Override
    public double nextValueDouble(double min, double max) {
        double choice = nextDouble(min, max);
        trackingState.appendChoiceIndex(valueIndices);
        return choice;
    }

    @Override
    public float nextValueFloat() {
        float choice = nextFloat();
        trackingState.appendChoiceIndex(valueIndices);
        return choice;
    }

    @Override
    public float nextValueFloat(float min, float max) {
        float choice = nextFloat(min, max);
        trackingState.appendChoiceIndex(valueIndices);
        return choice;
    }

    @Override
    public short nextValueShort(short min, short max) {
        short choice = nextShort(min, max);
        trackingState.appendChoiceIndex(valueIndices);
        return choice;
    }

    @Override
    public char nextValueChar(char min, char max) {
        char choice = nextChar(min, max);
        trackingState.appendChoiceIndex(valueIndices);
        return choice;
    }

    @Override
    public int nextValueInt() {
        int choice = nextInt();
        trackingState.appendChoiceIndex(valueIndices);
        return choice;
    }

    @Override
    public int nextValueInt(int n) {
        int choice = nextInt(n);
        trackingState.appendChoiceIndex(valueIndices);
        return choice;
    }

    @Override
    public int nextValueInt(int min, int max) {
        int choice = nextInt(min, max);
        trackingState.appendChoiceIndex(valueIndices);
        return choice;
    }

    @Override
    public boolean nextValueBoolean() {
        boolean choice = nextBoolean();
        trackingState.appendChoiceIndex(valueIndices, -1);
        return choice;
    }

    @Override
    public long nextValueLong() {
        long choice = nextLong();
        trackingState.appendChoiceIndex(valueIndices);
        return choice;
    }

    @Override
    public long nextValueLong(long min, long max) {
        long choice = nextLong(min, max);
        trackingState.appendChoiceIndex(valueIndices);
        return choice;
    }

    @Override
    public <T> T chooseValue(Collection<T> items) {
        if (items.size() > 1) {
            T choice = choose(items);
            trackingState.appendChoiceIndex(valueIndices);
            return choice;
        } else {
            return choose(items);
        }
    }

    @Override
    public <T> T chooseValue(T[] items) {
        T choice = choose(items);
        trackingState.appendChoiceIndex(valueIndices);
        return choice;
    }

}
