package de.hub.se.jqf.bedivfuzz.guidance;

import edu.berkeley.cs.jqf.fuzz.util.Coverage;
import edu.berkeley.cs.jqf.fuzz.util.ICoverage;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;

import java.io.File;
import java.util.Random;

import static java.lang.Math.ceil;
import static java.lang.Math.log;

/**
 * A test input represented by a split sequence of parametric bytes.
 * Adapted from {@linkplain edu.berkeley.cs.jqf.fuzz.ei.ZestGuidance.Input Input}.
 */
public abstract class SplitInput {

    /**
     * The file where the structural bytes are saved.
     *
     * <p>This field is null for inputs that are not saved.</p>
     */
    protected File structuralSaveFile = null;

    /**
     * The file where the value bytes are saved.
     *
     * <p>This field is null for inputs that are not saved.</p>
     */
    protected File valueSaveFile = null;

    /**
     * An ID for a saved input.
     *
     * <p>This field is -1 for inputs that are not saved.</p>
     */
    int id;

    /**
     * Whether this input is favored.
     */
    boolean favored;

    /**
     * The description for this input.
     *
     * <p>This field is modified by the construction and mutation
     * operations.</p>
     */
    String desc;

    /**
     * The run coverage for this input, if the input is saved.
     *
     * <p>This field is null for inputs that are not saved.</p>
     */
    ICoverage coverage = null;

    /**
     * The number of non-zero elements in `coverage`.
     *
     * <p>This field is -1 for inputs that are not saved.</p>
     *
     * <p></p>When this field is non-negative, the information is
     * redundant (can be computed using {@link Coverage#getNonZeroCount()}),
     * but we store it here for performance reasons.</p>
     */
    int nonZeroCoverage = -1;

    /**
     * The number of mutant children spawned from this input that
     * were saved.
     *
     * <p>This field is -1 for inputs that are not saved.</p>
     */
    int offspring = -1;

    /**
     * The set of coverage keys for which this input is
     * responsible.
     *
     * <p>This field is null for inputs that are not saved.</p>
     */
    IntHashSet responsibilities = null;

    /**
     * Create an empty input.
     */
    public SplitInput() {
        desc = "random";
    }

    /**
     * Create a copy of an existing input.
     *
     * @param toClone the input map to clone
     */
    public SplitInput(SplitInput toClone) {
        desc = String.format("src:%06d", toClone.id);
    }

    public abstract int getOrGenerateFreshStructure(Integer key, Random random);
    public abstract int getOrGenerateFreshValue(Integer key, Random random);
    public abstract int size();
    public abstract void gc();

    /**
     * Sets this input to be favored for fuzzing.
     */
    public void setFavored() {
        favored = true;
    }

    /**
     * Returns whether this input should be favored for fuzzing.
     *
     * <p>An input is favored if it is responsible for covering
     * at least one branch.</p>
     *
     * @return whether this input is favored
     */
    public boolean isFavored() {
        return favored;
    }

    /**
     * Sample from a geometric distribution with given mean.
     * Utility method used in implementing mutation operations.
     *
     * @param random a pseudo-random number generator
     * @param mean the mean of the distribution
     * @return a randomly sampled value
     */
    public static int sampleGeometric(Random random, double mean) {
        double p = 1 / mean;
        double uniform = random.nextDouble();
        return (int) ceil(log(1 - uniform) / log(1 - p));
    }
}
