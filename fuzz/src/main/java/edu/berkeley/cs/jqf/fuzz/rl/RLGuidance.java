package edu.berkeley.cs.jqf.fuzz.rl;

import java.time.Duration;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.eclipsecollections.EclipseCollectionsModule;
import de.hub.se.jqf.bedivfuzz.util.BehavioralDiversityMetrics;
import de.hub.se.jqf.bedivfuzz.util.BranchHitCounter;
import edu.berkeley.cs.jqf.fuzz.guidance.Guidance;
import edu.berkeley.cs.jqf.fuzz.guidance.GuidanceException;
import edu.berkeley.cs.jqf.fuzz.guidance.Result;
import edu.berkeley.cs.jqf.fuzz.guidance.TimeoutException;
import edu.berkeley.cs.jqf.fuzz.util.Coverage;
import edu.berkeley.cs.jqf.fuzz.util.CoverageFactory;
import edu.berkeley.cs.jqf.fuzz.util.FastNonCollidingCoverage;
import edu.berkeley.cs.jqf.fuzz.util.ICoverage;
import edu.berkeley.cs.jqf.fuzz.util.IOUtils;
import edu.berkeley.cs.jqf.instrument.tracing.FastCoverageSnoop;
import edu.berkeley.cs.jqf.instrument.tracing.FastSemanticCoverageSnoop;
import edu.berkeley.cs.jqf.instrument.tracing.events.TraceEvent;
import janala.instrument.FastCoverageListener;
import janala.instrument.ProbeCounter;
import org.eclipse.collections.api.iterator.IntIterator;
import org.eclipse.collections.api.list.primitive.IntList;
import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Created by clemieux on 6/17/19.
 */
public class RLGuidance implements Guidance {

    private RLGenerator generator;

    // Currently, we only support single-threaded applications
    // This field is used to ensure that
    protected Thread appThread;

    /** The name of the test for display purposes. */
    protected final String testName;

    // ------------ ALGORITHM BOOKKEEPING ------------

    /** The max amount of time to run for, in milli-seconds */
    protected final long maxDurationMillis;

    /** Maximum number of trials to run */
    protected Long maxTrials = Long.getLong("jqf.guidance.MAX_TRIALS");

    /** The number of trials completed. */
    protected long numTrials = 0;

    /** The number of valid inputs. */
    protected long numValid = 0;

    /** The directory where fuzzing results are written. */
    protected final File outputDirectory;

    /** The directory where saved inputs are written. */
    protected File savedCorpusDirectory;

    /** The directory where saved inputs are written. */
    protected File savedFailuresDirectory;

    /** The directory where all branch hit count distributions are logged (if enabled). */
    protected File branchHitCountsDirectory;

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

    /** Set of hashes of all valid inputs generated so far. */
    protected Set<Integer> uniqueValidInputs = new HashSet<>();

    /** Set of hashes of all valid paths generated so far. */
    protected IntHashSet uniqueValidPaths = new IntHashSet();

    /** Set of hashes of all paths generated so far. */
    protected IntHashSet uniquePaths = new IntHashSet();

    /** Coverage diversity metrics for all unique paths. */
    protected BranchHitCounter branchHitCounter = new BranchHitCounter();

    /** Unique branch sets for valid inputs */
    protected Set<Integer> uniqueBranchSets = new HashSet<>();

    /** The set of unique failures found so far. */
    protected Set<String> uniqueFailures = new HashSet<>();

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
    protected final long STATS_REFRESH_TIME_PERIOD = Integer.getInteger("jqf.guidance.STATS_REFRESH_TIME_PERIOD", 1000);

    /** The file where log data is written. */
    protected File logFile;

    /** The file where saved plot data is written. */
    protected File statsFile;

    /** The file where failure info is written. */
    protected File failureStatsFile;

    /** The currently executing input (for debugging purposes). */
    protected String currentInput;

    /** The object mapper responsible for serializing the branch hit counts. */
    protected ObjectMapper mapper;

    /** Whether to log the branch hit counts over time. **/
    protected final boolean LOG_BRANCH_HIT_COUNTS = Boolean.getBoolean("jqf.guidance.LOG_BRANCH_HIT_COUNTS");

    /** Index of the current branch hit count distribution file. **/
    protected int branchHitCountsFileIdx = 0;

    /** Whether to hide fuzzing statistics **/
    protected final boolean QUIET_MODE = Boolean.getBoolean("jqf.ei.QUIET_MODE");

    /** The current number of probes inserted through instrumentation. */
    protected final ProbeCounter probeCounter = ProbeCounter.instance;

    /** Metrics to collect. */
    protected boolean COUNT_UNIQUE_PATHS = false;
    protected boolean MEASURE_BEHAVIORAL_DIVERSITY = false;
    protected boolean TRACK_SEMANTIC_COVERAGE = false;

    // ------------- TIMEOUT HANDLING ------------

    /** Timeout for an individual run. */
    protected long singleRunTimeoutMillis;

    /** Date when last run was started. */
    protected Date runStart;

    /** Number of conditional jumps since last run was started. */
    protected long branchCount;

    // ----------- FUZZING HEURISTICS ------------

   /** Whether to use greybox information in rewards **/
    static final boolean USE_GREYBOX = Boolean.getBoolean("rl.guidance.USE_GREYBOX");

    public RLGuidance(RLGenerator g, String testName, Duration duration, File outputDirectory) throws IOException {
        this.generator = g;
        this.testName = testName;
        this.maxDurationMillis = duration != null ? duration.toMillis() : Long.MAX_VALUE;
        this.outputDirectory = outputDirectory;
        if (this.maxTrials == null) {
            this.maxTrials = Long.MAX_VALUE;
        }

        // Parse metrics to collect
        String metrics = System.getProperty("jqf.guidance.METRICS");
        if (metrics != null && !metrics.isEmpty()) {
            for (String metric : metrics.split(":")) {
                if (metric.equals("UPATHS")) {
                    COUNT_UNIQUE_PATHS = true;
                } else if (metric.equals("BEDIV")) {
                    MEASURE_BEHAVIORAL_DIVERSITY = true;
                } else if (metric.equals("SEMCOV")) {
                    TRACK_SEMANTIC_COVERAGE = true;
                } else {
                    throw new GuidanceException("Unknown metric: " + metric);
                }
            }
        }

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

    @Override
    public InputStream getInput() throws IllegalStateException, GuidanceException {
        runCoverage.clear();
        if (TRACK_SEMANTIC_COVERAGE) semanticRunCoverage.clear();
        currentInput = generator.generate();

        // Start time-counting for timeout handling
        this.runStart = new Date();
        this.branchCount = 0;

        return new ByteArrayInputStream(currentInput.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public boolean hasInput() {
        Date now = new Date();
        long elapsedMilliseconds = now.getTime() - startTime.getTime();
        if(elapsedMilliseconds < maxDurationMillis
                && numTrials < maxTrials) {
            return true;
        } else {
            displayStats(true);
            return false;
        }
    }


    @Override
    public void handleResult(Result result, Throwable error) throws GuidanceException {
        // Stop timeout handling
        this.runStart = null;

        // Increment run count
        this.numTrials++;

        boolean valid = result == Result.SUCCESS;

        if (valid) {
            // Increment valid counter
            numValid++;
        }

        if (result == Result.SUCCESS || result == Result.INVALID) {

            // Update hit counts
            boolean checkUniquePath = COUNT_UNIQUE_PATHS || MEASURE_BEHAVIORAL_DIVERSITY;
            if (checkUniquePath && uniquePaths.add(runCoverage.hashCode())) {
                if(MEASURE_BEHAVIORAL_DIVERSITY) {
                    if (TRACK_SEMANTIC_COVERAGE) {
                        branchHitCounter.incrementBranchCounts(semanticRunCoverage);
                    } else {
                        branchHitCounter.incrementBranchCounts(runCoverage);
                    }
                }
            }

            // Coverage before
            int nonZeroBefore = totalCoverage.getNonZeroCount();
            int validNonZeroBefore = validCoverage.getNonZeroCount();

            // Update total coverage
            boolean coverageBitsUpdated = totalCoverage.updateBits(runCoverage);
            if (TRACK_SEMANTIC_COVERAGE) semanticTotalCoverage.updateBits(semanticRunCoverage);

            int nonZeroAfter = totalCoverage.getNonZeroCount();

            if (valid) {
                validCoverage.updateBits(runCoverage);
                uniqueValidPaths.add(runCoverage.hashCode());
                if (!uniqueValidInputs.contains(currentInput.hashCode())){
                    uniqueValidInputs.add(currentInput.hashCode());

                    boolean has_new_branches_covered = uniqueBranchSets.add(runCoverage.nonZeroHashCode());
                    
                    if (USE_GREYBOX) {
                      // Greybox: only reward for inputs that cover new branches
                        if (has_new_branches_covered){
                            generator.update(20);
                        } else {
                            generator.update(0);
                        }
                    } else {
                        // Regular behavior: reward for inputs that are unique (see outer if)
                        generator.update(20);
                    }

                } else {
                    // TODO: allow this to be customizable
                    generator.update(0);
                }

            } else {
                generator.update(-1);
            }


            // Coverage after

            int validNonZeroAfter = validCoverage.getNonZeroCount();

            if (nonZeroAfter > nonZeroBefore || validNonZeroAfter > validNonZeroBefore) {
                try {
                    saveCurrentInput(valid);
                } catch (IOException e) {
                    throw new GuidanceException(e);
                }
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

                // Save crash to disk
                try {
                    saveCurrentFailure();
                } catch (IOException e) {
                    throw new GuidanceException(e);
                }

                String why = result == Result.FAILURE ? "crash" : "hang";
                int crashIdx = uniqueFailures.size() - 1;
                infoLog("%d Found failure id_%06d: %s %s %s", elapsedMilliseconds, crashIdx, why, error.getClass(),(msg != null ? msg : "(no message)"));

                String stackTrace = Arrays.stream(rootCause.getStackTrace())
                        .map(StackTraceElement::toString)
                        .limit(5)
                        .collect(Collectors.joining("-"));

                int stackHash = stackTrace.hashCode();

                // # ttd, exception_class, stack_hash, coverage_nonzero_hash, top5_stack_trace
                String line = String.format("%d, %s, %d, %d, %s", elapsedMilliseconds, error.getClass(), stackHash, runCoverage.nonZeroHashCode(), stackTrace);
                appendLineToFile(failureStatsFile, line);
            }

        }

        displayStats(false);

    }


    /// CL Note: Below this is boiler-plate that probably doesn't need to be messed with
    private void prepareOutputDirectory() throws IOException {
        // Create the output directory if it does not exist
        IOUtils.createDirectory(outputDirectory);

        // Name files and directories after AFL
        this.savedCorpusDirectory = IOUtils.createDirectory(outputDirectory, "corpus");
        this.savedFailuresDirectory = IOUtils.createDirectory(outputDirectory, "failures");

        this.statsFile = new File(outputDirectory, "plot_data");
        this.logFile = new File(outputDirectory, "fuzz.log");
        this.failureStatsFile = new File(outputDirectory, "failure_info.csv");

        // Perioridically serialize hit-counts (or after predefined timeout when assessing behavioral diversity)
        if (LOG_BRANCH_HIT_COUNTS || MEASURE_BEHAVIORAL_DIVERSITY) {
            this.MEASURE_BEHAVIORAL_DIVERSITY = true; // Make sure we are counting hit counts
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
        for (File file : savedCorpusDirectory.listFiles()) {
            if (file.isDirectory()) { // BeDivFuzz generated folder structure
                for (File innerFile : file.listFiles()) {
                    innerFile.delete();
                }
            }
            file.delete();
        }
        for (File file : savedFailuresDirectory.listFiles()) {
            if (file.isDirectory()) { // BeDivFuzz generated folder structure
                for (File innerFile : file.listFiles()) {
                    innerFile.delete();
                }
            }
            file.delete();
        }

        appendLineToFile(statsFile, getStatNames());
        appendLineToFile(failureStatsFile, getFailureStatNames());

    }

    protected String getStatNames() {
        return "# unix_time, cycles_done, cur_path, paths_total, pending_total, " +
                "pending_favs, map_size, unique_crashes, unique_hangs, max_depth, execs_per_sec, " +
                "valid_inputs, invalid_inputs, valid_cov, all_covered_probes, valid_covered_probes, num_coverage_probes, " +
                "covered_semantic_probes, num_semantic_probes, unique_paths, unique_valid_paths, b0, b1, b2";
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

    private String millisToDuration(long millis) {
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
        long intervalTrials = numTrials - lastNumTrials;
        long intervalExecsPerSec = intervalTrials * 1000L;
        double intervalExecsPerSecDouble = intervalTrials * 1000.0;
        if(intervalMilliseconds != 0) {
            intervalExecsPerSec = intervalTrials * 1000L / intervalMilliseconds;
            intervalExecsPerSecDouble = intervalTrials * 1000.0 / intervalMilliseconds;
        }
        lastRefreshTime = now;
        lastNumTrials = numTrials;
        long elapsedMilliseconds = now.getTime() - startTime.getTime();
        elapsedMilliseconds = Math.max(1, elapsedMilliseconds);
        long execsPerSec = numTrials * 1000L / elapsedMilliseconds;

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
            if (!QUIET_MODE) {
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
                console.printf("Unique failures:      %,d\n", uniqueFailures.size());
                console.printf("Saved inputs:         %,d\n", numSavedInputs);
                console.printf("Execution speed:      %,d/sec now | %,d/sec overall\n", intervalExecsPerSec, execsPerSec);
                console.printf("\nCoverage:\n");
                console.printf("  Total coverage:     %,d branches (%.2f%% of map)\n", nonZeroCount, nonZeroFraction);
                console.printf("  Valid coverage:     %,d branches (%.2f%% of map)\n", nonZeroValidCount, nonZeroValidFraction);
                if (TRACK_SEMANTIC_COVERAGE) {
                    double semanticFraction = numSemanticProbes > 0 ? semanticNonZeroCount * 100.0 / numSemanticProbes : 0;
                    console.printf("  Semantic coverage:  %,d branches (%.2f%% of map)\n", semanticNonZeroCount, semanticFraction);
                }
                if (COUNT_UNIQUE_PATHS) {
                    int numUniqueValidPaths = uniqueValidPaths.size();
                    console.printf("  Unique valid paths: %,d (%.2f%% of execs)\n", numUniqueValidPaths, numUniqueValidPaths * 100.0 / numTrials);
                }
                if (MEASURE_BEHAVIORAL_DIVERSITY) {
                    console.printf("\nBehavioral Diversity:\n");
                    console.printf("  q = 0:              %.0f\n", divMetrics.b0());
                    console.printf("  q = 1:              %.0f\n", divMetrics.b1());
                    console.printf("  q = 2:              %.0f\n", divMetrics.b2());
                }
            }
        }

        String plotData = String.format(
                "%d, %d, %d, %d, %d, %d, %.2f%%, %d, %d, %d, %.2f, %d, %d, %.2f%%, %d, %d, %d, %d, %d, %d, %d, %.2f, %.2f, %.2f",
                TimeUnit.MILLISECONDS.toSeconds(now.getTime()),
                0,
                0,
                numSavedInputs,
                0,
                0,
                nonZeroFraction,
                uniqueFailures.size(),
                0,
                0,
                intervalExecsPerSecDouble,
                numValid,
                numTrials-numValid,
                nonZeroValidFraction,
                nonZeroCount,
                nonZeroValidCount,
                numTotalProbes,
                semanticNonZeroCount,
                numSemanticProbes,
                uniquePaths.size(),
                uniqueValidPaths.size(),
                divMetrics.b0(),
                divMetrics.b1(),
                divMetrics.b2()
        );

        appendLineToFile(statsFile, plotData);

        if (LOG_BRANCH_HIT_COUNTS || (MEASURE_BEHAVIORAL_DIVERSITY && force)) {
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

    /* Returns the banner to be displayed on the status screen */
    protected String getTitle() {
        return  "RLCheck: Reinforcement-Learning-Guided Fuzzing\n" +
                "--------------------------------------------\n";
    }

    @Override
    public Consumer<TraceEvent> generateCallBack(Thread thread) {
        if (appThread != null) {
            throw new IllegalStateException(RLGuidance.class +
                    " only supports single-threaded apps at the moment");
        }
        appThread = thread;

        return this::handleEvent;
    }

    /** Handles a trace event generated during test execution */
    protected void handleEvent(TraceEvent e) {
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
    }

    /* Saves an interesting input to the queue. */
    protected void saveCurrentInput(Boolean is_valid) throws IOException {
        String valid_str = is_valid ? "_v" : "";
        // First, save to disk (note: we issue IDs to everyone, but only write to disk  if valid)
        int newInputIdx = numSavedInputs++;
        String saveFileName = String.format("id_%06d%s", newInputIdx, valid_str);
        File saveFile = new File(savedCorpusDirectory, saveFileName);
        PrintWriter writer = new PrintWriter(saveFile);
        writer.print(currentInput);
        writer.flush();
        infoLog("Saved - %s", saveFileName);
    }

    /* Saves an interesting input to the queue. */
    protected void saveCurrentFailure() throws IOException {
        int newInputIdx = uniqueFailures.size();
        String saveFileName = String.format("id_%06d", newInputIdx);
        File saveFile = new File(savedFailuresDirectory, saveFileName);
        PrintWriter writer = new PrintWriter(saveFile);
        writer.print(currentInput);
        writer.flush();
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
