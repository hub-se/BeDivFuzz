package de.hub.se.jqf.bedivfuzz.guidance.repro;

import de.hub.se.jqf.bedivfuzz.guidance.SplitParameterStream;
import de.hub.se.jqf.bedivfuzz.guidance.BeDivGuidance;
import edu.berkeley.cs.jqf.fuzz.guidance.GuidanceException;
import edu.berkeley.cs.jqf.fuzz.guidance.Result;
import edu.berkeley.cs.jqf.fuzz.repro.ReproGuidance;
import edu.berkeley.cs.jqf.fuzz.util.Coverage;
import edu.berkeley.cs.jqf.instrument.tracing.events.TraceEvent;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Test case reproduction for test inputs produced by BeDivFuzz.
 */

public class BeDivReproGuidance extends ReproGuidance implements BeDivGuidance {
    private SplitReproParameterStream splitInputStream;

    public BeDivReproGuidance(File[] inputFiles, File traceDir) throws IOException {
        super(inputFiles, traceDir);

        for (File inputFile : inputFiles) {
            if (!inputFile.isDirectory()) {
                throw new GuidanceException("Not an input directory: " + inputFile.getAbsolutePath());
            } else if (!new File(inputFile, "structural_parameters").exists()){
                throw new GuidanceException("Input directory is missing structural_parameters file.");
            } else if (!new File(inputFile, "value_parameters").exists()){
                throw new GuidanceException("Input directory is missing value_parameters file.");
            }
        }
    }

    @Override
    public InputStream getInput() {
        throw new GuidanceException("BeDivFuzz only produces SplitInputs.");
    }

    @Override
    public SplitParameterStream getSplitInput() throws GuidanceException {
        try {
            File inputFileDir = inputFiles[nextFileIdx];
            File structuralParameterFile = new File(inputFileDir, "structural_parameters");
            File valueParameterFile = new File(inputFileDir, "value_parameters");

            this.splitInputStream = new SplitReproParameterStream(structuralParameterFile, valueParameterFile);

            if (allBranchesCovered != null) {
                branchesCoveredInCurrentRun.clear();
            }

            return this.splitInputStream;
        } catch (IOException e) {
            throw new GuidanceException(e);
        }
    }

    @Override
    public void handleResult(Result result, Throwable error) {
        // Close the open input
        try {
            if (splitInputStream != null) {
                splitInputStream.close();
            }
        } catch (IOException e) {
            throw new GuidanceException(e);
        }

        // Then do same stuff as regular ReproGuidance
        super.handleResult(result, error);
    }

}


