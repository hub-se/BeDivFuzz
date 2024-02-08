package de.hub.se.jqf.bedivfuzz.junit.quickcheck.tracking;

import edu.berkeley.cs.jqf.fuzz.guidance.StreamBackedRandom;
import edu.berkeley.cs.jqf.fuzz.junit.quickcheck.FastSourceOfRandomness;

import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class TrackingSourceOfRandomness extends FastSourceOfRandomness {

    private final List<Choice> choiceIndices;
    private final ChoiceTrackingState trackingState;

    public TrackingSourceOfRandomness(StreamBackedRandom delegate) {
        super(delegate);

        this.choiceIndices = new ArrayList<>();
        this.trackingState = new ChoiceTrackingState(delegate);
    }

    public TrackingSourceOfRandomness(List<Choice> choiceIndices, ChoiceTrackingState trackingState) {
        super(trackingState.getDelegate());
        this.choiceIndices = choiceIndices;
        this.trackingState = trackingState;
    }

    public List<Choice> getChoiceIndices() {
        return choiceIndices;
    }

    public int getCurrentChoiceOffset() {
        return trackingState.getChoiceOffset();
    }

    @Override
    public byte nextByte(byte min, byte max) {
        byte choice = super.nextByte(min, max);
        trackingState.appendChoiceIndex(choiceIndices);
        return choice;
    }

    @Override
    public byte[] nextBytes(int count) {
        byte[] choice = super.nextBytes(count);
        trackingState.appendChoiceIndex(choiceIndices);
        return choice;
    }

    @Override
    public void nextBytes(byte[] bytes) {
        super.nextBytes(bytes);
        trackingState.appendChoiceIndex(choiceIndices);
    }

    @Override
    public double nextDouble() {
        double choice = super.nextDouble();
        trackingState.appendChoiceIndex(choiceIndices);
        return choice;
    }

    @Override
    public double nextDouble(double min, double max) {
        double choice = super.nextDouble(min, max);
        trackingState.appendChoiceIndex(choiceIndices);
        return choice;
    }

    @Override
    public float nextFloat() {
        float choice = super.nextFloat();
        trackingState.appendChoiceIndex(choiceIndices);
        return choice;
    }

    @Override
    public float nextFloat(float min, float max) {
        float choice = super.nextFloat(min, max);
        trackingState.appendChoiceIndex(choiceIndices);
        return choice;
    }

    @Override
    public short nextShort(short min, short max) {
        short choice = super.nextShort(min, max);
        trackingState.appendChoiceIndex(choiceIndices);
        return choice;
    }

    @Override
    public char nextChar(char min, char max) {
        char choice = super.nextChar(min, max);
        trackingState.appendChoiceIndex(choiceIndices);
        return choice;
    }

    @Override
    public int nextInt() {
        int choice = super.nextInt();
        trackingState.appendChoiceIndex(choiceIndices);
        return choice;
    }

    @Override
    public int nextInt(int n) {
        int choice = super.nextInt(n);
        trackingState.appendChoiceIndex(choiceIndices);
        return choice;
    }

    @Override
    public int nextInt(int min, int max) {
        int choice = super.nextInt(min, max);
        trackingState.appendChoiceIndex(choiceIndices);
        return choice;
    }

    @Override
    public boolean nextBoolean() {
        boolean choice = super.nextBoolean();
        trackingState.appendChoiceIndex(choiceIndices, -1);
        return choice;
    }

    @Override
    public long nextLong() {
        long choice = super.nextLong();
        trackingState.appendChoiceIndex(choiceIndices);
        return choice;
    }

    @Override
    public long nextLong(long min, long max) {
        long choice = super.nextLong(min, max);
        trackingState.appendChoiceIndex(choiceIndices);
        return choice;
    }

    @Override
    public BigInteger nextBigInteger(int numberOfBits) {
        BigInteger choice = super.nextBigInteger(numberOfBits);
        trackingState.appendChoiceIndex(choiceIndices);
        return choice;
    }

    /* Choose methods delegate to nextInt()
    @Override
    public <T> T choose(Collection<T> items) {
        return super.choose(items);
    }

    @Override
    public <T> T choose(T[] items) {
        return super.choose(items);
    }
    */

    @Override
    public double nextGaussian() {
        throw new UnsupportedOperationException("nextGaussian() not yet implemented.");
    }

    @Override
    public Instant nextInstant(Instant min, Instant max) {
        throw new UnsupportedOperationException("nextInstant(Instant, Instant) not yet implemented.");
    }

    @Override
    public Duration nextDuration(Duration min, Duration max) {
        throw new UnsupportedOperationException("nextDuration(Duration, Duration) not yet implemented.");
    }
}
