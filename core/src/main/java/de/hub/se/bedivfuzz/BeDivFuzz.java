package de.hub.se.bedivfuzz;

import de.hub.se.bedivfuzz.junit.quickcheck.BeDivFuzzStatement;
import edu.berkeley.cs.jqf.fuzz.Fuzz;
import edu.berkeley.cs.jqf.fuzz.JQF;
import edu.berkeley.cs.jqf.fuzz.guidance.Guidance;
import edu.berkeley.cs.jqf.fuzz.guidance.GuidanceException;
import edu.berkeley.cs.jqf.fuzz.junit.GuidedFuzzing;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

/**
 * Extends {@link JQF} to perform coverage-guided fuzzing with BeDivFuzz.
 */
public class BeDivFuzz extends JQF {

    public BeDivFuzz(Class<?> clazz) throws InitializationError {
        super(clazz);
    }

    @Override
    public Statement methodBlock(FrameworkMethod method) {
        Guidance guidance = GuidedFuzzing.getCurrentGuidance();

        if (method.getAnnotation(Fuzz.class) != null) {
                return new BeDivFuzzStatement(method, getTestClass(), generatorRepository, guidance);
        } else {
            throw new GuidanceException("Test method is not annotated with @Fuzz.");
        }
    }
}
