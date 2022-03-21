package de.hub.se.jqf.fuzz.div;

import de.hub.se.jqf.fuzz.guidance.BeDivGuidance;
import edu.berkeley.cs.jqf.fuzz.ei.ZestGuidance;
import edu.berkeley.cs.jqf.fuzz.guidance.GuidanceException;
import edu.berkeley.cs.jqf.fuzz.guidance.Result;
import edu.berkeley.cs.jqf.fuzz.util.Coverage;
import org.eclipse.collections.api.iterator.IntIterator;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;

import java.io.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * A guidance that performs structure-changing (exploration) and structure-preserving (exploitation) mutations.
 * @author lam
 */
public class BeDivFuzzGuidance extends ZestGuidance implements BeDivGuidance {

    // ------------ ALGORITHM BOOKKEEPING ------------

    /** Set of saved inputs to fuzz. */
    protected ArrayList<SplitLinearInput> savedInputs = new ArrayList<>();

    /** Set of input structures explored so far. */
    protected Set<Integer> exploredInputStructures = new HashSet<>();

    /** Hash code of last generated structure */
    protected int lastGeneratedStructureHash;

    /** Current input that's running -- valid after getInput() and before handleResult(). */
    protected SplitLinearInput currentInput;

    /** A mapping of coverage keys to inputs that are responsible for them. */
    protected Map<Object, SplitLinearInput> responsibleInputs = new HashMap<>(totalCoverage.size());

    /** Whether the last mutation was on the primary or secondary input (exploration or exploitation)*/
    enum Phase {INIT, EXPLORATION, EXPLOITATION, HAVOC}
    protected Phase currentMutationPhase = Phase.INIT;

    /** Parameter for epsilon-greedy exploration vs. exploitation trade-off */
    protected double epsilon = 0.2;

    /** Counters for attributing new coverage to exploration or exploitation */
    protected int currentExplorationCount = 0;
    protected double currentExplorationScore = 0;
    protected int currentExploitationCount = 0;
    protected double currentExploitationScore = 0;

    /** Overall exploration vs. exploitation stats */
    protected int totalExplorationCount = 0;
    protected double totalExplorationScore = 0;
    protected int totalExploitationCount = 0;
    protected double totalExploitationScore = 0;

    // ------------- FUZZING HEURISTICS ------------
    /** Baseline number of mutated children to produce from a given parent input. */
    static final int NUM_CHILDREN_BASELINE = 50;

    /** Multiplication factor for number of children to produce for favored inputs. */
    static final int NUM_CHILDREN_MULTIPLIER_FAVORED = 20;

    /** Whether to save only new valid structures in the queue */
    static final boolean SAVE_ONLY_NEW_STRUCTURES = Boolean.getBoolean("jqf.div.SAVE_ONLY_NEW_STRUCTURES");

    /** Whether to save only new valid structures, unless they increase valid coverage (not only counts) */
    static final boolean SAVE_OLD_STRUCTURE_IF_VALID_COV = Boolean.getBoolean("jqf.div.SAVE_OLD_STRUCTURE_IF_VALID_COV");

    /** Whether to give a reward for new coverage */
    static final boolean REWARD_NEW_COVERAGE = Boolean.getBoolean("jqf.div.REWARD_NEW_COVERAGE");

    /** Whether to give a reward for validity */
    static final boolean REWARD_UNIQUE_VALIDITY = Boolean.getBoolean("jqf.div.REWARD_UNIQUE_VALIDITY");

    /** Whether to give a reward for validity (enabled by default)*/
    static final boolean REWARD_UNIQUE_VALID_PATHS = true;

    /**
     * Creates a new guidance instance.
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
        super(testName, duration, trials, outputDirectory, sourceOfRandomness);

        // Make sure at most one strategy is selected
        if (SAVE_ONLY_NEW_STRUCTURES && SAVE_OLD_STRUCTURE_IF_VALID_COV) {
            throw new GuidanceException("Heuristic options -s and -v are mutually exclusive.");
        }
    }

    protected void writeInputToFile(ZestGuidance.Input<?> input, File saveFile) throws IOException {
        try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(saveFile))) {
            for (Integer b : input) {
                assert (b >= 0 && b < 256);
                out.write(b);
            }
        }
    }

    @Override
    protected void displayStats(boolean force) {
        Date now = new Date();
        long intervalMilliseconds = now.getTime() - lastRefreshTime.getTime();
        if (intervalMilliseconds < STATS_REFRESH_TIME_PERIOD && !force) {
            return;
        }
        long intervalTrials = numTrials - lastNumTrials;
        long intervalExecsPerSec = intervalTrials * 1000L / intervalMilliseconds;
        double intervalExecsPerSecDouble = intervalTrials * 1000.0 / intervalMilliseconds;
        lastRefreshTime = now;
        lastNumTrials = numTrials;
        long elapsedMilliseconds = now.getTime() - startTime.getTime();
        long execsPerSec = numTrials * 1000L / elapsedMilliseconds;

        String currentParentInputDesc;

        if (savedInputs.isEmpty()) {
            currentParentInputDesc = "<seed>";
        }
        else {
            SplitLinearInput currentParentInput = savedInputs.get(currentParentInputIdx);
            currentParentInputDesc = currentParentInputIdx + " ";
            currentParentInputDesc += currentParentInput.isFavored() ? "(favored)" : "(not favored)";
            currentParentInputDesc += " {" + numChildrenGeneratedForCurrentParentInput +
                    "/" + getTargetChildrenForParent(currentParentInput) + " mutations}";
        }

        int nonZeroCount = totalCoverage.getNonZeroCount();
        double nonZeroFraction = nonZeroCount * 100.0 / totalCoverage.size();
        int nonZeroValidCount = validCoverage.getNonZeroCount();
        double nonZeroValidFraction = nonZeroValidCount * 100.0 / validCoverage.size();

        double explorationScore = totalExplorationCount != 0 ?  totalExplorationScore / totalExplorationCount : 0;
        double exploitationScore = totalExploitationCount != 0 ? totalExploitationScore / totalExploitationCount : 0;

        double[] uniquePathsDivMetrics = uniquePathsDivMetricsCounter.getCachedMetrics(now);

        if (console != null ){
            if (LIBFUZZER_COMPAT_OUTPUT) {
                console.printf("#%,d\tNEW\tcov: %,d exec/s: %,d L: %,d\n", numTrials, nonZeroValidCount, intervalExecsPerSec, currentInput.size());
            } else if (!QUIET_MODE) {
                console.printf("\033[2J");
                console.printf("\033[H");
                console.printf(this.getTitle() + "\n");
                if (this.testName != null) {
                    console.printf("Test name:            %s\n", this.testName);
                }
                console.printf("Results directory:    %s\n", this.outputDirectory.getAbsolutePath());
                console.printf("Elapsed time:         %s (%s)\n", millisToDuration(elapsedMilliseconds),
                        maxDurationMillis == Long.MAX_VALUE ? "no time limit" : ("max " + millisToDuration(maxDurationMillis)));
                console.printf("Number of executions: %,d\n", numTrials);
                console.printf("Valid inputs:         %,d (%.2f%%)\n", numValid, numValid * 100.0 / numTrials);
                console.printf("Cycles completed:     %d\n", cyclesCompleted);
                console.printf("Unique failures:      %,d\n", uniqueFailures.size());
                console.printf("Queue size:           %,d (%,d favored last cycle)\n", savedInputs.size(), numFavoredLastCycle);
                console.printf("Current parent input: %s\n", currentParentInputDesc);
                console.printf("Execution speed:      %,d/sec now | %,d/sec overall\n", intervalExecsPerSec, execsPerSec);
				console.printf("Valid coverage:       %,d branches (%.2f%% of map)\n", nonZeroValidCount, nonZeroValidFraction);
				console.printf("Behavioral Diversity: (B(0): %.0f | B(1): %.0f | B(2): %.0f)\n", uniquePathsDivMetrics[0], uniquePathsDivMetrics[1], uniquePathsDivMetrics[2]);
                console.printf("Unique valid inputs:  %,d (%.2f%%)\n", uniqueValidInputs.size(),
                        uniqueValidInputs.size() * 100.0 / numTrials);
                console.printf("Unique valid paths:   %,d \n", uniqueValidPaths.size());
                console.printf("Structure-changing mutations (exploration):    %,d \n", totalExplorationCount);
                console.printf("Overall exploration score: %.3f \n", explorationScore);
                console.printf("Structure-preserving mutations (exploitation):    %,d \n", totalExploitationCount);
                console.printf("Overall exploitation score: %.3f \n", exploitationScore);
            }
        }


        String plotData = String.format("%d, %d, %d, %d, %d, %d, %d, %d, %d, %.3f, %.3f, %.3f",
                TimeUnit.MILLISECONDS.toSeconds(now.getTime()), uniqueFailures.size(), nonZeroCount, nonZeroValidCount,
                numTrials, numValid, uniqueValidPaths.size(), uniqueBranchSets.size(), uniqueValidInputs.size(),
                uniquePathsDivMetrics[0], uniquePathsDivMetrics[1], uniquePathsDivMetrics[2]
        );

        appendLineToFile(statsFile, plotData);

    }

    /** Returns the banner to be displayed on the status screen */
    protected String getTitle() {
        return "BeDivFuzz: Behavioral Diversity Fuzzing.";
    }

    protected int getTargetChildrenForParent(SplitLinearInput parentInput) {
        // Baseline is a constant
        int target = NUM_CHILDREN_BASELINE;

        // We like inputs that cover many things, so scale with fraction of max
        if (maxCoverage > 0) {
            target = (NUM_CHILDREN_BASELINE * parentInput.nonZeroCoverage) / maxCoverage;
        }

        // We absolutey love favored inputs, so fuzz them more
        if (parentInput.isFavored()) {
            target = target * NUM_CHILDREN_MULTIPLIER_FAVORED;
        }

        return target;
    }

    /** Handles the end of fuzzing cycle (i.e., having gone through the entire queue) */
    @Override
    protected void completeCycle() {
        // Increment cycle count
        cyclesCompleted++;
        //infoLog("\n# Cycle " + cyclesCompleted + " completed.");

        // Go over all inputs and do a sanity check (plus log)
        //infoLog("Here is a list of favored inputs:");
        int sumResponsibilities = 0;
        numFavoredLastCycle = 0;
        for (SplitLinearInput input : savedInputs) {
            if (input.isFavored()) {
                int responsibleFor = input.responsibilities.size();
                //infoLog("Input %d is responsible for %d branches", input.id, responsibleFor);
                sumResponsibilities += responsibleFor;
                numFavoredLastCycle++;
            }
        }
        int totalCoverageCount = totalCoverage.getNonZeroCount();
        //infoLog("Total %d branches covered", totalCoverageCount);
        if (sumResponsibilities != totalCoverageCount) {
            if (multiThreaded) {
                infoLog("Warning: other threads are adding coverage between test executions");
            } else {
                //System.out.println(sumResponsibilities + " " + totalCoverageCount + " " + allResponsibilities.size() + " " + allValidResponsibilities.size());
                //TODO: Fix
                //throw new AssertionError("Responsibility mismatch");
            }
        }

        // Break log after cycle
        //infoLog("\n\n\n");
    }

    @Override
    public void observeGeneratedArgs(Object[] args) {
        lastGeneratedInputsHash = Arrays.hashCode(args);
        lastGeneratedStructureHash = ((LinearInput) currentInput.primaryInput).hashCode();
    }

    @Override
    public InputStream getInput() throws GuidanceException {
        throw new GuidanceException("BeDivGuidance does not support getInput()");
    }

    public SplitLinearInput getSplitInput() throws GuidanceException {
        conditionallySynchronize(multiThreaded, () -> {
            // Clear coverage stats for this run
            runCoverage.clear();

            if (savedInputs.isEmpty()) {
                // If no seeds given try to start with something random
                if (!blind && numTrials > 100_000_000) {
                    throw new GuidanceException("Too many trials without coverage; " +
                            "likely all assumption violations");
                }

                currentInput = new SplitLinearInput(createFreshInput(), createFreshInput());

            } else {
                // The number of children to produce is determined by how much of the coverage
                // pool this parent input hits
                SplitLinearInput currentParentInput = savedInputs.get(currentParentInputIdx);
                int targetNumChildren = getTargetChildrenForParent(currentParentInput);
                if (numChildrenGeneratedForCurrentParentInput >= targetNumChildren) {
                    // Select the next saved input to fuzz
                    currentParentInputIdx = (currentParentInputIdx + 1) % savedInputs.size();

                    // Count cycles
                    if (currentParentInputIdx == 0) {
                        completeCycle();
                    }

                    numChildrenGeneratedForCurrentParentInput = 0;
                    currentExplorationCount = 0;
                    currentExplorationScore = 0;
                    currentExploitationCount = 0;
                    currentExploitationScore = 0;
                }

                LinearInput primaryParent = (LinearInput) currentParentInput.primaryInput;
                LinearInput secondaryParent = (LinearInput) currentParentInput.secondaryInput;

                if (secondaryParent.size() > 0) {
                    // with probability epsilon, perform random action
                    if (random.nextDouble() < epsilon) {
                        currentMutationPhase = random.nextBoolean() ? Phase.EXPLORATION : Phase.EXPLOITATION;
                    } else {
                        // choose best option (or random if tied)
                        double expectedExplorationReward = (currentExplorationCount != 0) ?
                                currentExplorationScore / currentExplorationCount : 0;

                        double expectedExploitationReward = (currentExploitationCount != 0) ?
                                currentExploitationScore / currentExploitationCount : 0;

                        if (expectedExplorationReward != expectedExploitationReward) {
                            currentMutationPhase = (expectedExplorationReward > expectedExploitationReward) ?
                                    Phase.EXPLORATION : Phase.EXPLOITATION;
                        } else {
                            currentMutationPhase = random.nextBoolean() ? Phase.EXPLORATION : Phase.EXPLOITATION;
                        }
                    }
                } else {
                    // can only perform exploration
                    currentMutationPhase = Phase.EXPLORATION;
                }

                if (currentMutationPhase == Phase.EXPLORATION) {
                    currentExplorationCount++;
                    currentInput = new SplitLinearInput(primaryParent.fuzz(random), new LinearInput(secondaryParent));
                } else {
                    currentExploitationCount++;
                    currentInput = new SplitLinearInput(new LinearInput(primaryParent), secondaryParent.fuzz(random));
                }

                numChildrenGeneratedForCurrentParentInput++;

                // Start time-counting for timeout handling
                this.runStart = new Date();
                this.branchCount = 0;
            }
        });

        return currentInput;
    }

    /**
     * Handles the result of a test execution.
     *
     */

    @Override
    public void handleResult(Result result, Throwable error) throws GuidanceException {
        conditionallySynchronize(multiThreaded, () -> {
            // Stop timeout handling
            this.runStart = null;

            // Increment run count
            this.numTrials++;

            boolean valid = result == Result.SUCCESS;

            if (uniquePaths.add(runCoverage.hashCode())) {
                uniquePathsDivMetricsCounter.incrementBranchCounts(runCoverage);
            }

            if (valid) {
                // Increment valid counter
                numValid++;

                // We won't add any coverage hash yet as we still need to decide whether to save the input
            }

            if (result == Result.SUCCESS || (result == Result.INVALID && !SAVE_ONLY_VALID)) {

                // Compute a list of keys for which this input can assume responsiblity.
                // Newly covered branches are always included.
                // Existing branches *may* be included, depending on the heuristics used.
                // A valid input will steal responsibility from invalid inputs
                IntHashSet responsibilities = computeResponsibilities(valid);

                // Determine if this input should be saved
                List<String> savingCriteriaSatisfied = checkSavingCriteriaSatisfied(result);
                boolean toSave = savingCriteriaSatisfied.size() > 0;

                if (toSave) {
                    // Add new valid structure which has increased coverage
                    exploredInputStructures.add(currentInput.primaryInput.hashCode());

                    String why = String.join(" ", savingCriteriaSatisfied);

                    // Trim input (remove unused keys)
                    currentInput.gc();

                    // Make sure it is not empty
                    assert(currentInput.primaryInput.size() > 0 || currentInput.secondaryInput.size() > 0);

                    // libFuzzerCompat stats are only displayed when they hit new coverage
                    if (LIBFUZZER_COMPAT_OUTPUT) {
                        displayStats(false);
                    }

                    /*
                    infoLog("Saving new input (at run %d): " +
                                    "input #%d " +
                                    "of size %d; " +
                                    "reason = %s",
                            numTrials,
                            savedInputs.size(),
                            currentInput.size(),
                            why);

                     */

                    // Save input to queue and to disk
                    final String reason = why;
                    GuidanceException.wrap(() -> saveCurrentInput(responsibilities, reason));

                    // Update coverage information
                    //updateCoverageFile();
                } else {
                    // Input has not been saved, transfer any remaining responsibilities to parent
                    if (responsibilities.size() > 0) {
                        // Transfer responsibilities to parent input
                        if (savedInputs.isEmpty()) {
                            throw new GuidanceException("Empty queue, cannot transfer responsibilities.");
                        }
                        savedInputs.get(currentParentInputIdx).responsibilities.addAll(responsibilities);
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

                    // Trim input (remove unused keys)
                    currentInput.gc();

                    // Make sure it is not empty
                    assert(currentInput.primaryInput.size() > 0 || currentInput.secondaryInput.size() > 0);


                    // Save crash to disk
                    try {
                        int crashIdx = uniqueFailures.size() - 1;

                        String primarySaveFileName = String.format("id_%06d", crashIdx);
                        File primarySaveFile = new File(savedFailuresDirectory, primarySaveFileName);
                        writeInputToFile(currentInput.primaryInput, primarySaveFile);

                        String secondarySaveFileName = primarySaveFileName + "_secondary";
                        File secondarySaveFile = new File(savedFailuresDirectory, secondarySaveFileName);
                        writeInputToFile(currentInput.secondaryInput, secondarySaveFile);

                        infoLog("%d %s %s", elapsedMilliseconds, primarySaveFileName, "Found crash: " + error.getClass() + " - " + (msg != null ? msg : ""));
                        String how = "unknown";
                        String why = result == Result.FAILURE ? "+crash" : "+hang";
                        //infoLog("Saved - %s %s %s", primarySaveFile.getPath(), how, why);

                        // Save stack trace to disk
                        File stackTraceFile = new File(failureStatsDirectory, primarySaveFileName + ".stacktrace");
                        appendLineToFile(stackTraceFile, error.getClass().toString());
                        for (StackTraceElement s: Arrays.asList(rootCause.getStackTrace())) {
                            appendLineToFile(stackTraceFile, s.toString());
                        }

                        File statsFile = new File(failureStatsDirectory, primarySaveFileName + ".stats");
                        appendLineToFile(statsFile, "Type: " + error.getClass());
                        appendLineToFile(statsFile, "TTD: " + elapsedMilliseconds);
                        appendLineToFile(statsFile, "HashCode: " + runCoverage.hashCode());
                        appendLineToFile(statsFile, "NonZeroHashCode: " + runCoverage.nonZeroHashCode());
                    } catch (IOException e) {
                        throw new GuidanceException(e);
                    }

                    // libFuzzerCompat stats are only displayed when they hit new coverage or crashes
                    if (LIBFUZZER_COMPAT_OUTPUT) {
                        displayStats(false);
                    }

                }
            }

            // Update overall stats
            totalExplorationCount += currentExplorationCount;
            totalExplorationScore += currentExplorationScore;
            totalExploitationCount += currentExploitationCount;
            totalExploitationScore += currentExploitationScore;

            // displaying stats on every interval is only enabled for AFL-like stats screen
            if (!LIBFUZZER_COMPAT_OUTPUT) {
                displayStats(false);
            }
        });
    }

    // Return a list of saving criteria that have been satisfied for a non-failure input
    @Override
    protected List<String> checkSavingCriteriaSatisfied(Result result) {
        // Total coverage before
        int nonZeroBefore = totalCoverage.getNonZeroCount();

        // Update total coverage
        boolean coverageBitsUpdated = totalCoverage.updateBits(runCoverage);

        // Coverage after
        int nonZeroAfter = totalCoverage.getNonZeroCount();
        if (nonZeroAfter > maxCoverage) {
            maxCoverage = nonZeroAfter;
        }

        // Invalid input, return
        if (result != Result.SUCCESS) {
            return new ArrayList<>();
        }

        // From here on, we can assume validity of the input

        // Stats before
        int uniqueValidsBefore = uniqueValidInputs.size();
        int validNonZeroBefore = validCoverage.getNonZeroCount();
        int uniqueValidPathsBefore = uniqueValidPaths.size();
        int uniqueBranchSetsBefore = uniqueBranchSets.size();

        // Update stats
        uniqueValidInputs.add(lastGeneratedInputsHash);
        uniqueValidPaths.add(runCoverage.hashCode());
        validCoverage.updateBits(runCoverage);
        uniqueBranchSets.add(runCoverage.nonZeroHashCode());

        // Stats after
        int uniqueValidsAfter = uniqueValidInputs.size();
        int validNonZeroAfter = validCoverage.getNonZeroCount();
        int uniqueValidPathsAfter = uniqueValidPaths.size();
        int uniqueBranchSetsAfter = uniqueBranchSets.size();


        /* Update reward based on last mutation */
        // Validity-focused reward
        if (REWARD_UNIQUE_VALIDITY && (uniqueValidsAfter > uniqueValidsBefore)) {
            switch (currentMutationPhase) {
                case EXPLORATION:
                    currentExplorationScore++;
                    break;
                case EXPLOITATION:
                    currentExploitationScore++;
                    break;
                default:
                    break;
            }
        }

        // Unique paths-focused reward
        if (REWARD_UNIQUE_VALID_PATHS && (uniqueValidPathsAfter > uniqueValidPathsBefore)) {
            switch (currentMutationPhase) {
                case EXPLORATION:
                    currentExplorationScore++;
                    break;
                case EXPLOITATION:
                    currentExploitationScore++;
                    break;
                default:
                    break;
            }
        }

        // Coverage-focused reward
        if (REWARD_NEW_COVERAGE && (nonZeroAfter > nonZeroBefore)) {
            switch (currentMutationPhase) {
                case EXPLORATION:
                    currentExplorationScore++;
                    break;
                case EXPLOITATION:
                    currentExploitationScore++;
                    break;
                default:
                    break;
            }
        }

        // Possibly save input
        List<String> reasonsToSave = new ArrayList<>();

        boolean isNewStructure = !exploredInputStructures.contains(((LinearInput) currentInput.primaryInput).hashCode());
        boolean newValidCoverage = validNonZeroAfter > validNonZeroBefore;

        // Optionally
        if (SAVE_ONLY_NEW_STRUCTURES && !isNewStructure) {
            // Do not save previously explored (old) structures
            return reasonsToSave;
        } else if (SAVE_OLD_STRUCTURE_IF_VALID_COV && !newValidCoverage) {
            // Do not save old structure if no new valid coverage
            return reasonsToSave;
        }

        // Save new counts (if enabled)
        if (!DISABLE_SAVE_NEW_COUNTS && coverageBitsUpdated) {
            reasonsToSave.add("+count");
        }

        // Always save if new valid coverage is found
        if (this.validityFuzzing && newValidCoverage) {
            currentInput.valid = true;
            reasonsToSave.add("+valid");
        }

        return reasonsToSave;

    }

    @Override
    protected void saveCurrentInput(IntHashSet responsibilities, String why) throws IOException {

        // First, save to disk (note: we issue IDs to everyone, but only write to disk  if valid)
        int newInputIdx = numSavedInputs++;
        String primarySaveFileName = String.format("id_%06d", newInputIdx);
        File primarySaveFile = new File(savedCorpusDirectory, primarySaveFileName);

        String secondarySaveFileName = primarySaveFileName + "_secondary";
        File secondarySaveFile = new File(savedCorpusDirectory, secondarySaveFileName);

        String how = currentInput.desc;

        writeInputToFile(currentInput.primaryInput,primarySaveFile);
        writeInputToFile(currentInput.secondaryInput,secondarySaveFile);
        //infoLog("Saved - %s %s %s", primarySaveFile.getPath(), how, why);

        // If not using guidance, do nothing else
        if (blind) {
            return;
        }

        // Second, save to queue
        savedInputs.add(currentInput);

        // Third, store basic book-keeping data
        currentInput.id = newInputIdx;
        currentInput.primarySaveFile = primarySaveFile;
        currentInput.secondarySaveFile = secondarySaveFile;
        currentInput.coverage = runCoverage.copy();
        currentInput.nonZeroCoverage = runCoverage.getNonZeroCount();
        currentInput.offspring = 0;
        savedInputs.get(currentParentInputIdx).offspring += 1;

        // Fourth, assume responsibility for branches
        currentInput.responsibilities = responsibilities;
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
}


