package de.hub.se.jqf.examples.common;

import com.pholser.junit.quickcheck.generator.java.lang.AbstractStringGenerator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import de.hub.se.jqf.fuzz.junit.quickcheck.SplitSourceOfRandomness;

/**
 * @author Hoang Lam Nguyen
 */
public class SplitAsciiStringGenerator extends AbstractStringGenerator {

    @Override
    protected int nextCodePoint(SourceOfRandomness sourceOfRandomness) {
        return ((SplitSourceOfRandomness) sourceOfRandomness).nextByte((byte) 0, (byte) 127, false);
    }

    @Override
    protected boolean codePointInRange(int i) {
        return i >= 1 && i <= 127;
    }
}
