package de.hub.se.jqf.bedivfuzz;

import de.hub.se.jqf.bedivfuzz.guidance.BeDivGuidance;
import de.hub.se.jqf.bedivfuzz.junit.quickcheck.BeDivFuzzStatement;
import edu.berkeley.cs.jqf.fuzz.Fuzz;
import edu.berkeley.cs.jqf.fuzz.JQF;
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
        if (method.getAnnotation(Fuzz.class) != null) {
            Guidance guidance = GuidedFuzzing.getCurrentGuidance();
            if (guidance instanceof BeDivGuidance) {
                return new BeDivFuzzStatement(method, getTestClass(), generatorRepository, guidance);
            } else {
                return super.methodBlock(method);
            }
        } else {
            throw new GuidanceException("Test method is not annotated with @Fuzz.");
        }
    }
}
