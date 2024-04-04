package de.hub.se.jqf.bedivfuzz;

import de.hub.se.jqf.bedivfuzz.guidance.BeDivFuzzGuidance;
import de.hub.se.jqf.bedivfuzz.junit.quickcheck.BeDivFuzzStatement;
import edu.berkeley.cs.jqf.fuzz.Fuzz;
import edu.berkeley.cs.jqf.fuzz.JQF;
import edu.berkeley.cs.jqf.fuzz.difffuzz.DiffFuzz;
import edu.berkeley.cs.jqf.fuzz.difffuzz.DiffFuzzGuidance;
import edu.berkeley.cs.jqf.fuzz.guidance.Guidance;
import edu.berkeley.cs.jqf.fuzz.guidance.GuidanceException;
import edu.berkeley.cs.jqf.fuzz.junit.GuidedFuzzing;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;


public class BeDivFuzz extends JQF {

    public BeDivFuzz(Class<?> clazz) throws InitializationError {
        super(clazz);
    }

    @Override
    public Statement methodBlock(FrameworkMethod method) {
        Guidance guidance = GuidedFuzzing.getCurrentGuidance();
        if (!(guidance instanceof BeDivFuzzGuidance)) {
            return super.methodBlock(method);
        }

        if (method.getAnnotation(Fuzz.class) != null) {
                return new BeDivFuzzStatement(method, getTestClass(), generatorRepository, guidance);
        } else if (method.getAnnotation(DiffFuzz.class) != null) {
            // Get currently set fuzzing guidance
            DiffFuzzGuidance diffGuidance;

            if (!(guidance instanceof DiffFuzzGuidance)) {
                throw new IllegalStateException("@DiffFuzz methods cannot be fuzzed with a " +
                        guidance.getClass().getName());
            }
            diffGuidance = (DiffFuzzGuidance) guidance;

            if (method.getAnnotation(DiffFuzz.class).cmp().isEmpty() == false) {
                diffGuidance.setCompare(cmpNames.get(method.getAnnotation(DiffFuzz.class).cmp()).getMethod());
            }

            return new BeDivFuzzStatement(method, getTestClass(), generatorRepository, diffGuidance);
        } else {
            throw new GuidanceException("Test method is not annotated with @Fuzz.");
        }
    }
}
