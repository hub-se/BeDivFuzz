package de.hub.se.bedivfuzz.guidance;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import de.hub.se.bedivfuzz.junit.quickcheck.tracing.SplitTracingSourceOfRandomness;

import java.util.function.BiConsumer;


public interface SplitGeneratorGuidance {
    void registerChoiceTracer(BiConsumer<SplitTracingSourceOfRandomness, GenerationStatus> tracer);
}
