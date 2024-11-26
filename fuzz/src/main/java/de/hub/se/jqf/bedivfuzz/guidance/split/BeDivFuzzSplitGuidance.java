package de.hub.se.jqf.bedivfuzz.guidance.split;

import de.hub.se.jqf.bedivfuzz.guidance.BeDivFuzzGuidance;
import edu.berkeley.cs.jqf.fuzz.guidance.Result;
import edu.berkeley.cs.jqf.fuzz.util.IOUtils;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Random;

public class BeDivFuzzSplitGuidance extends BeDivFuzzGuidance {

    private final int numSeedInputs;

    public BeDivFuzzSplitGuidance(String testName, Duration duration, Long trials, File outputDirectory, File seedInputDir, Random sourceOfRandomness, GuidanceState state) throws IOException {
        super(testName, duration, trials, outputDirectory, IOUtils.resolveInputFileOrDirectory(seedInputDir), sourceOfRandomness);
        this.numSeedInputs = state.numSavedInputs;
        this.numTrials = state.numTrials;
        this.numValid = state.numValid;
        this.lastNumTrials = state.numTrials;
        this.cyclesCompleted = state.cyclesCompleted;
        this.totalCoverage = state.totalCoverage;
        this.validCoverage = state.validCoverage;
        this.semanticTotalCoverage = state.semanticTotalCoverage;
        this.uniquePaths = state.uniquePaths;
        this.uniqueValidPaths = state.uniqueValidPaths;
        this.branchHitCounter = state.branchHitCounter;
        this.maxCoverage = state.maxCoverage;
        this.uniqueFailures = state.uniqueFailures;
    }

    @Override
    protected List<String> checkSavingCriteriaSatisfied(Result result) {
       List<String> reasons =  super.checkSavingCriteriaSatisfied(result);
       if (numSavedInputs < numSeedInputs) {
           reasons.add("seed");
       }
       return reasons;
    }

    @Override
    protected void completeCycle() {
        // TODO
        cyclesCompleted++;
    }

}
