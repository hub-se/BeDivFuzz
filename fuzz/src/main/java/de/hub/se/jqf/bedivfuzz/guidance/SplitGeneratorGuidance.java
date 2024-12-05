package de.hub.se.jqf.bedivfuzz.guidance;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import de.hub.se.jqf.bedivfuzz.junit.quickcheck.tracking.SplitTrackingSourceOfRandomness;

import java.util.function.BiConsumer;

public interface SplitGeneratorGuidance {
    void registerChoiceTracer(BiConsumer<SplitTrackingSourceOfRandomness, GenerationStatus> tracer);
}
