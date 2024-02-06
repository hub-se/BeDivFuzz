package de.hub.se.jqf.bedivfuzz.examples.bcel;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Size;
import de.hub.se.jqf.bedivfuzz.junit.quickcheck.SplitGenerator;
import de.hub.se.jqf.bedivfuzz.junit.quickcheck.SplitRandom;

import static com.pholser.junit.quickcheck.internal.Ranges.Type.INTEGRAL;
import static com.pholser.junit.quickcheck.internal.Ranges.checkRange;

public final class SplitJavaIdentifierGenerator extends SplitGenerator<String> {
    private static final char MIN_VALID_CHAR = '$';
    private static final char MAX_VALID_CHAR = 'z';
    private int minSize = 1;
    private int maxSize = 30;

    public SplitJavaIdentifierGenerator() {
        super(String.class);
    }

    @Override
    public String generate(SplitRandom random, GenerationStatus status) {
        return generate(random);
    }

    @SuppressWarnings("unused")
    public void configure(Size size) {
        if (size.min() <= 0) {
            throw new IllegalArgumentException("Minimum size must be positive");
        }
        checkRange(INTEGRAL, size.min(), size.max());
        minSize = size.min();
        maxSize = size.max();
    }

    public String generate(SplitRandom random) {
        int size = random.nextStructureInt(minSize, maxSize);
        char[] values = new char[size];
        values[0] = generateJavaIdentifierStart(random);
        for (int i = 1; i < values.length; i++) {
            values[i] = generateJavaIdentifierPart(random);
        }
        return new String(values);
    }

    static char generateJavaIdentifierStart(SplitRandom random) {
        char c = random.nextValueChar(MIN_VALID_CHAR, MAX_VALID_CHAR);
        if (!Character.isJavaIdentifierStart(c)) {
            return mapToAlpha(c);
        }
        return c;
    }

    static char generateJavaIdentifierPart(SplitRandom random) {
        char c = random.nextValueChar(MIN_VALID_CHAR, MAX_VALID_CHAR);
        if (!Character.isJavaIdentifierPart(c)) {
            return mapToAlpha(c);
        }
        return c;
    }

    static char mapToAlpha(char c) {
        char min = 'a';
        char max = 'z';
        int range = max - min + 1;
        return (char) ((c % range) + min);
    }
}
