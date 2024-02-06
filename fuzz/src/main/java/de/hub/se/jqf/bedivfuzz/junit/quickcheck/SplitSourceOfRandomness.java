package de.hub.se.jqf.bedivfuzz.junit.quickcheck;

import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import edu.berkeley.cs.jqf.fuzz.guidance.StreamBackedRandom;
import edu.berkeley.cs.jqf.fuzz.junit.quickcheck.FastSourceOfRandomness;

import java.io.InputStream;
import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
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

    public byte nextStructureByte(byte min, byte max) {
        return structure.nextByte(min, max);
    }

    public byte[] nextStructureBytes(int count) {
        return structure.nextBytes(count);
    }

    public void nextStructureBytes(byte[] bytes) {
        structure.nextBytes(bytes);
    }

    public double nextStructureDouble() {
        return structure.nextDouble();
    }

    public double nextStructureDouble(double min, double max) {
        return structure.nextDouble(min, max);
    }

    public float nextStructureFloat() {
        return structure.nextFloat();
    }

    public float nextStructureFloat(float min, float max) {
        return structure.nextFloat(min, max);
    }

    public short nextStructureShort(short min, short max) {
        return structure.nextShort(min, max);
    }

    public char nextStructureChar(char min, char max) {
        return structure.nextChar(min, max);
    }

    public int nextStructureInt() {
        return structure.nextInt();
    }

    public int nextStructureInt(int n) {
        return structure.nextInt(n);
    }

    public int nextStructureInt(int min, int max) {
        return structure.nextInt(min, max);
    }

    public boolean nextStructureBoolean() {
        return structure.nextBoolean();
    }

    public long nextStructureLong() {
        return structure.nextLong();
    }

    public long nextStructureLong(long min, long max) {
        return structure.nextLong(min, max);
    }

    public BigInteger nextStructureBigInteger(int numberOfBits) {
        return structure.nextBigInteger(numberOfBits);
    }

    public <T> T chooseStructure(Collection<T> items) {
        return structure.choose(items);
    }

    public <T> T chooseStructure(T[] items) {
        return structure.choose(items);
    }

    public double nextStructureGaussian() {
        return structure.nextGaussian();
    }

    public Instant nextStructureInstant(Instant min, Instant max) {
        return structure.nextInstant(min, max);
    }

    public Duration nextStructureDuration(Duration min, Duration max) {
        return structure.nextDuration(min, max);
    }

    /**
     * Value random choices.
     */

    public byte nextValueByte(byte min, byte max) {
        return value.nextByte(min, max);
    }

    public byte[] nextValueBytes(int count) {
        return value.nextBytes(count);
    }

    public void nextValueBytes(byte[] bytes) {
        value.nextBytes(bytes);
    }

    public double nextValueDouble() {
        return value.nextDouble();
    }

    public double nextValueDouble(double min, double max) {
        return value.nextDouble(min, max);
    }

    public float nextValueFloat() {
        return value.nextFloat();
    }

    public float nextValueFloat(float min, float max) {
        return value.nextFloat(min, max);
    }
    public short nextValueShort(short min, short max) {
        return value.nextShort(min, max);
    }

    public char nextValueChar(char min, char max) {
        return value.nextChar(min, max);
    }

    public int nextValueInt() {
        return value.nextInt();
    }

    public int nextValueInt(int n) {
        return value.nextInt(n);
    }

    public int nextValueInt(int min, int max) {
        return value.nextInt(min, max);
    }

    public boolean nextValueBoolean() {
        return value.nextBoolean();
    }

    public long nextValueLong() {
        return value.nextLong();
    }

    public long nextValueLong(long min, long max) {
        return value.nextLong(min, max);
    }

    public BigInteger nextValueBigInteger(int numberOfBits) {
        return value.nextBigInteger(numberOfBits);
    }

    public <T> T chooseValue(Collection<T> items) {
        return value.choose(items);
    }

    public <T> T chooseValue(T[] items) {
        return value.choose(items);
    }

    public double nextValueGaussian() {
        return value.nextGaussian();
    }

    public Instant nextValueInstant(Instant min, Instant max) {
        return value.nextInstant(min, max);
    }

    public Duration nextValueDuration(Duration min, Duration max) {
        return value.nextDuration(min, max);
    }

}
