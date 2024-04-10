package de.hub.se.jqf.bedivfuzz.junit.quickcheck;

import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import edu.berkeley.cs.jqf.fuzz.junit.quickcheck.FastSourceOfRandomness;

import java.util.Collection;

/**
 * A source of randomness used by {@linkplain SplitGenerator} SplitGenerators to generate random inputs.
 * Simply delegates all structure/value random calls to super.
 */
public class SplitSourceOfRandomness extends FastSourceOfRandomness implements SplitRandom {

    /**
     * Creates a new split source of randomness.
     *
     * @param  delegate a file-backed random number generator for random decisions
     *
     */
    public SplitSourceOfRandomness(SeedingStreamBackedRandom delegate) {
        super(delegate);
        delegate.initialize();
    }

    public SourceOfRandomness getStructureDelegate() {
        return this;
    }

    public SourceOfRandomness getValueDelegate() {
        return this;
    }

    @Override
    public byte nextStructureByte(byte min, byte max) {
        return nextByte(min, max);
    }

    @Override
    public void nextStructureBytes(byte[] bytes) {
        nextBytes(bytes);
    }

    @Override
    public double nextStructureDouble() {
        return nextDouble();
    }

    @Override
    public double nextStructureDouble(double min, double max) {
        return nextDouble(min, max);
    }

    @Override
    public float nextStructureFloat() {
        return nextFloat();
    }

    @Override
    public float nextStructureFloat(float min, float max) {
        return nextFloat(min, max);
    }

    @Override
    public short nextStructureShort(short min, short max) {
        return nextShort(min, max);
    }

    @Override
    public char nextStructureChar(char min, char max) {
        return nextChar(min, max);
    }

    @Override
    public int nextStructureInt() {
        return nextInt();
    }

    @Override
    public int nextStructureInt(int n) {
        return nextInt(n);
    }

    @Override
    public int nextStructureInt(int min, int max) {
        return nextInt(min, max);
    }

    @Override
    public boolean nextStructureBoolean() {
        return nextBoolean();
    }

    @Override
    public long nextStructureLong() {
        return nextLong();
    }

    @Override
    public long nextStructureLong(long min, long max) {
        return nextLong(min, max);
    }

    @Override
    public <T> T chooseStructure(Collection<T> items) {
        return choose(items);
    }

    @Override
    public <T> T chooseStructure(T[] items) {
        return choose(items);
    }

    /**
     * Value random choices.
     */

    @Override
    public byte nextValueByte(byte min, byte max) {
        return nextByte(min, max);
    }

    @Override
    public byte[] nextValueBytes(int count) {
        return nextBytes(count);
    }

    @Override
    public double nextValueDouble() {
        return nextDouble();
    }

    @Override
    public double nextValueDouble(double min, double max) {
        return nextDouble(min, max);
    }

    @Override
    public float nextValueFloat() {
        return nextFloat();
    }

    @Override
    public float nextValueFloat(float min, float max) {
        return nextFloat(min, max);
    }
    @Override
    public short nextValueShort(short min, short max) {
        return nextShort(min, max);
    }

    @Override
    public char nextValueChar(char min, char max) {
        return nextChar(min, max);
    }

    @Override
    public int nextValueInt() {
        return nextInt();
    }

    @Override
    public int nextValueInt(int n) {
        return nextInt(n);
    }

    @Override
    public int nextValueInt(int min, int max) {
        return nextInt(min, max);
    }

    @Override
    public boolean nextValueBoolean() {
        return nextBoolean();
    }

    @Override
    public long nextValueLong() {
        return nextLong();
    }

    @Override
    public long nextValueLong(long min, long max) {
        return nextLong(min, max);
    }

    @Override
    public <T> T chooseValue(Collection<T> items) {
        return choose(items);
    }

    @Override
    public <T> T chooseValue(T[] items) {
        return choose(items);
    }

}
