package de.hub.se.jqf.fuzz.div;

import edu.berkeley.cs.jqf.fuzz.ei.ZestGuidance;
import edu.berkeley.cs.jqf.fuzz.ei.ZestGuidance.Input;
import edu.berkeley.cs.jqf.fuzz.ei.ZestGuidance.LinearInput;
import edu.berkeley.cs.jqf.fuzz.util.Coverage;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;
import java.util.Set;

public class SplitLinearInput implements SplitInput {
    // Global random instance for new input values
    protected static Random random = new Random();

    public final Input primaryInput;
    public final Input secondaryInput;

    /**
     * The files where this input is saved.
     *
     * <p>This field is null for inputs that are not saved.</p>
     */
    File primarySaveFile = null;
    File secondarySaveFile = null;


    /**
     * An ID for a saved input.
     *
     * <p>This field is -1 for inputs that are not saved.</p>
     */
    int id;

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
    Coverage coverage = null;

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
     * Whether this input resulted in a valid run.
     */
    boolean valid = false;

    /**
     * The set of coverage keys for which this input is
     * responsible.
     *
     * <p>This field is null for inputs that are not saved.</p>
     *
     * <p>Each coverage key appears in the responsibility set
     * of exactly one saved input, and all covered keys appear
     * in at least some responsibility set. Hence, this list
     * needs to be kept in-sync with {@link BeDivFuzzGuidance#responsibleInputs}.</p>
     */
    Set<Object> responsibilities = null;

    public SplitLinearInput(Input primary, Input secondary) {
        this.primaryInput = primary;
        this.secondaryInput = secondary;
    }

    public void gc() {
        primaryInput.gc();
        secondaryInput.gc();
    }

    public int size() {
        return primaryInput.size() + secondaryInput.size();
    }

    public boolean isFavored() {
        return responsibilities.size() > 0;
    }

    public InputStream createPrimaryParameterStream() {
        // Return an input stream that reads bytes from a linear array
        return new InputStream() {
            int bytesRead = 0;

            @Override
            public int read() throws IOException {
                assert primaryInput instanceof ZestGuidance.LinearInput : "BeDivGuidance should only mutate LinearInput(s)";

                // For linear inputs, get with key = bytesRead (which is then incremented)
                LinearInput linearInput = (LinearInput) primaryInput;
                // Attempt to get a value from the list, or else generate a random value
                int ret = linearInput.getOrGenerateFresh(bytesRead++, random);
                // infoLog("read(%d) = %d", bytesRead, ret);
                return ret;
            }
        };
    }

    public InputStream createSecondaryParameterStream() {
        // Return an input stream that reads bytes from a linear array
        return new InputStream() {
            int bytesRead = 0;

            @Override
            public int read() throws IOException {
                assert secondaryInput instanceof ZestGuidance.LinearInput : "BeDivGuidance should only mutate LinearInput(s)";

                // For linear inputs, get with key = bytesRead (which is then incremented)
                LinearInput linearInput = (LinearInput) secondaryInput;
                // Attempt to get a value from the list, or else generate a random value
                int ret = linearInput.getOrGenerateFresh(bytesRead++, random);
                // infoLog("read(%d) = %d", bytesRead, ret);
                return ret;
            }
        };
    }
}
