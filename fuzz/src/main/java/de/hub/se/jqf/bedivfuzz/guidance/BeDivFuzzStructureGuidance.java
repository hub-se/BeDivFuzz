package de.hub.se.jqf.bedivfuzz.guidance;

import edu.berkeley.cs.jqf.fuzz.guidance.GuidanceException;
import org.eclipse.collections.api.list.primitive.MutableIntList;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;
import org.eclipse.collections.impl.map.mutable.primitive.IntIntHashMap;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class BeDivFuzzStructureGuidance extends BeDivFuzzGuidance {

    /** Saved input idx, grouped by input structure. */
    protected List<MutableIntList> inputStructureQueue = new ArrayList<>();

    /** Mapping from input structure hashes to index in inputStructureQueue. */
    protected IntIntHashMap inputStructureToIndex = new IntIntHashMap();

    /** The current index in the input structure queue. */
    protected int currentInputStructureIdx = 0;

    public BeDivFuzzStructureGuidance(String testName, Duration duration, Long trials, File outputDirectory, Random sourceOfRandomness) throws IOException {
        super(testName, duration, trials, outputDirectory, sourceOfRandomness);
    }

    @Override
    protected void displayStats(boolean force) {
        Date now = new Date();
        long intervalMilliseconds = now.getTime() - lastRefreshTime.getTime();
        intervalMilliseconds = Math.max(1, intervalMilliseconds);
        if (intervalMilliseconds < STATS_REFRESH_TIME_PERIOD && !force) {
            return;
        }
        super.displayStats(force);
        if (console != null && !QUIET_MODE) {
            console.printf("  Input structures:   %,d\n", inputStructureQueue.size());
        }
    }

    @Override
    public InputStream getInput() throws GuidanceException {
        conditionallySynchronize(multiThreaded, () -> {
            // Clear coverage stats for this run
            runCoverage.clear();
            if (TRACK_SEMANTIC_COVERAGE) semanticRunCoverage.clear();

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
                Input currentParentInput = savedInputs.get(currentParentInputIdx);
                int targetNumChildren = getTargetChildrenForParent(currentParentInput);
                if (numChildrenGeneratedForCurrentParentInput >= targetNumChildren) {
                    // Select the next input structure to fuzz
                    currentInputStructureIdx = (currentInputStructureIdx + 1) % inputStructureQueue.size();

                    // Select one of the concrete inputs, giving higher weight to inputs saved later
                    MutableIntList inputIndices = inputStructureQueue.get(currentInputStructureIdx);
                    int numInputs = inputIndices.size();
                    if (numInputs == 1) {
                        currentParentInputIdx = inputIndices.get(0);
                    } else {
                        // Inputs are assigned weights 1,2,...,N
                        int cumulativeWeights = numInputs * (numInputs + 1) / 2;
                        double rnd = random.nextDouble() * cumulativeWeights;

                        for (int weight = numInputs; weight >= 1; weight--) {
                            rnd -= weight;
                            if (rnd < 0) {
                                currentParentInputIdx = inputIndices.get(weight - 1);
                            }
                        }
                    }

                    currentParentInputIdx = inputIndices.get(random.nextInt(inputIndices.size()));

                    // Count cycles
                    if (currentInputStructureIdx == 0) {
                        completeCycle();
                    }

                    numChildrenGeneratedForCurrentParentInput = 0;
                }
                Input parent = savedInputs.get(currentParentInputIdx);

                // Fuzz it to get a new input
                // infoLog("Mutating input: %s", parent.desc);
                currentInput = parent.fuzz(random);
                numChildrenGeneratedForCurrentParentInput++;

                // Write it to disk for debugging
                try {
                    writeCurrentInputToFile(currentInputFile);
                } catch (IOException ignore) {
                }

                // Start time-counting for timeout handling
                this.runStart = new Date();
                this.branchCount = 0;
            }
        });

        return createParameterStream();
    }

    @Override
    protected void saveCurrentInput(IntHashSet responsibilities, String why) throws IOException {
        // Save tracking input
        super.saveCurrentInput(responsibilities, why);

        // If we see this input structure for the first time, add new sub-list
        int structuralHash = ((TrackingInput) currentInput).structuralHashCode();
        if (!inputStructureToIndex.containsKey(structuralHash)) {
            inputStructureToIndex.put(structuralHash, inputStructureQueue.size());
            inputStructureQueue.add(new IntArrayList());
        }

        inputStructureQueue.get(inputStructureToIndex.get(structuralHash)).add(numSavedInputs - 1);
    }
}
