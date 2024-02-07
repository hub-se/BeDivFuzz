package de.hub.se.jqf.bedivfuzz.junit.quickcheck;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.internal.ParameterTypeContext;
import com.pholser.junit.quickcheck.internal.generator.GeneratorRepository;
import de.hub.se.jqf.bedivfuzz.guidance.BeDivGuidance;
import de.hub.se.jqf.bedivfuzz.guidance.SplitParameterStream;
import de.hub.se.jqf.bedivfuzz.guidance.TrackingBeDivFuzzGuidance;
import de.hub.se.jqf.bedivfuzz.junit.quickcheck.tracking.SplitTrackingSourceOfRandomness;
import edu.berkeley.cs.jqf.fuzz.guidance.*;
import edu.berkeley.cs.jqf.fuzz.junit.quickcheck.FuzzStatement;
import edu.berkeley.cs.jqf.fuzz.junit.quickcheck.NonTrackingGenerationStatus;
import edu.berkeley.cs.jqf.instrument.InstrumentationException;
import org.junit.AssumptionViolatedException;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.MultipleFailureException;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;
import ru.vyarus.java.generics.resolver.GenericsResolver;
import ru.vyarus.java.generics.resolver.context.MethodGenericsContext;

import java.io.EOFException;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static edu.berkeley.cs.jqf.fuzz.guidance.Result.*;

/**
 *
 * Custom JUnit {@link Statement} adapted from {@link FuzzStatement} to perform
 * guided fuzz testing with BeDivFuzz.
 */

public class BeDivFuzzStatement extends Statement {
    private final FrameworkMethod method;
    private final TestClass testClass;
    private final MethodGenericsContext generics;
    private final List<Class<?>> expectedExceptions;
    private final List<Throwable> failures = new ArrayList<>();
    private final Guidance guidance;
    private final boolean skipExceptionSwallow;
    private final List<Generator<?>> generators;

    public BeDivFuzzStatement(FrameworkMethod method, TestClass testClass,
                              GeneratorRepository generatorRepository, Guidance fuzzGuidance) {
        this.method = method;
        this.testClass = testClass;
        this.generics = GenericsResolver.resolve(testClass.getJavaClass())
                .method(method.getMethod());
        this.expectedExceptions = Arrays.asList(method.getMethod().getExceptionTypes());
        this.guidance = fuzzGuidance;
        this.skipExceptionSwallow = Boolean.getBoolean("jqf.failOnDeclaredExceptions");
        this.generators = Arrays.stream(method.getMethod().getParameters())
                .map(this::createParameterTypeContext)
                .map(generatorRepository::produceGenerator)
                .collect(Collectors.toList());

        if (!generators.stream().allMatch(SplitGenerator.class::isInstance)) {
            throw new GuidanceException("Parameter generators must extend the SplitGenerator<T> class.");
        }

    }

    /**
     * Run the test.
     *
     * @throws Throwable if the test fails
     */
    @Override
    public void evaluate() throws Throwable {
        // Keep fuzzing until no more input or I/O error with guidance
        try {

            // Keep fuzzing as long as guidance wants to
            while (guidance.hasInput()) {
                Result result = INVALID;
                Throwable error = null;

                // Initialize guided fuzzing using a file-backed random number source
                try {
                    Object[] args;
                    try {
                        // Generate input values
                        SplitParameterStream input = ((BeDivGuidance) guidance).getSplitInput();
                        StreamBackedRandom structuralDelegate = new StreamBackedRandom(input.createStructuralParameterStream(), Long.BYTES);
                        StreamBackedRandom valueDelegate = new StreamBackedRandom(input.createValueParameterStream(), Long.BYTES);
                        SplitSourceOfRandomness random = new SplitSourceOfRandomness(structuralDelegate, valueDelegate);
                        GenerationStatus genStatus = new NonTrackingGenerationStatus(random.getStructureDelegate());
                        args = generators.stream()
                                .map(g -> ((SplitGenerator<?>) g).generate(random, genStatus))
                                .toArray();

                        // Let guidance observe the generated input args
                        guidance.observeGeneratedArgs(args);
                    } catch (IllegalStateException e) {
                        if (e.getCause() instanceof EOFException) {
                            // This happens when we reach EOF before reading all the random values.
                            // The only thing we can do is try again
                            continue;
                        } else {
                            throw e;
                        }
                    } catch (AssumptionViolatedException | TimeoutException e) {
                        // Propagate early termination of tests from generator
                        continue;
                    } catch (GuidanceException e) {
                        // Throw the guidance exception outside to stop fuzzing
                        throw e;
                    } catch (Throwable e) {
                        // Throw the guidance exception outside to stop fuzzing
                        throw new GuidanceException(e);
                    }

                    // Attempt to run the trial
                    guidance.run(testClass, method, args);

                    // If we reached here, then the trial must be a success
                    result = SUCCESS;
                } catch(InstrumentationException e) {
                    // Throw a guidance exception outside to stop fuzzing
                    throw new GuidanceException(e);
                } catch (GuidanceException e) {
                    // Throw the guidance exception outside to stop fuzzing
                    throw e;
                } catch (AssumptionViolatedException e) {
                    result = INVALID;
                    error = e;
                } catch (TimeoutException e) {
                    result = TIMEOUT;
                    error = e;
                } catch (Throwable e) {

                    // Check if this exception was expected
                    if (isExceptionExpected(e.getClass())) {
                        result = SUCCESS; // Swallow the error
                    } else {
                        result = FAILURE;
                        error = e;
                        failures.add(e);
                    }
                }

                // Inform guidance about the outcome of this trial
                try {
                    guidance.handleResult(result, error);
                } catch (GuidanceException e) {
                    throw e; // Propagate
                } catch (Throwable e) {
                    // Anything else thrown from handleResult is an internal error, so wrap
                    throw new GuidanceException(e);
                }


            }
        } catch (GuidanceException e) {
            System.err.println("Fuzzing stopped due to guidance exception: " + e.getMessage());
            throw e;
        }

        if (failures.size() > 0) {
            if (failures.size() == 1) {
                throw failures.get(0);
            } else {
                // Not sure if we should report each failing run,
                // as there may be duplicates
                throw new MultipleFailureException(failures);
            }
        }
    }

    /**
     * Returns whether an exception is expected to be thrown by a trial method
     *
     * @param e the class of an exception that is thrown
     * @return <code>true</code> if e is a subclass of any exception specified
     * in the <code>throws</code> clause of the trial method.
     */
    private boolean isExceptionExpected(Class<? extends Throwable> e) {
        if (skipExceptionSwallow) {
            return false;
        }
        for (Class<?> expectedException : expectedExceptions) {
            if (expectedException.isAssignableFrom(e)) {
                return true;
            }
        }
        return false;
    }

    private ParameterTypeContext createParameterTypeContext(Parameter parameter) {
        return ParameterTypeContext.forParameter(parameter, generics).annotate(parameter);
    }

}