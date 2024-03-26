package edu.berkeley.cs.jqf.examples.png;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import edu.berkeley.cs.jqf.examples.common.ByteArrayWrapper;

public class PngGenerator extends Generator<ByteArrayWrapper> {

    PngDataGenerator generator;

    public PngGenerator() {
        super(ByteArrayWrapper.class);
        generator = new PngDataGenerator();
    }

    @Override
    public ByteArrayWrapper generate(SourceOfRandomness random, GenerationStatus __ignore__) {
        return new ByteArrayWrapper(generator.generate(random));
    }

}
