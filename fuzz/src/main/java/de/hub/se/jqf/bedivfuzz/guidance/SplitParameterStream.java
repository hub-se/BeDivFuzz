package de.hub.se.jqf.bedivfuzz.guidance;

import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

/**
 * A split parameter stream delivering structural and value parameters to the generator.
 */
public class SplitParameterStream {
    private final SplitInput input;
    private final Random random;

    public SplitParameterStream(SplitInput input, Random random) {
        this.input = input;
        this.random = random;
    }

    // Returns an InputStream that delivers structural parameters from a linear array.
    public InputStream createStructuralParameterStream() {
        return new InputStream() {
            int bytesRead = 0;

            @Override
            public int read() throws IOException {
                // Attempt to get a value from the list, or else generate a random value
                return input.getOrGenerateFreshStructure(bytesRead++, random);
            }
        };
    }

    // Returns an InputStream that delivers structural parameters from a linear array.
    public InputStream createValueParameterStream() {
        return new InputStream() {
            int bytesRead = 0;

            @Override
            public int read() throws IOException {
                // Attempt to get a value from the list, or else generate a random value
                return input.getOrGenerateFreshValue(bytesRead++, random);
            }
        };
    }

}
