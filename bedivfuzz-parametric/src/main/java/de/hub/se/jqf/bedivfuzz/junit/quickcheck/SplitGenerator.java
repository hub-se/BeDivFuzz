package de.hub.se.jqf.bedivfuzz.junit.quickcheck;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

public abstract class SplitGenerator<T> extends Generator<T> {

    protected SplitGenerator(Class<T> type) {
        super(type);
    }

    @Override
    public T generate(SourceOfRandomness random, GenerationStatus status) {
        throw new UnsupportedOperationException("SplitGenerators should generate values using SplitSourceOfRandomness. " +
                " Is the test class annotated with @RunWith(BeDivFuzz.class)?"
        );
    }

    public abstract T generate(SplitRandom random, GenerationStatus status);

}
