package de.hub.se.jqf.fuzz.repro;

import de.hub.se.jqf.fuzz.div.SplitInput;
import de.hub.se.jqf.fuzz.guidance.BeDivGuidance;
import edu.berkeley.cs.jqf.fuzz.guidance.GuidanceException;
import edu.berkeley.cs.jqf.fuzz.guidance.Result;
import edu.berkeley.cs.jqf.fuzz.repro.ReproGuidance;
import edu.berkeley.cs.jqf.fuzz.util.Coverage;
import edu.berkeley.cs.jqf.instrument.tracing.events.TraceEvent;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * DivReproGuidance implementation based on {@link ReproGuidance}.
 */
public class BeDivReproGuidance implements BeDivGuidance {
    private File[] primaryInputFiles;
    private final File traceDir;
    private int nextFileIdx = 0;

    private List<PrintStream> traceStreams = new ArrayList<>();
    private SplitReproInput currentInput;
    private Coverage coverage = new Coverage();

    public BeDivReproGuidance(File[] inputFiles, File traceDir) {
        this.traceDir = traceDir;
        this.primaryInputFiles = inputFiles;
        int numPrimaryInputs = 0;
        int numSecondaryInputs = 0;
        for (File file : inputFiles) {
            File secondaryInput = new File(file.getAbsolutePath() + "_secondary");
            if (!secondaryInput.isFile()) {
                throw new GuidanceException("No secondary input found for file: " + file.getName());
            }
        }
        assert(numPrimaryInputs == numSecondaryInputs);
    }

    public BeDivReproGuidance(File inputFile, File traceDir) {
        assert(!inputFile.getName().endsWith("_secondary")): "BeDivReproGuidance must be run with primary input.";
        File secondaryInput = new File(inputFile.getAbsolutePath() + "_secondary");
        if (!secondaryInput.isFile()) {
            throw new GuidanceException("No secondary input found for file: " + inputFile.getName());
        }
        this.primaryInputFiles = new File[]{inputFile};
        this.traceDir = traceDir;
    }

    private File getCurrentInputFile() {
        return primaryInputFiles[nextFileIdx];
    }


    @Override
    public InputStream getInput() {
        throw new UnsupportedOperationException("BeDivReproGuidance does not support getInput(), use getInput() instead.");
    }

    public SplitInput getSplitInput() throws GuidanceException {
        File primaryRandomFile = primaryInputFiles[nextFileIdx];
        File secondaryRandomFile = new File(primaryRandomFile.getAbsolutePath() + "_secondary");
        currentInput = new SplitReproInput(primaryRandomFile, secondaryRandomFile);

        return currentInput;
    }

    @Override
    public boolean hasInput() {
        return nextFileIdx < primaryInputFiles.length;
    }

    @Override
    public void handleResult(Result result, Throwable error) {
        // Close the open input
        currentInput.gc();

        // Print result
        File inputFile = getCurrentInputFile();

        if (result == Result.FAILURE) {
            System.out.printf("%s: %s (%s)\n", inputFile.getName(), result, error.getClass().getName());
        } else {
            System.out.printf("%s: %s\n", inputFile.getName(), result);
        }

        // Maybe add to results csv
        if (traceDir != null) {
            File resultsCsv = new File(traceDir, "results.csv");
            boolean append = nextFileIdx > 0; // append for all but the first input
            try (PrintStream out = new PrintStream(new FileOutputStream(resultsCsv, append))) {
                String inputName = getCurrentInputFile().toString();
                String exception = result == Result.FAILURE ? error.getClass().getName() : "";
                out.printf("%s,%s,%s\n", inputName, result, exception);
            } catch (IOException e) {
                throw new GuidanceException(e);
            }
        }

        // Increment file
        nextFileIdx++;
    }

    @Override
    public Consumer<TraceEvent> generateCallBack(Thread thread) {
        if (traceDir != null) {
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

    public Coverage getCoverage() {
        return coverage;
    }

}


