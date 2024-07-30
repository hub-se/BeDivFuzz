package de.hub.se.jqf.bedivfuzz.util;

/**
 * Simple wrapper class for storing behavioral diversity metrics.
 */
public class BehavioralDiversityMetrics {
    /** Diversity indices. */
    protected double b0 = 0;
    protected double b1 = 0;
    protected double b2 = 0;

    /** Default constructor. */
    public BehavioralDiversityMetrics() {}

    public double b0() {
        return b0;
    }

    public double b1() {
        return b1;
    }

    public double b2() {
        return b2;
    }
}
