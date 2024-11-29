package edu.berkeley.cs.jqf.fuzz.ei;

import de.hub.se.jqf.bedivfuzz.guidance.split.GuidanceState;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.Random;

public class ZestSplitGuidance extends ZestGuidance {

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
    public ZestSplitGuidance(String testName, Duration duration, File outputDirectory) throws IOException {
        super(testName, duration, null, outputDirectory, new Random());
    }

    public GuidanceState exportState() {
        GuidanceState state = new GuidanceState();
        state.numSavedInputs = numSavedInputs;
        state.numTrials = numTrials;
        state.numValid = numValid;
        state.cyclesCompleted = cyclesCompleted;
        state.totalCoverage = totalCoverage;
        state.validCoverage = validCoverage;
        state.semanticTotalCoverage = semanticTotalCoverage;
        state.uniquePaths = uniquePaths;
        state.uniqueValidPaths = uniqueValidPaths;
        state.branchHitCounter = branchHitCounter;
        state.maxCoverage = maxCoverage;
        state.uniqueFailures = uniqueFailures;
        return state;
    }
}
