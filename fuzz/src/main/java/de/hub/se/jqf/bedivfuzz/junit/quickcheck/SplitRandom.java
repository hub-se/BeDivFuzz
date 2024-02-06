package de.hub.se.jqf.bedivfuzz.junit.quickcheck;

import com.pholser.junit.quickcheck.random.SourceOfRandomness;

import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;

public interface SplitRandom {

    SourceOfRandomness getValueDelegate();

    SourceOfRandomness getStructureDelegate();

    /**
     * Structure random choices.
     */

    default byte nextStructureByte(byte min, byte max) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    default byte[] nextStructureBytes(int count) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    default void nextStructureBytes(byte[] bytes) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    default double nextStructureDouble() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    default double nextStructureDouble(double min, double max) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    default float nextStructureFloat() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    default float nextStructureFloat(float min, float max) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }
    default short nextStructureShort(short min, short max) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    default char nextStructureChar(char min, char max) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    default int nextStructureInt() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    default int nextStructureInt(int n) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    default int nextStructureInt(int min, int max) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    default boolean nextStructureBoolean() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    default long nextStructureLong() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    default long nextStructureLong(long min, long max) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    default BigInteger nextStructureBigInteger(int numberOfBits) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    default <T> T chooseStructure(Collection<T> items) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    default <T> T chooseStructure(T[] items) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    default double nextStructureGaussian() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    default Instant nextStructureInstant(Instant min, Instant max) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    default Duration nextStructureDuration(Duration min, Duration max) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    /**
     * Value random choices.
     */

    default byte nextValueByte(byte min, byte max) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    default byte[] nextValueBytes(int count) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    default void nextValueBytes(byte[] bytes) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    default double nextValueDouble() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    default double nextValueDouble(double min, double max) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    default float nextValueFloat() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    default float nextValueFloat(float min, float max) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }
    default short nextValueShort(short min, short max) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    default char nextValueChar(char min, char max) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    default int nextValueInt() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    default int nextValueInt(int n) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    default int nextValueInt(int min, int max) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    default boolean nextValueBoolean() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    default long nextValueLong() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    default long nextValueLong(long min, long max) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    default BigInteger nextValueBigInteger(int numberOfBits) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    default <T> T chooseValue(Collection<T> items) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    default <T> T chooseValue(T[] items) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    default double nextValueGaussian() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    default Instant nextValueInstant(Instant min, Instant max) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    default Duration nextValueDuration(Duration min, Duration max) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

}
