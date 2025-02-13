package de.hub.se.bedivfuzz.junit.quickcheck.tracing;

import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import de.hub.se.bedivfuzz.junit.quickcheck.SplitRandom;
import edu.berkeley.cs.jqf.fuzz.guidance.StreamBackedRandom;
import edu.berkeley.cs.jqf.fuzz.junit.quickcheck.FastSourceOfRandomness;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A source of randomness that distinguishes between structural and value random choices.
 * Additionally, keeps track of the choice indices for each choice type.
 */
public class SplitTracingSourceOfRandomness implements SplitRandom {
    private final List<Choice> structuralIndices;
    private final List<Choice> valueIndices;
    private final ChoiceTracingState trackingState;

    private final SourceOfRandomness random;
    private final TracingSourceOfRandomness structureDelegate;
    private final TracingSourceOfRandomness valueDelegate;

    public SplitTracingSourceOfRandomness(
            InputStream input,
            List<Choice> structuralChoiceIndices,
            List<Choice> valueChoiceIndices
    ) {
        // We need to ignore 24 bytes because we instantiate 3 SourceOfRandomness instances
        StreamBackedRandom delegate = new StreamBackedRandom(input, 3 * Long.BYTES);
        this.random = new FastSourceOfRandomness(delegate);

        this.structuralIndices = structuralChoiceIndices;
        this.valueIndices = valueChoiceIndices;
        this.trackingState = new ChoiceTracingState(delegate);

        this.structureDelegate = new TracingSourceOfRandomness(structuralChoiceIndices, trackingState);
        this.valueDelegate = new TracingSourceOfRandomness(valueChoiceIndices, trackingState);
    }

    // Exposed for testing
    public SplitTracingSourceOfRandomness(InputStream input) {
        this(input, new ArrayList<>(), new ArrayList<>());
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
        byte choice = random.nextByte(min, max);
        trackingState.appendChoiceIndex(structuralIndices);
        return choice;
    }

    @Override
    public void nextStructureBytes(byte[] bytes) {
        random.nextBytes(bytes);
        trackingState.appendChoiceIndex(structuralIndices);
    }

    @Override
    public double nextStructureDouble() {
        double choice = random.nextDouble();
        trackingState.appendChoiceIndex(structuralIndices);
        return choice;
    }

    @Override
    public double nextStructureDouble(double min, double max) {
        double choice = random.nextDouble(min, max);
        trackingState.appendChoiceIndex(structuralIndices);
        return choice;
    }

    @Override
    public float nextStructureFloat() {
        float choice = random.nextFloat();
        trackingState.appendChoiceIndex(structuralIndices);
        return choice;
    }

    @Override
    public float nextStructureFloat(float min, float max) {
        float choice = random.nextFloat(min, max);
        trackingState.appendChoiceIndex(structuralIndices);
        return choice;
    }

    @Override
    public short nextStructureShort(short min, short max) {
        short choice = random.nextShort(min, max);
        trackingState.appendChoiceIndex(structuralIndices);
        return choice;
    }

    @Override
    public char nextStructureChar(char min, char max) {
        char choice = random.nextChar(min, max);
        trackingState.appendChoiceIndex(structuralIndices);
        return choice;
    }

    @Override
    public int nextStructureInt() {
        int choice = random.nextInt();
        trackingState.appendChoiceIndex(structuralIndices);
        return choice;
    }

    @Override
    public int nextStructureInt(int n) {
        int choice = random.nextInt(n);
        trackingState.appendChoiceIndex(structuralIndices);
        return choice;
    }

    @Override
    public int nextStructureInt(int min, int max) {
        int choice = random.nextInt(min, max);
        trackingState.appendChoiceIndex(structuralIndices);
        return choice;
    }

    @Override
    public boolean nextStructureBoolean() {
        boolean choice = random.nextBoolean();
        trackingState.appendChoiceIndex(structuralIndices, -1);
        return choice;
    }

    @Override
    public long nextStructureLong() {
        long choice = random.nextLong();
        trackingState.appendChoiceIndex(structuralIndices);
        return choice;
    }

    @Override
    public long nextStructureLong(long min, long max) {
        long choice = random.nextLong(min, max);
        trackingState.appendChoiceIndex(structuralIndices);
        return choice;
    }

    @Override
    public <T> T chooseStructure(Collection<T> items) {
        T choice = random.choose(items);
        trackingState.appendChoiceIndex(structuralIndices);
        return choice;
    }

    @Override
    public <T> T chooseStructure(T[] items) {
        T choice = random.choose(items);
        trackingState.appendChoiceIndex(structuralIndices);
        return choice;
    }

    /**
     * Value random choices.
     */

    @Override
    public byte nextValueByte(byte min, byte max) {
        byte choice = random.nextByte(min, max);
        trackingState.appendChoiceIndex(valueIndices);
        return choice;
    }

    @Override
    public byte[] nextValueBytes(int count) {
        byte[] choice = random.nextBytes(count);
        trackingState.appendChoiceIndex(valueIndices);
        return choice;
    }

    @Override
    public double nextValueDouble() {
        double choice = random.nextDouble();
        trackingState.appendChoiceIndex(valueIndices);
        return choice;
    }

    @Override
    public double nextValueDouble(double min, double max) {
        double choice = random.nextDouble(min, max);
        trackingState.appendChoiceIndex(valueIndices);
        return choice;
    }

    @Override
    public float nextValueFloat() {
        float choice = random.nextFloat();
        trackingState.appendChoiceIndex(valueIndices);
        return choice;
    }

    @Override
    public float nextValueFloat(float min, float max) {
        float choice = random.nextFloat(min, max);
        trackingState.appendChoiceIndex(valueIndices);
        return choice;
    }

    @Override
    public short nextValueShort(short min, short max) {
        short choice = random.nextShort(min, max);
        trackingState.appendChoiceIndex(valueIndices);
        return choice;
    }

    @Override
    public char nextValueChar(char min, char max) {
        char choice = random.nextChar(min, max);
        trackingState.appendChoiceIndex(valueIndices);
        return choice;
    }

    @Override
    public int nextValueInt() {
        int choice = random.nextInt();
        trackingState.appendChoiceIndex(valueIndices);
        return choice;
    }

    @Override
    public int nextValueInt(int n) {
        int choice = random.nextInt(n);
        trackingState.appendChoiceIndex(valueIndices);
        return choice;
    }

    @Override
    public int nextValueInt(int min, int max) {
        int choice = random.nextInt(min, max);
        trackingState.appendChoiceIndex(valueIndices);
        return choice;
    }

    @Override
    public boolean nextValueBoolean() {
        boolean choice = random.nextBoolean();
        trackingState.appendChoiceIndex(valueIndices, -1);
        return choice;
    }

    @Override
    public long nextValueLong() {
        long choice = random.nextLong();
        trackingState.appendChoiceIndex(valueIndices);
        return choice;
    }

    @Override
    public long nextValueLong(long min, long max) {
        long choice = random.nextLong(min, max);
        trackingState.appendChoiceIndex(valueIndices);
        return choice;
    }

    @Override
    public <T> T chooseValue(Collection<T> items) {
        if (items.size() > 1) {
            T choice = random.choose(items);
            trackingState.appendChoiceIndex(valueIndices);
            return choice;
        } else {
            return random.choose(items);
        }
    }

    @Override
    public <T> T chooseValue(T[] items) {
        T choice = random.choose(items);
        trackingState.appendChoiceIndex(valueIndices);
        return choice;
    }

}
