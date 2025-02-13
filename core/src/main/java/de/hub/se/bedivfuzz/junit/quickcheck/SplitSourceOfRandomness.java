package de.hub.se.bedivfuzz.junit.quickcheck;

import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import edu.berkeley.cs.jqf.fuzz.guidance.StreamBackedRandom;
import edu.berkeley.cs.jqf.fuzz.junit.quickcheck.FastSourceOfRandomness;

import java.io.InputStream;
import java.util.Collection;

/**
 * A source of randomness used by {@linkplain SplitGenerator} SplitGenerators to generate
 * random inputs. Backed by two random sources that are responsible for producing
 * either structural or value random data of the input.
 */
public class SplitSourceOfRandomness implements SplitRandom {

    /** The source of randomness that produces structural random data. */
    private final SourceOfRandomness structure;

    /** The source of randomness that produces value random data. */
    private final SourceOfRandomness value;

    /**
     * Creates a new split source of randomness.
     *
     * @param structuralDelegate a file-backed random number generator for structural decisions
     * @param valueDelegate a file-backed random number generator for value decisions
     */
    public SplitSourceOfRandomness(StreamBackedRandom structuralDelegate, StreamBackedRandom valueDelegate) {
        structure = new FastSourceOfRandomness(structuralDelegate);
        value = new FastSourceOfRandomness(valueDelegate);
    }

    public SplitSourceOfRandomness(InputStream input) {
        StreamBackedRandom delegate = new StreamBackedRandom(input, 2 * Long.BYTES);
        structure = new FastSourceOfRandomness(delegate);
        value = new FastSourceOfRandomness(delegate);
    }

    public SourceOfRandomness getStructureDelegate() {
        return structure;
    }

    public SourceOfRandomness getValueDelegate() {
        return value;
    }

    @Override
    public byte nextStructureByte(byte min, byte max) {
        return structure.nextByte(min, max);
    }

    @Override
    public void nextStructureBytes(byte[] bytes) {
        structure.nextBytes(bytes);
    }

    @Override
    public double nextStructureDouble() {
        return structure.nextDouble();
    }

    @Override
    public double nextStructureDouble(double min, double max) {
        return structure.nextDouble(min, max);
    }

    @Override
    public float nextStructureFloat() {
        return structure.nextFloat();
    }

    @Override
    public float nextStructureFloat(float min, float max) {
        return structure.nextFloat(min, max);
    }

    @Override
    public short nextStructureShort(short min, short max) {
        return structure.nextShort(min, max);
    }

    @Override
    public char nextStructureChar(char min, char max) {
        return structure.nextChar(min, max);
    }

    @Override
    public int nextStructureInt() {
        return structure.nextInt();
    }

    @Override
    public int nextStructureInt(int n) {
        return structure.nextInt(n);
    }

    @Override
    public int nextStructureInt(int min, int max) {
        return structure.nextInt(min, max);
    }

    @Override
    public boolean nextStructureBoolean() {
        return structure.nextBoolean();
    }

    @Override
    public long nextStructureLong() {
        return structure.nextLong();
    }

    @Override
    public long nextStructureLong(long min, long max) {
        return structure.nextLong(min, max);
    }

    @Override
    public <T> T chooseStructure(Collection<T> items) {
        return structure.choose(items);
    }

    @Override
    public <T> T chooseStructure(T[] items) {
        return structure.choose(items);
    }

    /**
     * Value random choices.
     */

    @Override
    public byte nextValueByte(byte min, byte max) {
        return value.nextByte(min, max);
    }

    @Override
    public byte[] nextValueBytes(int count) {
        return value.nextBytes(count);
    }

    @Override
    public double nextValueDouble() {
        return value.nextDouble();
    }

    @Override
    public double nextValueDouble(double min, double max) {
        return value.nextDouble(min, max);
    }

    @Override
    public float nextValueFloat() {
        return value.nextFloat();
    }

    @Override
    public float nextValueFloat(float min, float max) {
        return value.nextFloat(min, max);
    }
    @Override
    public short nextValueShort(short min, short max) {
        return value.nextShort(min, max);
    }

    @Override
    public char nextValueChar(char min, char max) {
        return value.nextChar(min, max);
    }

    @Override
    public int nextValueInt() {
        return value.nextInt();
    }

    @Override
    public int nextValueInt(int n) {
        return value.nextInt(n);
    }

    @Override
    public int nextValueInt(int min, int max) {
        return value.nextInt(min, max);
    }

    @Override
    public boolean nextValueBoolean() {
        return value.nextBoolean();
    }

    @Override
    public long nextValueLong() {
        return value.nextLong();
    }

    @Override
    public long nextValueLong(long min, long max) {
        return value.nextLong(min, max);
    }

    @Override
    public <T> T chooseValue(Collection<T> items) {
        return value.choose(items);
    }

    @Override
    public <T> T chooseValue(T[] items) {
        return value.choose(items);
    }

}
