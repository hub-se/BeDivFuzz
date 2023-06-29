package de.hub.se.jqf.bedivfuzz.junit.quickcheck;

import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import edu.berkeley.cs.jqf.fuzz.guidance.StreamBackedRandom;
import edu.berkeley.cs.jqf.fuzz.junit.quickcheck.FastSourceOfRandomness;

import java.util.Collection;

/**
 * A source of randomness that is used by
 * {@linkplain SplitGenerator} SplitGenerators
 * to produce random inputs.
 */
public class SplitSourceOfRandomness {

    /** The source of randomness that produces structural random decisions. */
    protected SourceOfRandomness structuralRandom;

    /** The source of randomness that produces value random decisions. */
    protected SourceOfRandomness valueRandom;

    /**
     * Creates a new split source of randomness.
     *
     * @param structuralDelegate a file-backed random number generator for structural decisions
     * @param valueDelegate a file-backed random number generator for value decisions
     */
    public SplitSourceOfRandomness(StreamBackedRandom structuralDelegate, StreamBackedRandom valueDelegate) {
        structuralRandom = new FastSourceOfRandomness(structuralDelegate);
        valueRandom = new FastSourceOfRandomness(valueDelegate);
    }

    public SourceOfRandomness getStructuralRandom() {
        return structuralRandom;
    }

    public SourceOfRandomness getValueRandom() {
        return valueRandom;
    }

    public byte nextStructuralByte(byte min, byte max) {
        return structuralRandom.nextByte(min, max);
    }

    public short nextStructuralShort(short min, short max) {
        return structuralRandom.nextShort(min, max);
    }

    public char nextStructuralChar(char min, char max) {
        return structuralRandom.nextChar(min, max);
    }

    public int nextStructuralInt() {
        return structuralRandom.nextInt();
    }

    public int nextStructuralInt(int n) {
        return structuralRandom.nextInt(n);
    }

    public int nextStructuralInt(int min, int max) {
        return structuralRandom.nextInt(min, max);
    }

    public boolean nextStructuralBoolean() {
        return structuralRandom.nextBoolean();
    }

    public long nextStructuralLong(long min, long max) {
        return structuralRandom.nextLong(min, max);
    }

    public double nextStructuralDouble() {
        return structuralRandom.nextDouble();
    }

    public double nextStructuralDouble(double min, double max) {
        return structuralRandom.nextDouble(min, max);
    }

    public <T> T structuralChoose(Collection<T> items) {
        return structuralRandom.choose(items);
    }

    public <T> T structuralChoose(T[] items) {
        return structuralRandom.choose(items);
    }

    public byte nextValueByte(byte min, byte max) {
        return valueRandom.nextByte(min, max);
    }

    public short nextValueShort(short min, short max) {
        return valueRandom.nextShort(min, max);
    }

    public char nextValueChar(char min, char max) {
        return valueRandom.nextChar(min, max);
    }

    public int nextValueInt() {
        return valueRandom.nextInt();
    }

    public int nextValueInt(int n) {
        return valueRandom.nextInt(n);
    }

    public int nextValueInt(int min, int max) {
        return valueRandom.nextInt(min, max);
    }

    public boolean nextValueBoolean() {
        return valueRandom.nextBoolean();
    }

    public long nextValueLong(long min, long max) {
        return valueRandom.nextLong(min, max);
    }

    public double nextValueDouble() {
        return valueRandom.nextDouble();
    }

    public double nextValueDouble(double min, double max) {
        return valueRandom.nextDouble(min, max);
    }

    public <T> T valueChoose(Collection<T> items) {
        return valueRandom.choose(items);
    }

    public <T> T valueChoose(T[] items) {
        return valueRandom.choose(items);
    }

}

