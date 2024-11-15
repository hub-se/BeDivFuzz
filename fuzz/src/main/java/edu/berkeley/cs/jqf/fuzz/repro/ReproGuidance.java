/*
 * Copyright (c) 2017-2018 The Regents of the University of California
 * Copyright (c) 2021 Rohan Padhye
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
package edu.berkeley.cs.jqf.fuzz.repro;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import edu.berkeley.cs.jqf.fuzz.guidance.Guidance;
import edu.berkeley.cs.jqf.fuzz.guidance.GuidanceException;
import edu.berkeley.cs.jqf.fuzz.guidance.Result;
import edu.berkeley.cs.jqf.fuzz.util.Coverage;
import edu.berkeley.cs.jqf.fuzz.util.CoverageFactory;
import edu.berkeley.cs.jqf.fuzz.util.ICoverage;
import edu.berkeley.cs.jqf.fuzz.util.IOUtils;
import edu.berkeley.cs.jqf.instrument.tracing.FastCoverageSnoop;
import edu.berkeley.cs.jqf.instrument.tracing.FastSemanticCoverageSnoop;
import edu.berkeley.cs.jqf.instrument.tracing.events.BranchEvent;
import edu.berkeley.cs.jqf.instrument.tracing.events.CallEvent;
import edu.berkeley.cs.jqf.instrument.tracing.events.TraceEvent;
import janala.instrument.FastCoverageListener;
import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.tools.ExecFileLoader;
import org.jacoco.report.IReportVisitor;
import org.jacoco.report.csv.CSVFormatter;

/**
 * A front-end that provides a specified set of inputs for test
 * case reproduction,
 *
 * This class enables reproduction of a test case with an input file
 * generated by a guided fuzzing front-end such as AFL.
 *
 * @author Rohan Padhye
 */
public class ReproGuidance implements Guidance {
    protected final File[] inputFiles;
    private final File traceDir;
    protected int nextFileIdx = 0;
    private List<PrintStream> traceStreams = new ArrayList<>();
    private InputStream inputStream;
    private Coverage coverage = new Coverage();

    // Fast coverage
    private boolean TRACK_SEMANTIC_COVERAGE = Boolean.getBoolean("jqf.guidance.TRACK_SEMANTIC_COVERAGE");
    private ICoverage totalCoverage = CoverageFactory.newInstance();
    private ICoverage semanticCoverage = CoverageFactory.newInstance();

    protected Set<String> branchesCoveredInCurrentRun;
    protected Set<String> allBranchesCovered;
    private boolean ignoreInvalidCoverage;
    private boolean printArgs;
    private String dumpArgsDir;

    HashMap<Integer, String> branchDescCache = new HashMap<>();

    private boolean stopOnFailure = false;
    private boolean observedFailure = false;

    /**
     * Constructs an instance of ReproGuidance with a list of
     * input files to replay and a directory where the trace
     * events may be logged.
     *
     * @param inputFiles a list of input files
     * @param traceDir an optional directory, which if non-null will
     *                 be the destination for log files containing event
     *                 traces
     */
    public ReproGuidance(File[] inputFiles, File traceDir) throws IOException {
        this.inputFiles = inputFiles;
        this.traceDir = traceDir;
        if (Boolean.getBoolean("jqf.repro.logUniqueBranches")) {
            allBranchesCovered = new HashSet<>();
            branchesCoveredInCurrentRun = new HashSet<>();
            ignoreInvalidCoverage = Boolean.getBoolean("jqf.repro.ignoreInvalidCoverage");
        }
        printArgs = Boolean.getBoolean("jqf.repro.printArgs");
        dumpArgsDir = System.getProperty("jqf.repro.dumpArgsDir");
        if (dumpArgsDir != null) {
            IOUtils.createDirectory(new File(dumpArgsDir));
        }

        if(TRACK_SEMANTIC_COVERAGE) {
            System.out.println("Tracking semantic coverage: " + System.getProperty("janala.semanticAnalysisClasses"));
            FastCoverageSnoop.setFastCoverageListener((FastCoverageListener) this.totalCoverage);
            FastSemanticCoverageSnoop.setCoverageListeners((FastCoverageListener) this.totalCoverage, (FastCoverageListener) this.semanticCoverage);
        }

    }

    /**
     * Constructs an instance of ReproGuidance with a single
     * input file or with a directory of input files
     * to replay, and a directory where the trace
     * events may be logged.
     *
     * @param inputFile an input file or directory of input files
     * @param traceDir an optional directory, which if non-null will
     *                 be the destination for log files containing event
     *                 traces
     * @throws FileNotFoundException if `inputFile` is not a valid file or directory
     */
    public ReproGuidance(File inputFile, File traceDir) throws IOException {
        this(IOUtils.resolveInputFileOrDirectory(inputFile), traceDir);
    }

    /**
     * Configure whether the repro execution should stop as soon as
     * the first failure is encountered.
     *
     * @param value whether to stop the repro on failure
     */
    public void setStopOnFailure(boolean value) {
        this.stopOnFailure = value;
    }

    /**
     * Returns an input stream corresponding to the next input file.
     *
     * @return an input stream corresponding to the next input file
     */
    @Override
    public InputStream getInput() {
        try {
            File inputFile = inputFiles[nextFileIdx];
            this.inputStream = new BufferedInputStream(new FileInputStream(inputFile));

            if (allBranchesCovered != null) {
                branchesCoveredInCurrentRun.clear();
            }

            return this.inputStream;
        } catch (IOException e) {
            throw new GuidanceException(e);
        }
    }

    /**
     * Returns <code>true</code> if there are more input files to replay.
     * @return <code>true</code> if there are more input files to replay
     */
    @Override
    public boolean hasInput() {
        return nextFileIdx < inputFiles.length && !(stopOnFailure && observedFailure);
    }

    @Override
    public void observeGeneratedArgs(Object[] args) {
        if (printArgs) {
            String inputFileName = getCurrentInputFile().getName();
            for (int i = 0; i < args.length; i++) {
                System.out.printf("%s[%d]: %s\n", inputFileName, i, String.valueOf(args[i]));
            }
        }

        // Save generated args to file (e.g. id_000000.1 for second arg of id_000000)
        if (dumpArgsDir != null) {
            for (int i = 0; i < args.length; i++) {
                String dumpFileName = String.format("%s.%d",
                        getCurrentInputFile().getName(), i);
                File dumpFile = new File(dumpArgsDir, dumpFileName);
                Object arg = args[i];
                GuidanceException.wrap(() -> writeObjectToFile(dumpFile, arg));
            }
        }
    }

    @Override
    public String observeGuidance() {
        return "Repro";
    }

    /**
     * Writes an object to a file
     *
     * @param file  the file to write to
     * @param obj   the object to serialize
     * @throws IOException if the object cannot be written to file
     */
    private void writeObjectToFile(File file, Object obj) throws IOException {
        try (PrintWriter out = new PrintWriter(file)) {
            out.print(obj);
        }
    }


    /**
     * Returns the input file which is currently being repro'd.
     * @return the current input file
     */
    private File getCurrentInputFile() {
        return inputFiles[nextFileIdx];
    }

    /**
     * Logs the end of run in the log files, if any.
     *
     * @param result   the result of the fuzzing trial
     * @param error    the error thrown during the trial, or <code>null</code>
     */
    @Override
    public void handleResult(Result result, Throwable error) {
        // Close the open input file
        try {
            if (inputStream != null) {
                inputStream.close();
            }
        } catch (IOException e) {
            throw new GuidanceException(e);
        }

        // Print result
        File inputFile = getCurrentInputFile();
        if (result == Result.FAILURE) {
            observedFailure = true;
            System.out.printf("%s ::= %s (%s)%n", inputFile.getName(), result, error.getClass().getName());
        } else {
            System.out.printf("%s ::= %s%n", inputFile.getName(), result);
        }

        if (TRACK_SEMANTIC_COVERAGE) {
            System.out.printf("coverage  ::= total %d semantic %d%n", totalCoverage.getNonZeroCount(), semanticCoverage.getNonZeroCount());
        }

        // Possibly accumulate coverage
        if (allBranchesCovered != null && (!ignoreInvalidCoverage || result == Result.SUCCESS)) {
            assert branchesCoveredInCurrentRun != null;
            allBranchesCovered.addAll(branchesCoveredInCurrentRun);
        }

        // Maybe add to results csv
        if (traceDir != null) {
            File resultsCsv = new File(traceDir, "results.csv");
            boolean append = nextFileIdx > 0; // append for all but the first input
            try (PrintStream out = new PrintStream(new FileOutputStream(resultsCsv, append))) {
                String inputName = getCurrentInputFile().toString();
                String exception = result == Result.FAILURE ? error.getClass().getName() : "";
                if (TRACK_SEMANTIC_COVERAGE) {
                    int totalCov = totalCoverage.getNonZeroCount();
                    int semanticCov = semanticCoverage.getNonZeroCount();
                    out.printf("%s,%s,%s,%d,%d%n", inputName, result, exception, totalCov, semanticCov);
                } else {
                    out.printf("%s,%s,%s%n", inputName, result, exception);
                }
            } catch (IOException e) {
                throw new GuidanceException(e);
            }
        }

        // Maybe checkpoint JaCoCo coverage
        String jacocoAccumulateJar = System.getProperty("jqf.repro.jacocoAccumulateJar");
        if (jacocoAccumulateJar != null) {
            String dir = System.getProperty("jqf.repro.jacocoAccumulateDir", ".");
            jacocoCheckpoint(new File(jacocoAccumulateJar), new File(dir));

        }

        // Increment file
        nextFileIdx++;


    }

    /**
     * Returns a callback that can log trace events or code coverage info.
     *
     * <p>If the system property <code>jqf.repro.logUniqueBranches</code> was
     * set to <code>true</code>, then the callback collects coverage info into
     * the set {@link #branchesCoveredInCurrentRun}, which can be accessed using
     * {@link #getBranchesCovered()}.</p>
     *
     * <p>Otherwise, if the <code>traceDir</code> was non-null during the construction of
     * this Guidance instance, then one log file per thread of
     * execution is created in this directory. The callbacks generated
     * by this method write trace event descriptions in sequence to
     * their own thread's log files.</p>
     *
     * <p>If neither of the above are true, the returned callback simply updates
     * a total coverage map (see {@link #getCoverage()}.</p>
     *
     * @param thread the thread whose events to handle
     * @return a callback to log code coverage or execution traces
     */
    @Override
    public Consumer<TraceEvent> generateCallBack(Thread thread) {
        if (branchesCoveredInCurrentRun != null) {
            return (e) -> {
                coverage.handleEvent(e);
                if (e instanceof BranchEvent) {
                    BranchEvent b = (BranchEvent) e;
                    int hash = b.getIid() * 31 + b.getArm();
                    String str = branchDescCache.get(hash);
                    if (str == null) {
                        str = String.format("(%09d) %s#%s():%d [%d]", b.getIid(), b.getContainingClass(), b.getContainingMethodName(),
                                b.getLineNumber(), b.getArm());
                        branchDescCache.put(hash, str);
                    }
                    branchesCoveredInCurrentRun.add(str);
                } else if (e instanceof CallEvent) {
                    CallEvent c = (CallEvent) e;
                    String str = branchDescCache.get(c.getIid());
                    if (str == null) {
                        str = String.format("(%09d) %s#%s():%d --> %s", c.getIid(), c.getContainingClass(), c.getContainingMethodName(),
                                c.getLineNumber(), c.getInvokedMethodName());
                        branchDescCache.put(c.getIid(), str);
                    }
                    branchesCoveredInCurrentRun.add(str);
                }
            };
        } else if (traceDir != null) {
            File traceFile = new File(traceDir, thread.getName() + ".log");
            try {
                PrintStream out = new PrintStream(traceFile);
                traceStreams.add(out);

                // Return an event logging callback
                return (e) -> {
                    coverage.handleEvent(e);
                    out.println(e);
                };
            } catch (FileNotFoundException e) {
                // Note the exception, but ignore trace events
                System.err.println("Could not open trace file: " + traceFile.getAbsolutePath());
            }
        }

        // If none of the above work, just update coverage
        return coverage::handleEvent;

    }

    /**
     * Returns a reference to the coverage statistics.
     * @return a reference to the coverage statistics
     */
    public Coverage getCoverage() {
        return coverage;
    }


    /**
     * Retyrns the set of branches covered by this repro.
     *
     * <p>This set will only be non-empty if the system
     * property <code>jqf.repro.logUniqueBranches</code> was
     * set to <code>true</code> before the guidance instance
     * was constructed.</p>
     *
     * <p>The format of each element in this set is a
     * custom format that strives to be both human and
     * machine readable.</p>
     *
     * <p>A branch is only logged for inputs that execute
     * successfully. In particular, branches are not recorded
     * for failing runs or for runs that violate assumptions.</p>
     *
     * @return the set of branches covered by this repro
     */
    public Set<String> getBranchesCovered() {
        return allBranchesCovered;
    }


    public void jacocoCheckpoint(File classFile, File csvDir) {
        int idx = nextFileIdx;
        csvDir.mkdirs();
        try {
            // Get exec data by dynamically calling RT.getAgent().getExecutionData()
            Class RT = Class.forName("org.jacoco.agent.rt.RT");
            Method getAgent = RT.getMethod("getAgent");
            Object agent = getAgent.invoke(null);
            Method dump = agent.getClass().getMethod("getExecutionData", boolean.class);
            byte[] execData = (byte[]) dump.invoke(agent, false);

            // Analyze exec data
            ExecFileLoader loader = new ExecFileLoader();
            loader.load(new ByteArrayInputStream(execData));
            final CoverageBuilder builder = new CoverageBuilder();
            Analyzer analyzer = new Analyzer(loader.getExecutionDataStore(), builder);
            analyzer.analyzeAll(classFile);

            // Generate CSV
            File csv = new File(csvDir, String.format("cov-%05d.csv", idx));
            try (FileOutputStream out = new FileOutputStream(csv)) {
                IReportVisitor coverageVisitor = new CSVFormatter().createVisitor(out);
                coverageVisitor.visitBundle(builder.getBundle("JQF"), null);
                coverageVisitor.visitEnd();
                out.flush();
            }


        } catch (Exception e) {
            System.err.println(e);
        }
    }

}
