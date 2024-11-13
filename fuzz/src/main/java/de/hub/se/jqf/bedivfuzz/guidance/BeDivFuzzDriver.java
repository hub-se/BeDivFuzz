package de.hub.se.jqf.bedivfuzz.guidance;

import java.io.File;
import java.util.Locale;
import java.util.Random;

import edu.berkeley.cs.jqf.fuzz.ei.ZestGuidance;
import edu.berkeley.cs.jqf.fuzz.junit.GuidedFuzzing;
import org.junit.runner.Result;

/**
 * Entry point for fuzzing with BeDivGuidance. Adapted from {@link ZestGuidance}
 *
 */
public class BeDivFuzzDriver {

    public static void main(String[] args) {
        if (args.length < 2){
            System.err.println("Usage: java " + BeDivFuzzDriver.class + " TEST_CLASS TEST_METHOD [OUTPUT_DIR]");
            System.exit(1);
        }

        String testClassName  = args[0];
        String testMethodName = args[1];
        String outputDirectoryName = args.length > 2 ? args[2] : "fuzz-results";
        File outputDirectory = new File(outputDirectoryName);
        File[] seedFiles = null;
        if (args.length > 3) {
            seedFiles = new File[args.length-3];
            for (int i = 3; i < args.length; i++) {
                seedFiles[i-3] = new File(args[i]);
            }
        }

        try {
            // Load the guidance
            String title = testClassName+"#"+testMethodName;
            Random rnd = new Random(); // TODO: Support deterministic PRNG
            BeDivFuzzGuidance guidance = new BeDivFuzzGuidance(title, null, null, outputDirectory, rnd);

            if (seedFiles == null) {
                guidance = new BeDivFuzzGuidance(title, null, null, outputDirectory, rnd);
            } else if (seedFiles.length == 1 && seedFiles[0].isDirectory()) {
                guidance = new BeDivFuzzGuidance(title, null, null, outputDirectory, seedFiles[0], rnd);
            } else {
                guidance = new BeDivFuzzGuidance(title, null, null, outputDirectory, seedFiles, rnd);
            }

            Locale.setDefault(Locale.US);

            // Run the Junit test
            Result res = GuidedFuzzing.run(testClassName, testMethodName, guidance, System.out);
            if (Boolean.getBoolean("jqf.logCoverage")) {
                System.out.println(String.format("Covered %d edges.",
                        guidance.getTotalCoverage().getNonZeroCount()));
            }
            if (Boolean.getBoolean("jqf.ei.EXIT_ON_CRASH") && !res.wasSuccessful()) {
                System.exit(3);
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(2);
        }

    }
}

