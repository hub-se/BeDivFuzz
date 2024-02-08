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

package de.hub.se.jqf.bedivfuzz.guidance;

import java.io.BufferedOutputStream;
import java.io.Console;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import de.hub.se.jqf.bedivfuzz.util.BranchHitCounter;
import de.hub.se.jqf.bedivfuzz.util.BehavioralDiversityMetrics;
import edu.berkeley.cs.jqf.fuzz.guidance.GuidanceException;
import edu.berkeley.cs.jqf.fuzz.guidance.Result;
import edu.berkeley.cs.jqf.fuzz.guidance.TimeoutException;
import edu.berkeley.cs.jqf.fuzz.util.*;
import edu.berkeley.cs.jqf.instrument.tracing.FastCoverageSnoop;
import edu.berkeley.cs.jqf.instrument.tracing.FastSemanticCoverageSnoop;
import edu.berkeley.cs.jqf.instrument.tracing.events.TraceEvent;
import janala.instrument.FastCoverageListener;
import janala.instrument.ProbeCounter;
import org.eclipse.collections.api.iterator.IntIterator;
import org.eclipse.collections.api.list.primitive.IntList;
import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.eclipsecollections.EclipseCollectionsModule;

/**
 * A guidance that performs structure-changing (exploration) and structure-preserving (exploitation) mutations.
 * Adapted from {@linkplain edu.berkeley.cs.jqf.fuzz.ei.ZestGuidance ZestGuidance}.
 * @author Hoang Lam Nguyen
 */
public class BeDivFuzzGuidance implements BeDivGuidance {

    // ------------ BeDivFuzz Variables ------------
    /** Set of input structures explored so far. */
    protected IntHashSet exploredInputStructures = new IntHashSet();

    /** Hash code of last generated structure */
    protected int lastGeneratedStructureHash;

    // ------------ Zest Variables ------------

    /** A pseudo-random number generator for generating fresh values. */
    protected Random random;

    /** The name of the test for display purposes. */
    protected final String testName;

    // ------------ ALGORITHM BOOKKEEPING ------------

    /** The max amount of time to run for, in milli-seconds */
    protected final long maxDurationMillis;

    /** The max number of trials to run */
    protected final long maxTrials;

    /** The number of trials completed. */
    protected long numTrials = 0;

    /** The number of valid inputs. */
    protected long numValid = 0;

    /** The directory where fuzzing results are produced. */
    protected final File outputDirectory;

    /** The directory where interesting inputs are saved. */
    protected File savedCorpusDirectory;

    /** The directory where saved inputs are saved. */
    protected File savedFailuresDirectory;

    /** The directory where all generated inputs are logged in sub-directories (if enabled). */
    protected File allInputsDirectory;

    /** The directory where all branch hit count distributions are logged (if enabled). */
    protected File branchHitCountsDirectory;

    /** Set of saved inputs to fuzz. */
    protected ArrayList<SplitLinearInput> savedInputs = new ArrayList<>();

    /** Queue of seeds to fuzz. */
    protected Deque<SplitLinearInput> seedInputs = new ArrayDeque<>();

    /** Current input that's running -- valid after getInput() and before handleResult(). */
    protected SplitLinearInput currentInput;

    /** Index of currentInput in the savedInputs -- valid after seeds are processed (OK if this is inaccurate). */
    protected int currentParentInputIdx = 0;

    /** Number of mutated inputs generated from currentInput. */
    protected int numChildrenGeneratedForCurrentParentInput = 0;

    /** Number of cycles completed (i.e. how many times we've reset currentParentInputIdx to 0. */
    protected int cyclesCompleted = 0;

    /** Number of favored inputs in the last cycle. */
    protected int numFavoredLastCycle = 0;

    /** Blind fuzzing -- if true then the queue is always empty. */
    protected boolean blind;

    /** Validity fuzzing -- if true then save valid inputs that increase valid coverage */
    protected boolean validityFuzzing;

    /** Number of saved inputs.
     *
     * This is usually the same as savedInputs.size(),
     * but we do not really save inputs in TOTALLY_RANDOM mode.
     */
    protected int numSavedInputs = 0;

    /** Coverage statistics for a single run. */
    protected ICoverage runCoverage = CoverageFactory.newInstance();

    /** Cumulative coverage statistics. */
    protected ICoverage totalCoverage = CoverageFactory.newInstance();

    /** Cumulative coverage for valid inputs. */
    protected ICoverage validCoverage = CoverageFactory.newInstance();

    /** Semantic coverage for a single run. */
    protected ICoverage semanticRunCoverage = CoverageFactory.newInstance();

    /** Cumulative coverage of semantic analysis classes. */
    protected ICoverage semanticTotalCoverage = CoverageFactory.newInstance();

    /** Set of hashes of all paths generated so far. */
    protected IntHashSet uniquePaths = new IntHashSet();

    /** Set of hashes of all valid paths generated so far. */
    protected IntHashSet uniqueValidPaths = new IntHashSet();

    /** Branch hit-count metrics for all unique paths. */
    protected BranchHitCounter branchHitCounter = new BranchHitCounter();

    /** The maximum number of keys covered by any single input found so far. */
    protected int maxCoverage = 0;

    /** A mapping of coverage keys to inputs that are responsible for them. */
    protected IntObjectHashMap<SplitLinearInput> responsibleInputs = new IntObjectHashMap<>(totalCoverage.size());

    /** The set of unique failures found so far. */
    protected Set<String> uniqueFailures = new HashSet<>();

    /** save crash to specific location (should be used with EXIT_ON_CRASH) **/
    protected final String EXACT_CRASH_PATH = System.getProperty("jqf.ei.EXACT_CRASH_PATH");

    // ---------- LOGGING / STATS OUTPUT ------------

    /** Whether to print log statements to stderr (debug option; manually edit). */
    protected final boolean verbose = true;

    /** A system console, which is non-null only if STDOUT is a console. */
    protected final Console console = System.console();

    /** Time since this guidance instance was created. */
    protected final Date startTime = new Date();

    /** Time at last stats refresh. */
    protected Date lastRefreshTime = startTime;

    /** Total execs at last stats refresh. */
    protected long lastNumTrials = 0;

    /** Minimum amount of time (in millis) between two stats refreshes. */
    protected final long STATS_REFRESH_TIME_PERIOD = Integer.getInteger("jqf.guidance.STATS_REFRESH_TIME_PERIOD", 30_000);

    /** The file where log data is written. */
    protected File logFile;

    /** The file where saved plot data is written. */
    protected File statsFile;

    /** The file where failure info is written. */
    protected File failureStatsFile;

    /** The currently executing structural input (for debugging purposes). */
    protected File currentStructuralInputFile;

    /** The currently executing value input (for debugging purposes). */
    protected File currentValueInputFile;

    /** The file containing the coverage information */
    protected File coverageFile;

    /** The object mapper responsible for serializing the branch hit counts. */
    protected ObjectMapper mapper;

    /** Whether to log the branch hit counts over time. **/
    protected final boolean LOG_BRANCH_HIT_COUNTS = Boolean.getBoolean("jqf.guidance.LOG_BRANCH_HIT_COUNTS");

    /** Index of the current branch hit count distribution file. **/
    protected int branchHitCountsFileIdx = 0;

    /** Use libFuzzer like output instead of AFL like stats screen (https://llvm.org/docs/LibFuzzer.html#output) **/
    protected final boolean LIBFUZZER_COMPAT_OUTPUT = Boolean.getBoolean("jqf.ei.LIBFUZZER_COMPAT_OUTPUT");

    /** Whether to hide fuzzing statistics **/
    protected final boolean QUIET_MODE = Boolean.getBoolean("jqf.ei.QUIET_MODE");

    /** Whether to store all generated inputs to disk (can get slowww!) */
    protected final boolean LOG_ALL_INPUTS = Boolean.getBoolean("jqf.ei.LOG_ALL_INPUTS");

    protected final boolean TRACK_SEMANTIC_COVERAGE = Boolean.getBoolean("jqf.guidance.TRACK_SEMANTIC_COVERAGE");

    protected final ProbeCounter probeCounter = ProbeCounter.instance;

    // ------------- TIMEOUT HANDLING ------------

    /** Timeout for an individual run. */
    protected long singleRunTimeoutMillis;

    /** Date when last run was started. */
    protected Date runStart;

    /** Number of conditional jumps since last run was started. */
    protected long branchCount;

    /** Whether to stop/exit once a crash is found. **/
    protected final boolean EXIT_ON_CRASH = Boolean.getBoolean("jqf.ei.EXIT_ON_CRASH");

    // ------------- THREAD HANDLING ------------

    /** The first thread in the application, which usually runs the test method. */
    protected Thread firstThread;

    /** Whether the application has more than one thread running coverage-instrumented code */
    protected boolean multiThreaded = false;

    // ------------- FUZZING HEURISTICS ------------

    /** Whether to save only valid inputs **/
    protected final boolean SAVE_ONLY_VALID = Boolean.getBoolean("jqf.ei.SAVE_ONLY_VALID");

    /** Max input size to generate. */
    protected final int MAX_INPUT_SIZE = Integer.getInteger("jqf.ei.MAX_INPUT_SIZE", 10240);

    /** Whether to generate EOFs when we run out of bytes in the input, instead of randomly generating new bytes. **/
    protected final boolean GENERATE_EOF_WHEN_OUT = Boolean.getBoolean("jqf.ei.GENERATE_EOF_WHEN_OUT");

    /** Baseline number of mutated children to produce from a given parent input. */
    protected final int NUM_CHILDREN_BASELINE = 50;

    /** Multiplication factor for number of children to produce for favored inputs. */
    protected final int NUM_CHILDREN_MULTIPLIER_FAVORED = 20;

    /** Mean number of mutations to perform in each round. */
    protected final double MEAN_MUTATION_COUNT = 8.0;

    /** Mean number of contiguous bytes to mutate in each mutation. */
    protected final double MEAN_MUTATION_SIZE = 4.0; // Bytes

    /** Whether to save inputs that only add new coverage bits (but no new responsibilities). */
    protected final boolean DISABLE_SAVE_NEW_COUNTS = Boolean.getBoolean("jqf.ei.DISABLE_SAVE_NEW_COUNTS");

    /** Whether to steal responsibility from old inputs (this increases computation cost). */
    protected final boolean STEAL_RESPONSIBILITY = Boolean.getBoolean("jqf.ei.STEAL_RESPONSIBILITY");

    /**
     * Creates a new Zest guidance instance with optional duration,
     * optional trial limit, and possibly deterministic PRNG.
     *
     * @param testName the name of test to display on the status screen
     * @param duration the amount of time to run fuzzing for, where
     *                 {@code null} indicates unlimited time.
     * @param trials   the number of trials for which to run fuzzing, where
     *                 {@code null} indicates unlimited trials.
     * @param outputDirectory the directory where fuzzing results will be written
     * @param sourceOfRandomness      a pseudo-random number generator
     * @throws IOException if the output directory could not be prepared
     */
    public BeDivFuzzGuidance(String testName, Duration duration, Long trials, File outputDirectory, Random sourceOfRandomness) throws IOException {
        this.random = sourceOfRandomness;
        this.testName = testName;
        this.maxDurationMillis = duration != null ? duration.toMillis() : Long.MAX_VALUE;
        this.maxTrials = trials != null ? trials : Long.MAX_VALUE;
        this.outputDirectory = outputDirectory;
        this.blind = Boolean.getBoolean("jqf.ei.TOTALLY_RANDOM");
        this.validityFuzzing = !Boolean.getBoolean("jqf.ei.DISABLE_VALIDITY_FUZZING");
        prepareOutputDirectory();

        if(this.runCoverage instanceof FastCoverageListener){
            FastCoverageSnoop.setFastCoverageListener((FastCoverageListener) this.runCoverage);
        }

        if(TRACK_SEMANTIC_COVERAGE) {
            FastSemanticCoverageSnoop.setCoverageListeners(
                    (FastCoverageListener) this.runCoverage,
                    (FastCoverageListener) this.semanticRunCoverage);
        }

        // Try to parse the single-run timeout
        String timeout = System.getProperty("jqf.ei.TIMEOUT");
        if (timeout != null && !timeout.isEmpty()) {
            try {
                // Interpret the timeout as milliseconds (just like `afl-fuzz -t`)
                this.singleRunTimeoutMillis = Long.parseLong(timeout);
            } catch (NumberFormatException e1) {
                throw new IllegalArgumentException("Invalid timeout duration: " + timeout);
            }
        }
    }

    /**
     * Creates a new Zest guidance instance with seed input files and optional
     * duration, optional trial limit, an possibly deterministic PRNG.
     *
     * @param testName the name of test to display on the status screen
     * @param duration the amount of time to run fuzzing for, where
     *                 {@code null} indicates unlimited time.
     * @param trials   the number of trials for which to run fuzzing, where
     *                 {@code null} indicates unlimited trials.
     * @param outputDirectory the directory where fuzzing results will be written
     * @param seedInputFiles one or more input files to be used as initial inputs
     * @param sourceOfRandomness      a pseudo-random number generator
     * @throws IOException if the output directory could not be prepared
     */
    public BeDivFuzzGuidance(String testName, Duration duration, Long trials, File outputDirectory, File[] seedInputFiles, Random sourceOfRandomness) throws IOException {
        this(testName, duration, trials, outputDirectory, sourceOfRandomness);
        if (seedInputFiles != null) {
            throw new GuidanceException("BeDivFuzz currently does not support seed inputs.");
        }
    }

    /**
     * Creates a new Zest guidance instance with seed input directory and optional
     * duration, optional trial limit, an possibly deterministic PRNG.
     *
     * @param testName the name of test to display on the status screen
     * @param duration the amount of time to run fuzzing for, where
     *                 {@code null} indicates unlimited time.
     * @param trials   the number of trials for which to run fuzzing, where
     *                 {@code null} indicates unlimited trials.
     * @param outputDirectory the directory where fuzzing results will be written
     * @param seedInputDir the directory containing one or more input files to be used as initial inputs
     * @param sourceOfRandomness      a pseudo-random number generator
     * @throws IOException if the output directory could not be prepared
     */
    public BeDivFuzzGuidance(String testName, Duration duration, Long trials, File outputDirectory, File seedInputDir, Random sourceOfRandomness) throws IOException {
        this(testName, duration, trials, outputDirectory, IOUtils.resolveInputFileOrDirectory(seedInputDir), sourceOfRandomness);
    }

    /**
     * Creates a new Zest guidance instance with seed inputs and
     * optional duration.
     *
     * @param testName the name of test to display on the status screen
     * @param duration the amount of time to run fuzzing for, where
     *                 {@code null} indicates unlimited time.
     * @param outputDirectory the directory where fuzzing results will be written
     * @param seedInputDir the directory containing one or more input files to be used as initial inputs
     * @throws IOException if the output directory could not be prepared
     */
    public BeDivFuzzGuidance(String testName, Duration duration, File outputDirectory, File seedInputDir) throws IOException {
        this(testName, duration, null, outputDirectory, seedInputDir, new Random());
    }

    /**
     * Creates a new Zest guidance instance with seed inputs and
     * optional duration.
     *
     * @param testName the name of test to display on the status screen
     * @param duration the amount of time to run fuzzing for, where
     *                 {@code null} indicates unlimited time.
     * @param outputDirectory the directory where fuzzing results will be written
     * @throws IOException if the output directory could not be prepared
     */
    public BeDivFuzzGuidance(String testName, Duration duration, File outputDirectory) throws IOException {
        this(testName, duration, null, outputDirectory, new Random());
    }

    /**
     * Creates a new Zest guidance instance with seed inputs and
     * optional duration.
     *
     * @param testName the name of test to display on the status screen
     * @param duration the amount of time to run fuzzing for, where
     *                 {@code null} indicates unlimited time.
     * @param outputDirectory the directory where fuzzing results will be written
     * @throws IOException if the output directory could not be prepared
     */
    public BeDivFuzzGuidance(String testName, Duration duration, File outputDirectory, File[] seedFiles) throws IOException {
        this(testName, duration, null, outputDirectory, seedFiles, new Random());
    }

    private void prepareOutputDirectory() throws IOException {
        // Create the output directory if it does not exist
        IOUtils.createDirectory(outputDirectory);

        // Name files and directories after AFL
        this.savedCorpusDirectory = IOUtils.createDirectory(outputDirectory, "corpus");
        this.savedFailuresDirectory = IOUtils.createDirectory(outputDirectory, "failures");
        if (LOG_ALL_INPUTS) {
            this.allInputsDirectory = IOUtils.createDirectory(outputDirectory, "all");
            IOUtils.createDirectory(allInputsDirectory, "success");
            IOUtils.createDirectory(allInputsDirectory, "invalid");
            IOUtils.createDirectory(allInputsDirectory, "failure");
        }
        this.statsFile = new File(outputDirectory, "plot_data");
        this.logFile = new File(outputDirectory, "fuzz.log");
        this.failureStatsFile = new File(outputDirectory, "failure_info.csv");
        this.currentStructuralInputFile = new File(outputDirectory, ".cur_structural_parameters");
        this.currentValueInputFile = new File(outputDirectory, ".cur_value_parameters");
        this.coverageFile = new File(outputDirectory, "coverage_hash");

        if (LOG_BRANCH_HIT_COUNTS) {
            this.mapper = new ObjectMapper().registerModule(new EclipseCollectionsModule());
            this.branchHitCountsDirectory = IOUtils.createDirectory(outputDirectory, "hitcounts");
            for (File file : branchHitCountsDirectory.listFiles()) {
                file.delete();
            }
        }

        // Delete everything that we may have created in a previous run.
        // Trying to stay away from recursive delete of parent output directory in case there was a
        // typo and that was not a directory we wanted to nuke.
        // We also do not check if the deletes are actually successful.
        statsFile.delete();
        failureStatsFile.delete();
        logFile.delete();
        coverageFile.delete();
        for (File file : savedCorpusDirectory.listFiles()) {
            file.delete();
        }
        for (File file : savedFailuresDirectory.listFiles()) {
            file.delete();
        }

        appendLineToFile(statsFile, getStatNames());
        appendLineToFile(failureStatsFile, getFailureStatNames());
    }

    protected String getStatNames() {
        return "# unix_time, cycles_done, cur_path, paths_total, pending_total, " +
                "pending_favs, map_size, unique_crashes, unique_hangs, max_depth, execs_per_sec, " +
                "valid_inputs, invalid_inputs, valid_cov, all_covered_probes, valid_covered_probes, num_coverage_probes, " +
                "covered_semantic_probes, num_semantic_probes, b0, b1, b2";
    }

    protected String getFailureStatNames() {
        return "# ttd, exception_class, stack_hash, coverage_hash, top5_stack_trace";
    }

    /* Writes a line of text to a given log file. */
    protected void appendLineToFile(File file, String line) throws GuidanceException {
        try (PrintWriter out = new PrintWriter(new FileWriter(file, true))) {
            out.println(line);
        } catch (IOException e) {
            throw new GuidanceException(e);
        }
    }

    /* Writes a line of text to the log file. */
    protected void infoLog(String str, Object... args) {
        if (verbose) {
            String line = String.format(str, args);
            if (logFile != null) {
                appendLineToFile(logFile, line);

            } else {
                System.err.println(line);
            }
        }
    }

    protected String millisToDuration(long millis) {
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis % TimeUnit.MINUTES.toMillis(1));
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis % TimeUnit.HOURS.toMillis(1));
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        String result = "";
        if (hours > 0) {
            result = hours + "h ";
        }
        if (hours > 0 || minutes > 0) {
            result += minutes + "m ";
        }
        result += seconds + "s";
        return result;
    }

    // Call only if console exists
    protected void displayStats(boolean force) {
        Date now = new Date();
        long intervalMilliseconds = now.getTime() - lastRefreshTime.getTime();
        intervalMilliseconds = Math.max(1, intervalMilliseconds);
        if (intervalMilliseconds < STATS_REFRESH_TIME_PERIOD && !force) {
            return;
        }
        long interlvalTrials = numTrials - lastNumTrials;
        long intervalExecsPerSec = interlvalTrials * 1000L;
        double intervalExecsPerSecDouble = interlvalTrials * 1000.0;
        if(intervalMilliseconds != 0) {
            intervalExecsPerSec = interlvalTrials * 1000L / intervalMilliseconds;
            intervalExecsPerSecDouble = interlvalTrials * 1000.0 / intervalMilliseconds;
        }
        lastRefreshTime = now;
        lastNumTrials = numTrials;
        long elapsedMilliseconds = now.getTime() - startTime.getTime();
        elapsedMilliseconds = Math.max(1, elapsedMilliseconds);
        long execsPerSec = numTrials * 1000L / elapsedMilliseconds;

        String currentParentInputDesc;
        if (seedInputs.size() > 0 || savedInputs.isEmpty()) {
            currentParentInputDesc = "<seed>";
        } else {
            SplitInput currentParentInput = savedInputs.get(currentParentInputIdx);
            currentParentInputDesc = currentParentInputIdx + " ";
            currentParentInputDesc += currentParentInput.isFavored() ? "(favored)" : "(not favored)";
            currentParentInputDesc += " {" + numChildrenGeneratedForCurrentParentInput +
                    "/" + getTargetChildrenForParent(currentParentInput) + " mutations}";
        }

        int nonZeroCount = totalCoverage.getNonZeroCount();
        int nonZeroValidCount = validCoverage.getNonZeroCount();
        double nonZeroFraction;
        double nonZeroValidFraction;
        int numTotalProbes = probeCounter.getNumTotalProbes();
        if (this.runCoverage instanceof FastNonCollidingCoverage) {
            nonZeroFraction = numTotalProbes > 0 ? nonZeroCount * 100.0 / numTotalProbes : 0;
            nonZeroValidFraction = numTotalProbes > 0 ? nonZeroValidCount * 100.0 / numTotalProbes : 0;
        } else {
            nonZeroFraction = nonZeroCount * 100.0 / totalCoverage.size();
            nonZeroValidFraction = nonZeroValidCount * 100.0 / validCoverage.size();
        }
        long semanticNonZeroCount = semanticTotalCoverage.getNonZeroCount();
        int numSemanticProbes = probeCounter.getNumSemanticProbes();

        BehavioralDiversityMetrics divMetrics = branchHitCounter.getCachedMetrics(force);

        if (console != null) {
            if (LIBFUZZER_COMPAT_OUTPUT) {
                console.printf("#%,d\tNEW\tcov: %,d exec/s: %,d L: %,d\n", numTrials, nonZeroValidCount, intervalExecsPerSec, currentInput.size());
            } else if (!QUIET_MODE) {
                console.printf("\033[2J");
                console.printf("\033[H");
                console.printf(this.getTitle() + "\n");
                if (this.testName != null) {
                    console.printf("Test name:            %s\n", this.testName);
                }

                String instrumentationType = "Janala";
                if (this.runCoverage instanceof FastNonCollidingCoverage) {
                    instrumentationType = "Fast";
                }
                console.printf("Instrumentation:      %s\n", instrumentationType);
                console.printf("Results directory:    %s\n", this.outputDirectory.getAbsolutePath());
                console.printf("Elapsed time:         %s (%s)\n", millisToDuration(elapsedMilliseconds),
                        maxDurationMillis == Long.MAX_VALUE ? "no time limit" : ("max " + millisToDuration(maxDurationMillis)));
                console.printf("Number of executions: %,d (%s)\n", numTrials,
                        maxTrials == Long.MAX_VALUE ? "no trial limit" : ("max " + maxTrials));
                console.printf("Valid inputs:         %,d (%.2f%%)\n", numValid, numValid * 100.0 / numTrials);
                console.printf("Cycles completed:     %d\n", cyclesCompleted);
                console.printf("Unique failures:      %,d\n", uniqueFailures.size());
                console.printf("Queue size:           %,d (%,d favored last cycle)\n", savedInputs.size(), numFavoredLastCycle);
                console.printf("Current parent input: %s\n", currentParentInputDesc);
                console.printf("Execution speed:      %,d/sec now | %,d/sec overall\n", intervalExecsPerSec, execsPerSec);
                console.printf("Total coverage:       %,d branches (%.2f%% of map)\n", nonZeroCount, nonZeroFraction);
                console.printf("Valid coverage:       %,d branches (%.2f%% of map)\n", nonZeroValidCount, nonZeroValidFraction);
                if (TRACK_SEMANTIC_COVERAGE) {
                    double semanticFraction = numSemanticProbes > 0 ? semanticNonZeroCount * 100.0 / numSemanticProbes : 0;
                    console.printf("Semantic coverage:    %,d branches (%.2f%% of map)\n", semanticNonZeroCount, semanticFraction);
                }
                console.printf("Behavioral diversity: b0: %.0f | b1: %.0f | b2: %.0f\n", divMetrics.b0(), divMetrics.b1(), divMetrics.b2());
                console.printf("Input structures:     %,d\n", exploredInputStructures.size());
                if (!savedInputs.isEmpty()) {
                    SplitLinearInput currentParentInput = savedInputs.get(currentParentInputIdx);
                    console.printf("Exploration score:    %.2f\n", currentParentInput.getStructureScore());
                    console.printf("Exploitation score:   %.2f\n", currentParentInput.getValueScore());
                }
            }
        }

        String plotData = String.format("%d, %d, %d, %d, %d, %d, %.2f%%, %d, %d, %d, %.2f, %d, %d, %.2f%%, %d, %d, %d, %d, %d, %.2f, %.2f, %.2f",
                TimeUnit.MILLISECONDS.toSeconds(now.getTime()), cyclesCompleted, currentParentInputIdx,
                numSavedInputs, 0, 0, nonZeroFraction, uniqueFailures.size(), 0, 0, intervalExecsPerSecDouble,
                numValid, numTrials-numValid, nonZeroValidFraction, nonZeroCount, nonZeroValidCount, numTotalProbes,
                semanticNonZeroCount, numSemanticProbes, divMetrics.b0(), divMetrics.b1(), divMetrics.b2());

        appendLineToFile(statsFile, plotData);

        if (LOG_BRANCH_HIT_COUNTS) {
            String branchHitCountsFileName = String.format("id_%06d", branchHitCountsFileIdx++);
            File saveFile = new File(branchHitCountsDirectory, branchHitCountsFileName);
            writeBranchHitCountFile(saveFile);
        }
    }

    /** Updates the branch hit count file. */
    protected void writeBranchHitCountFile(File saveFile) throws GuidanceException{
        try (BufferedOutputStream out = new BufferedOutputStream(Files.newOutputStream(saveFile.toPath()))) {
            out.write(mapper.writeValueAsBytes(branchHitCounter.getHitCounts()));
        } catch (IOException e) {
            throw new GuidanceException(e);
        }
    }

    /** Updates the data in the coverage file */
    protected void updateCoverageFile() {
        try {
            PrintWriter pw = new PrintWriter(coverageFile);
            pw.println(getTotalCoverage().toString());
            pw.println("Hash code: " + getTotalCoverage().hashCode());
            pw.close();
        } catch (FileNotFoundException ignore) {
            throw new GuidanceException(ignore);
        }
    }

    /* Returns the banner to be displayed on the status screen */
    protected String getTitle() {
        return  "BeDivFuzz: Behavioral Diversity Fuzzing\n" +
                "--------------------------------------------\n";
    }


    public void setBlind(boolean blind) {
        this.blind = blind;
    }

    protected int getTargetChildrenForParent(SplitInput parentInput) {
        // Baseline is a constant
        int target = NUM_CHILDREN_BASELINE;

        // We like inputs that cover many things, so scale with fraction of max
        if (maxCoverage > 0) {
            target = (NUM_CHILDREN_BASELINE * parentInput.nonZeroCoverage) / maxCoverage;
        }

        // We absolutely love favored inputs, so fuzz them more
        if (parentInput.isFavored()) {
            target = target * NUM_CHILDREN_MULTIPLIER_FAVORED;
        }

        return target;
    }

    /** Handles the end of fuzzing cycle (i.e., having gone through the entire queue) */
    protected void completeCycle() {
        // Increment cycle count
        cyclesCompleted++;
        infoLog("\n# Cycle " + cyclesCompleted + " completed.");

        // Go over all inputs and do a sanity check (plus log)
        infoLog("Here is a list of favored inputs:");
        int sumResponsibilities = 0;
        numFavoredLastCycle = 0;
        for (SplitInput input : savedInputs) {
            if (input.isFavored()) {
                int responsibleFor = input.responsibilities.size();
                infoLog("Input %d is responsible for %d branches", input.id, responsibleFor);
                sumResponsibilities += responsibleFor;
                numFavoredLastCycle++;
            }
        }
        int totalCoverageCount = totalCoverage.getNonZeroCount();
        infoLog("Total %d branches covered", totalCoverageCount);
        if (sumResponsibilities != totalCoverageCount) {
            if (multiThreaded) {
                infoLog("Warning: other threads are adding coverage between test executions");
            } else {
                throw new AssertionError("Responsibilty mismatch");
            }
        }

        // Break log after cycle
        infoLog("\n\n\n");
    }

    /**
     * Spawns a new input from thin air (i.e., actually random)
     *
     * @return a fresh input
     */
    protected SplitLinearInput createFreshInput() {
        return new SplitLinearInput();
    }

    @Override
    public InputStream getInput() throws GuidanceException {
        throw new GuidanceException("BeDivFuzz only produces SplitInputs.");
    }

    @Override
    public SplitParameterStream getSplitInput() throws GuidanceException {
        conditionallySynchronize(multiThreaded, () -> {
            // Clear coverage stats for this run
            runCoverage.clear();
            semanticRunCoverage.clear();

            // Choose an input to execute based on state of queues
            if (!seedInputs.isEmpty()) {
                // First, if we have some specific seeds, use those
                currentInput = seedInputs.removeFirst();

                // Hopefully, the seeds will lead to new coverage and be added to saved inputs

            } else if (savedInputs.isEmpty()) {
                // If no seeds given try to start with something random
                if (!blind && numTrials > 100_000) {
                    throw new GuidanceException("Too many trials without coverage; " +
                            "likely all assumption violations");
                }

                // Make fresh input using either list or maps
                // infoLog("Spawning new input from thin air");
                currentInput = createFreshInput();
            } else {
                // The number of children to produce is determined by how much of the coverage
                // pool this parent input hits
                SplitInput currentParentInput = savedInputs.get(currentParentInputIdx);
                int targetNumChildren = getTargetChildrenForParent(currentParentInput);
                if (numChildrenGeneratedForCurrentParentInput >= targetNumChildren) {
                    // Select the next saved input to fuzz
                    currentParentInputIdx = (currentParentInputIdx + 1) % savedInputs.size();

                    // Count cycles
                    if (currentParentInputIdx == 0) {
                        completeCycle();
                    }

                    numChildrenGeneratedForCurrentParentInput = 0;
                }
                SplitLinearInput parent = savedInputs.get(currentParentInputIdx);

                // We still want to do some havoc mutations
                if (numChildrenGeneratedForCurrentParentInput <= 0.2 * targetNumChildren) {
                    currentInput = parent.fuzzHavoc(random);
                } else {
                    currentInput = parent.fuzz(random);
                }

                numChildrenGeneratedForCurrentParentInput++;

                // Write it to disk for debugging
                try {
                    writeCurrentInputToFile(currentStructuralInputFile, currentValueInputFile);
                } catch (IOException ignore) {
                }

                // Start time-counting for timeout handling
                this.runStart = new Date();
                this.branchCount = 0;
            }
        });

        return new SplitParameterStream(currentInput, random);
    }

    @Override
    public boolean hasInput() {
        Date now = new Date();
        long elapsedMilliseconds = now.getTime() - startTime.getTime();
        if (EXIT_ON_CRASH && uniqueFailures.size() >= 1) {
            // exit
            return false;
        }
        if(elapsedMilliseconds < maxDurationMillis
                && numTrials < maxTrials) {
            return true;
        } else {
            displayStats(true);
            return false;
        }
    }

    @Override
    public void observeGeneratedArgs(Object[] args) {
        lastGeneratedStructureHash = currentInput.hashCode();
    }

    @Override
    public void handleResult(Result result, Throwable error) throws GuidanceException {
        conditionallySynchronize(multiThreaded, () -> {
            // Stop timeout handling
            this.runStart = null;

            // Increment run count
            this.numTrials++;

            boolean valid = result == Result.SUCCESS;

            if (valid) {
                // Increment valid counter
                numValid++;
            }

            if (result == Result.SUCCESS || (result == Result.INVALID && !SAVE_ONLY_VALID)) {

                // Compute a list of keys for which this input can assume responsibility.
                // Newly covered branches are always included.
                // Existing branches *may* be included, depending on the heuristics used.
                // A valid input will steal responsibility from invalid inputs
                IntHashSet responsibilities = computeResponsibilities(valid);

                // Determine if this input should be saved
                List<String> savingCriteriaSatisfied = checkSavingCriteriaSatisfied(result);
                boolean toSave = savingCriteriaSatisfied.size() > 0;

                if (toSave) {

                    String why = String.join(" ", savingCriteriaSatisfied);

                    // Trim input (remove unused keys)
                    currentInput.gc();

                    // It must still be non-empty
                    assert (currentInput.size() > 0) : String.format("Empty input: %s", currentInput.desc);

                    // Add new valid structure which has increased coverage
                    exploredInputStructures.add(currentInput.structuralParameters.hashCode());

                    // libFuzzerCompat stats are only displayed when they hit new coverage
                    if (LIBFUZZER_COMPAT_OUTPUT) {
                        displayStats(false);
                    }

                    infoLog("Saving new input (at run %d): " +
                                    "input #%d " +
                                    "of size %d; " +
                                    "reason = %s",
                            numTrials,
                            savedInputs.size(),
                            currentInput.size(),
                            why);
                    // Save input to queue and to disk
                    final String reason = why;
                    GuidanceException.wrap(() -> saveCurrentInput(responsibilities, reason));

                    // Update coverage information
                    updateCoverageFile();
                }
            } else if (result == Result.FAILURE || result == Result.TIMEOUT) {
                Date now = new Date();
                long elapsedMilliseconds = now.getTime() - startTime.getTime();

                String msg = error.getMessage();

                // Get the root cause of the failure
                Throwable rootCause = error;
                while (rootCause.getCause() != null) {
                    rootCause = rootCause.getCause();
                }

                // Attempt to add this to the set of unique failures
                if (uniqueFailures.add(failureDigest(rootCause.getStackTrace()))) {

                    // Trim input (remove unused keys)
                    currentInput.gc();

                    // It must still be non-empty
                    assert (currentInput.size() > 0) : String.format("Empty input: %s", currentInput.desc);

                    // Save crash to disk
                    int crashIdx = uniqueFailures.size() - 1;

                    try {
                        File failureDirectory = IOUtils.createDirectory(savedFailuresDirectory, String.format("id_%06d", crashIdx));
                        File structuralSaveFile = new File(failureDirectory, "structural_parameters");
                        File valueSaveFile = new File(failureDirectory, "value_parameters");
                        writeCurrentInputToFile(structuralSaveFile, valueSaveFile);
                    } catch (IOException e) {
                        throw new GuidanceException(String.format("Error while saving failing input: id_%06d", crashIdx));
                    }

                    String how = currentInput.desc;
                    String why = result == Result.FAILURE ? "crash" : "hang";
                    infoLog("%d Found failure id_%06d: %s %s %s %s", elapsedMilliseconds, crashIdx, why, error.getClass(),(msg != null ? msg : "(no message)"), how);

                    String stackTrace = Arrays.stream(rootCause.getStackTrace())
                            .map(StackTraceElement::toString)
                            .limit(5)
                            .collect(Collectors.joining("-"));

                    int stackHash = stackTrace.hashCode();

                    // # ttd, exception_class, stack_hash, coverage_nonzero_hash, top5_stack_trace
                    String line = String.format("%d, %s, %d, %d, %s", elapsedMilliseconds, error.getClass(), stackHash, runCoverage.nonZeroHashCode(), stackTrace);
                    appendLineToFile(failureStatsFile, line);

                    if (EXACT_CRASH_PATH != null && !EXACT_CRASH_PATH.equals("")) {
                        File exactStructuralCrashFile = new File(EXACT_CRASH_PATH+"_structure.parameters");
                        File exactValueCrashFile = new File(EXACT_CRASH_PATH+"_value.parameters");
                        GuidanceException.wrap(() -> writeCurrentInputToFile(exactStructuralCrashFile, exactValueCrashFile));
                    }

                    // libFuzzerCompat stats are only displayed when they hit new coverage or crashes
                    if (LIBFUZZER_COMPAT_OUTPUT) {
                        displayStats(false);
                    }
                }
            }

            // displaying stats on every interval is only enabled for AFL-like stats screen
            if (!LIBFUZZER_COMPAT_OUTPUT) {
                displayStats(false);
            }

            // Save input unconditionally if such a setting is enabled
            if (LOG_ALL_INPUTS && (SAVE_ONLY_VALID ? valid : true)) {
                File logDirectory = new File(allInputsDirectory, result.toString().toLowerCase());

                String structuralSaveFileName = String.format("id_%06d_structure", numTrials);
                String valueSaveFileName = String.format("id_%06d_value", numTrials);

                File structuralSaveFile = new File(logDirectory, structuralSaveFileName);
                File valueSaveFile = new File(logDirectory, valueSaveFileName);

                GuidanceException.wrap(() -> writeCurrentInputToFile(structuralSaveFile, valueSaveFile));
            }
        });
    }

    // Return a list of saving criteria that have been satisfied for a non-failure input
    protected List<String> checkSavingCriteriaSatisfied(Result result) {
        // Coverage before
        int nonZeroBefore = totalCoverage.getNonZeroCount();
        int validNonZeroBefore = validCoverage.getNonZeroCount();

        // Update total coverage
        boolean coverageBitsUpdated = totalCoverage.updateBits(runCoverage);
        semanticTotalCoverage.updateBits(semanticRunCoverage);
        if (result == Result.SUCCESS) {
            validCoverage.updateBits(runCoverage);
        }

        // Update hit counts
        if (uniquePaths.add(runCoverage.hashCode())) {
            if (TRACK_SEMANTIC_COVERAGE) {
                branchHitCounter.incrementBranchCounts(semanticRunCoverage);
            } else {
                branchHitCounter.incrementBranchCounts(runCoverage);
            }
        }

        if (!savedInputs.isEmpty()) {
            SplitLinearInput currentParent = savedInputs.get(currentParentInputIdx);
            if (result == Result.SUCCESS && uniqueValidPaths.add(runCoverage.hashCode())) {
                currentParent.addScore(1);
            } else {
                currentParent.addScore(0);
            }
        }

        // Coverage after
        int nonZeroAfter = totalCoverage.getNonZeroCount();
        if (nonZeroAfter > maxCoverage) {
            maxCoverage = nonZeroAfter;
        }
        int validNonZeroAfter = validCoverage.getNonZeroCount();

        // Possibly save input
        List<String> reasonsToSave = new ArrayList<>();


        if (!DISABLE_SAVE_NEW_COUNTS && coverageBitsUpdated) {
            reasonsToSave.add("+count");
        }

        // Save if new total coverage found
        if (nonZeroAfter > nonZeroBefore) {
            reasonsToSave.add("+cov");
        }

        // Save if new valid coverage is found
        if (this.validityFuzzing && validNonZeroAfter > validNonZeroBefore) {
            reasonsToSave.add("+valid");
        }

        return reasonsToSave;
    }


    // Compute a set of branches for which the current input may assume responsibility
    protected IntHashSet computeResponsibilities(boolean valid) {
        IntHashSet result = new IntHashSet();

        // This input is responsible for all new coverage
        IntList newCoverage = runCoverage.computeNewCoverage(totalCoverage);
        if (newCoverage.size() > 0) {
            result.addAll(newCoverage);
        }

        // If valid, this input is responsible for all new valid coverage
        if (valid) {
            IntList newValidCoverage = runCoverage.computeNewCoverage(validCoverage);
            if (newValidCoverage.size() > 0) {
                result.addAll(newValidCoverage);
            }
        }

        // Perhaps it can also steal responsibility from other inputs
        if (STEAL_RESPONSIBILITY) {
            int currentNonZeroCoverage = runCoverage.getNonZeroCount();
            int currentInputSize = currentInput.size();
            IntHashSet covered = new IntHashSet();
            covered.addAll(runCoverage.getCovered());

            // Search for a candidate to steal responsibility from
            candidate_search:
            for (SplitInput candidate : savedInputs) {
                IntHashSet responsibilities = candidate.responsibilities;

                // Candidates with no responsibility are not interesting
                if (responsibilities.isEmpty()) {
                    continue candidate_search;
                }

                // To avoid thrashing, only consider candidates with either
                // (1) strictly smaller total coverage or
                // (2) same total coverage but strictly larger size
                if (candidate.nonZeroCoverage < currentNonZeroCoverage ||
                        (candidate.nonZeroCoverage == currentNonZeroCoverage &&
                                currentInputSize < candidate.size())) {

                    // Check if we can steal all responsibilities from candidate
                    IntIterator iter = responsibilities.intIterator();
                    while(iter.hasNext()){
                        int b = iter.next();
                        if (covered.contains(b) == false) {
                            // Cannot steal if this input does not cover something
                            // that the candidate is responsible for
                            continue candidate_search;
                        }
                    }
                    // If all of candidate's responsibilities are covered by the
                    // current input, then it can completely subsume the candidate
                    result.addAll(responsibilities);
                }

            }
        }

        return result;
    }

    protected void writeCurrentInputToFile(File structuralSaveFile, File valueSaveFile) throws IOException {
        // First, write out structural parameters
        try (BufferedOutputStream out = new BufferedOutputStream(Files.newOutputStream(structuralSaveFile.toPath()))) {
            for (Integer b : currentInput.structuralParameters) {
                assert (b >= 0 && b < 256);
                out.write(b);
            }
        }

        // Then write value parameters
        try (BufferedOutputStream out = new BufferedOutputStream(Files.newOutputStream(valueSaveFile.toPath()))) {
            for (Integer b : currentInput.valueParameters) {
                assert (b >= 0 && b < 256);
                out.write(b);
            }
        }

    }

    /* Saves an interesting input to the queue. */
    protected void saveCurrentInput(IntHashSet responsibilities, String why) throws IOException {

        // First, save to disk (note: we issue IDs to everyone, but only write to disk  if valid)
        int newInputIdx = numSavedInputs++;
        File savedInputDirectory = IOUtils.createDirectory(savedCorpusDirectory, String.format("id_%06d", newInputIdx));
        File structuralSaveFile = new File(savedInputDirectory, "structural_parameters");
        File valueSaveFile = new File(savedInputDirectory, "value_parameters");
        writeCurrentInputToFile(structuralSaveFile, valueSaveFile);

        String how = currentInput.desc;
        infoLog("Saved - %s/id_%06d_(structure|value) %s %s", savedCorpusDirectory.getPath(), newInputIdx, how, why);

        // If not using guidance, do nothing else
        if (blind) {
            return;
        }

        // Second, save to queue
        savedInputs.add(currentInput);

        // Third, store basic book-keeping data
        currentInput.id = newInputIdx;
        currentInput.structuralSaveFile = structuralSaveFile;
        currentInput.valueSaveFile = valueSaveFile;
        currentInput.coverage = runCoverage.copy();
        currentInput.nonZeroCoverage = runCoverage.getNonZeroCount();
        currentInput.offspring = 0;
        savedInputs.get(currentParentInputIdx).offspring += 1;

        // Fourth, assume responsibility for branches
        currentInput.responsibilities = responsibilities;
        if (responsibilities.size() > 0) {
            currentInput.setFavored();
        }
        IntIterator iter = responsibilities.intIterator();
        while(iter.hasNext()){
            int b = iter.next();
            // If there is an old input that is responsible,
            // subsume it
            SplitLinearInput oldResponsible = responsibleInputs.get(b);
            if (oldResponsible != null) {
                oldResponsible.responsibilities.remove(b);
                // infoLog("-- Stealing responsibility for %s from input %d", b, oldResponsible.id);
            } else {
                // infoLog("-- Assuming new responsibility for %s", b);
            }
            // We are now responsible
            responsibleInputs.put(b, currentInput);
        }

    }

    @Override
    public Consumer<TraceEvent> generateCallBack(Thread thread) {
        if (firstThread == null) {
            firstThread = thread;
        } else if (firstThread != thread) {
            multiThreaded = true;
        }
        return this::handleEvent;
    }

    /**
     * Handles a trace event generated during test execution.
     *
     * Not used by FastNonCollidingCoverage, which does not allocate an
     * instance of TraceEvent at each branch probe execution.
     *
     * @param e the trace event to be handled
     */
    protected void handleEvent(TraceEvent e) {
        conditionallySynchronize(multiThreaded, () -> {
            // Collect totalCoverage
            ((Coverage) runCoverage).handleEvent(e);
            // Check for possible timeouts every so often
            if (this.singleRunTimeoutMillis > 0 &&
                    this.runStart != null && (++this.branchCount) % 10_000 == 0) {
                long elapsed = new Date().getTime() - runStart.getTime();
                if (elapsed > this.singleRunTimeoutMillis) {
                    throw new TimeoutException(elapsed, this.singleRunTimeoutMillis);
                }
            }
        });
    }

    /**
     * Returns a reference to the coverage statistics.
     * @return a reference to the coverage statistics
     */
    public ICoverage getTotalCoverage() {
        return totalCoverage;
    }

    /**
     * Conditionally run a method using synchronization.
     *
     * This is used to handle multi-threaded fuzzing.
     */
    protected void conditionallySynchronize(boolean cond, Runnable task) {
        if (cond) {
            synchronized (this) {
                task.run();
            }
        } else {
            task.run();
        }
    }

    private static MessageDigest sha1;

    protected static String failureDigest(StackTraceElement[] stackTrace) {
        if (sha1 == null) {
            try {
                sha1 = MessageDigest.getInstance("SHA-1");
            } catch (NoSuchAlgorithmException e) {
                throw new GuidanceException(e);
            }
        }
        byte[] bytes = sha1.digest(Arrays.deepToString(stackTrace).getBytes());
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16)
                    .substring(1));
        }
        return sb.toString();
    }

}

