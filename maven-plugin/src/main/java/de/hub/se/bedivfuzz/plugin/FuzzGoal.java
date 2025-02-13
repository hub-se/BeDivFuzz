/*
 * Copyright (c) 2017-2018 The Regents of the University of California
 * Copyright (c) 2020-2021 Rohan Padhye
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package de.hub.se.bedivfuzz.plugin;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Random;

import de.hub.se.bedivfuzz.guidance.BeDivFuzzGuidance;
import edu.berkeley.cs.jqf.fuzz.ei.ZestGuidance;
import edu.berkeley.cs.jqf.fuzz.guidance.GuidanceException;
import edu.berkeley.cs.jqf.fuzz.junit.GuidedFuzzing;
import edu.berkeley.cs.jqf.instrument.InstrumentingClassLoader;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.junit.runner.Result;


/**
 * Maven plugin for feedback-directed fuzzing using BeDivFuzz. Adapted from JQF implementation.
 *
 * <p>Performs code-coverage-guided generator-based fuzz testing
 * using a provided entry point.</p>
 *
 * @author Rohan Padhye
 * @author Hoang Lam Nguyen
 */
@Mojo(name="fuzz",
        requiresDependencyResolution= ResolutionScope.TEST,
        defaultPhase=LifecyclePhase.VERIFY)
public class FuzzGoal extends AbstractMojo {

    @Parameter(defaultValue="${project}", required=true, readonly=true)
    MavenProject project;

    @Parameter(property="target", defaultValue="${project.build.directory}", readonly=true)
    private File target;

    /**
     * The fully-qualified name of the test class containing methods
     * to fuzz.
     *
     * <p>This class will be loaded using the Maven project's test
     * classpath. It must be annotated with {@code @RunWith(JQF.class)}</p>
     */
    @Parameter(property="class", required=true)
    private String testClassName;

    /**
     * The name of the method to fuzz.
     *
     * <p>This method must be annotated with {@code @Fuzz}, and take
     * one or more arguments (with optional junit-quickcheck
     * annotations) whose values will be fuzzed by JQF.</p>
     *
     * <p>If more than one method of this name exists in the
     * test class or if the method is not declared
     * {@code public void}, then the fuzzer will not launch.</p>
     */
    @Parameter(property="method", required=true)
    private String testMethod;

    /**
     * Whether to use the fast instrumentation method.
     *
     * <p>If not provided, defaults to {@code false}.</p>
     */
    @Parameter(property="fastInstrumentation")
    private boolean fastInstrumentation;

    /**
     * Comma-separated list of FQN prefixes to exclude from
     * coverage instrumentation.
     *
     * <p>Example: <code>org/mozilla/javascript/gen,org/slf4j/logger</code>,
     * will exclude classes auto-generated by Mozilla Rhino's CodeGen and
     * logging classes.</p>
     */
    @Parameter(property="excludes")
    private String excludes;

    /**
     * Comma-separated list of FQN prefixes to forcibly include,
     * even if they match an exclude.
     *
     * <p>Typically, these will be a longer prefix than a prefix
     * in the excludes clauses.</p>
     */
    @Parameter(property="includes")
    private String includes;

    /**
     * The duration of time for which to run fuzzing.
     *
     * <p>
     * If neither this property nor {@code trials} are provided, the fuzzing
     * session is run for an unlimited time until the process is terminated by the
     * user (e.g. via kill or CTRL+C).
     * </p>
     *
     * <p>
     * Valid time durations are non-empty strings in the format [Nh][Nm][Ns], such
     * as "60s" or "2h30m".
     * </p>
     */
    @Parameter(property="time")
    private String time;

    /**
     * The number of trials for which to run fuzzing.
     *
     * <p>
     * If neither this property nor {@code time} are provided, the fuzzing
     * session is run for an unlimited time until the process is terminated by the
     * user (e.g. via kill or CTRL+C).
     * </p>
     */ 
    @Parameter(property="trials")
    private Long trials;

    /**
     * A number to seed the source of randomness in the fuzzing algorithm.
     *
     * <p>
     * Setting this to any value will make the result of running the same fuzzer
     * with on the same input the same. This is useful for testing the fuzzer, but
     * shouldn't be used on code attempting to find real bugs. By default, the
     * seed is chosen randomly based on system state.
     * </p>
     */
    @Parameter(property="randomSeed")
    private Long randomSeed;

    /**
     * The name of the input directory containing seed files.
     *
     * <p>If not provided, then fuzzing starts with randomly generated
     * initial inputs.</p>
     */
    @Parameter(property="in")
    private String inputDirectory;

    /**
     * The name of the output directory where fuzzing results will
     * be stored.
     *
     * <p>The directory will be created inside the standard Maven
     * project build directory.</p>
     *
     * <p>If not provided, defaults to
     * <em>jqf-fuzz/${testClassName}/${$testMethod}</em>.</p>
     */
    @Parameter(property="out")
    private String outputDirectory;

    /**
     * Weather to use libFuzzer like output instead of AFL like stats
     * screen
     *
     * <p>If this property is set to <code>true</>, then output will look like libFuzzer output
     * https://llvm.org/docs/LibFuzzer.html#output
     * .</p>
     */
    @Parameter(property="libFuzzerCompatOutput")
    private String libFuzzerCompatOutput;

    /**
     * Whether to avoid printing fuzzing statistics progress in the console.
     *
     * <p>If not provided, defaults to {@code false}.</p>
     */
    @Parameter(property="quiet")
    private boolean quiet;
  
    /**
     * Whether to stop fuzzing once a crash is found.
     *
     * <p>If this property is set to <code>true</code>, then the fuzzing
     * will exit on first crash. Useful for continuous fuzzing when you dont wont to consume resource
     * once a crash is found. Also fuzzing will be more effective once the crash is fixed.</p>
     */
    @Parameter(property="exitOnCrash")
    private String exitOnCrash;

    /**
     * The timeout for each individual trial, in milliseconds.
     *
     * <p>If not provided, defaults to 0 (unlimited).</p>
     */
    @Parameter(property="runTimeout")
    private int runTimeout;

    /**
     * The havoc mutation rate (between 0 and 1).
     */
    @Parameter(property="havocRate")
    private double havocRate;

    /**
     * The exploration-exploitation trade-off (between 0 and 1).
     */
    @Parameter(property="epsilon")
    private double epsilon;

    /**
     * Whether to enable input structure feedback.
     *
     * <p>If not provided, defaults to {@code false}.</p>
     */
    @Parameter(property="inputStructureFeedback")
    private boolean structuralFeedback;


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        ClassLoader loader;
        ZestGuidance guidance;
        Log log = getLog();
        PrintStream out = log.isDebugEnabled() ? System.out : null;
        Result result;

        // Configure instrumentation
        if(fastInstrumentation) {
            System.setProperty("useFastNonCollidingCoverageInstrumentation", "true");
        }

        // Configure classes to instrument
        if (excludes != null) {
            System.setProperty("janala.excludes", excludes);
        }
        if (includes != null) {
            System.setProperty("janala.includes", includes);
        }

        // Configure Guidance
        if (libFuzzerCompatOutput != null) {
            System.setProperty("jqf.ei.LIBFUZZER_COMPAT_OUTPUT", libFuzzerCompatOutput);
        }
        if (quiet) {
            System.setProperty("jqf.ei.QUIET_MODE", "true");
        }
        if (exitOnCrash != null) {
            System.setProperty("jqf.ei.EXIT_ON_CRASH", exitOnCrash);
        }
        if (runTimeout > 0) {
            System.setProperty("jqf.ei.TIMEOUT", String.valueOf(runTimeout));
        }
        if (havocRate > 0) {
            System.setProperty("jqf.guidance.bedivfuzz.havoc_rate", String.valueOf(havocRate));
        }
        if (epsilon > 0) {
            System.setProperty("jqf.guidance.bedivfuzz.epsilon", String.valueOf(epsilon));
        }
        if (structuralFeedback) {
            System.setProperty("jqf.guidance.bedivfuzz.STRUCTURAL_FEEDBACK", "true");
        }

        Duration duration = null;
        if (time != null && !time.isEmpty()) {
            try {
                duration = Duration.parse("PT"+time);
            } catch (DateTimeParseException e) {
                throw new MojoExecutionException("Invalid time duration: " + time);
            }
        }

        if (outputDirectory == null || outputDirectory.isEmpty()) {
            outputDirectory = "fuzz-results" + File.separator + testClassName + File.separator + testMethod;
        }

        try {
            List<String> classpathElements = project.getTestClasspathElements();
            loader = new InstrumentingClassLoader(
                    classpathElements.toArray(new String[0]),
                    getClass().getClassLoader());
        } catch (DependencyResolutionRequiredException|MalformedURLException e) {
            throw new MojoExecutionException("Could not get project classpath", e);
        }

        File resultsDir = new File(target, outputDirectory);
        String targetName = testClassName + "#" + testMethod;
        File seedsDir = inputDirectory == null ? null : new File(inputDirectory);
        Random rnd = randomSeed != null ? new Random(randomSeed) : new Random();

        try {
            guidance = new BeDivFuzzGuidance(targetName, duration, trials, resultsDir, seedsDir, rnd);
        } catch (FileNotFoundException e) {
            throw new MojoExecutionException("File not found", e);
        } catch (IOException e) {
            throw new MojoExecutionException("I/O error", e);
        }

        try {
            result = GuidedFuzzing.run(testClassName, testMethod, loader, guidance, out);
        } catch (ClassNotFoundException e) {
            throw new MojoExecutionException("Could not load test class", e);
        } catch (IllegalArgumentException e) {
            throw new MojoExecutionException("Bad request", e);
        } catch (RuntimeException e) {
            throw new MojoExecutionException("Internal error", e);
        }

        if (!result.wasSuccessful()) {
            Throwable e = result.getFailures().get(0).getException();
            if (result.getFailureCount() == 1) {
                if (e instanceof GuidanceException) {
                    throw new MojoExecutionException("Internal error", e);
                }
            }
            throw new MojoFailureException(String.format("Fuzzing resulted in the test failing on " +
                    "%d input(s). Possible bugs found. " +
                    "Use bin/jqf-repro to reproduce failing test cases from %s/failures. ",
                    result.getFailureCount(), resultsDir) +
                    "Sample exception included with this message.", e);
        }
    }
}
