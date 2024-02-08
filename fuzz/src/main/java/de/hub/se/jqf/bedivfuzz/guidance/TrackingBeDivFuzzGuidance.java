package de.hub.se.jqf.bedivfuzz.guidance;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import de.hub.se.jqf.bedivfuzz.junit.quickcheck.tracking.SplitTrackingSourceOfRandomness;
import de.hub.se.jqf.bedivfuzz.util.IntPair;
import edu.berkeley.cs.jqf.fuzz.ei.ZestGuidance;
import edu.berkeley.cs.jqf.fuzz.guidance.GuidanceException;
import edu.berkeley.cs.jqf.fuzz.junit.quickcheck.NonTrackingGenerationStatus;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;

public class TrackingBeDivFuzzGuidance extends ZestGuidance implements BeDivGuidance {

    private BiConsumer<SplitTrackingSourceOfRandomness, GenerationStatus> choiceTracer;
    private TrackingInput trackingInput;
    private int numUniquePathsBefore = 0;
    private int numInputsUnfavored = 0;

    public TrackingBeDivFuzzGuidance(String testName, Duration duration, File outputDirectory) throws IOException {
        this(testName, duration, null, outputDirectory, new Random());
    }

    public TrackingBeDivFuzzGuidance(String testName, Duration duration, Long trials, File outputDirectory, Random sourceOfRandomness) throws IOException {
        super(testName, duration, trials, outputDirectory, sourceOfRandomness);
    }

    public void registerChoiceTracer(BiConsumer<SplitTrackingSourceOfRandomness, GenerationStatus> tracer) {
        this.choiceTracer = tracer;
    }

    @Override
    protected String getTitle() {
        return "TrackingBeDivFuzzGuidance";
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
        if (console != null) {
            console.printf("Number of unfavored inputs: %,d\n", numInputsUnfavored);
        }
    }

    @Override
    public InputStream getInput() throws GuidanceException {
        if (!savedInputs.isEmpty()) {
            // The number of children to produce is determined by how much of the coverage
            // pool this parent input hits
            Input currentParentInput = savedInputs.get(currentParentInputIdx);
            int targetNumChildren = getTargetChildrenForParent(currentParentInput);
            if (numChildrenGeneratedForCurrentParentInput >= targetNumChildren) {
                int numUniquePathsAfter = uniqueValidPaths.size();

                // Unfavor inputs that do not produce new paths
                if (currentParentInput.isFavored() && numUniquePathsAfter <= numUniquePathsBefore) {
                    currentParentInput.setFavored(false);
                    numInputsUnfavored++;
                }

                numUniquePathsBefore = numUniquePathsAfter;
            }
        }
        return super.getInput();
    }

    @Override
    protected void saveCurrentInput(IntHashSet responsibilities, String why) throws IOException {
        trackingInput = new TrackingInput((LinearInput) currentInput);

        SplitTrackingSourceOfRandomness random = new SplitTrackingSourceOfRandomness(
                createTrackingParameterStream(),
                trackingInput.structureChoices,
                trackingInput.valueChoices
        );

        GenerationStatus genStatus = new NonTrackingGenerationStatus(random.getStructureDelegate());
        choiceTracer.accept(random, genStatus);

        super.saveCurrentInput(responsibilities, why);
    }

    protected InputStream createTrackingParameterStream() {
        // Return an input stream that reads bytes from a linear array
        return new InputStream() {
            int bytesRead = 0;

            @Override
            public int read() throws IOException {
                TrackingInput input = trackingInput;
                // Attempt to get a value from the list, or else generate a random value
                int ret = input.getOrGenerateFresh(bytesRead++, random);
                // infoLog("read(%d) = %d", bytesRead, ret);
                return ret;
            }
        };
    }

    @Override
    public SplitParameterStream getSplitInput() throws GuidanceException {
        throw new GuidanceException("TrackingBeDivFuzzGuidance produces LinearInputs.");
    }

    public class TrackingInput extends LinearInput {
        private final List<IntPair> structureChoices = new ArrayList<>();
        private final List<IntPair> valueChoices = new ArrayList<>();

        public TrackingInput(LinearInput baseInput) {
            super(baseInput);
        }
    }

}