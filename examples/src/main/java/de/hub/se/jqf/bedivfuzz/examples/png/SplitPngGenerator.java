package de.hub.se.jqf.bedivfuzz.examples.png;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import de.hub.se.jqf.bedivfuzz.junit.quickcheck.SplitGenerator;
import de.hub.se.jqf.bedivfuzz.junit.quickcheck.SplitRandom;
import edu.berkeley.cs.jqf.examples.common.ByteArrayWrapper;

public class SplitPngGenerator extends SplitGenerator<ByteArrayWrapper> {

    SplitPngDataGenerator generator;

    public SplitPngGenerator() {
        super(ByteArrayWrapper.class);
        generator = new SplitPngDataGenerator();
    }

    @Override
    public ByteArrayWrapper generate(SplitRandom random, GenerationStatus __ignore__) {
        return new ByteArrayWrapper(generator.generate(random));
    }

}
