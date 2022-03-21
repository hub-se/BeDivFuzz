package de.hub.se.jqf.fuzz.div;

import java.io.File;
import java.util.Locale;
import java.util.Random;

import edu.berkeley.cs.jqf.fuzz.ei.ZestGuidance;
import edu.berkeley.cs.jqf.fuzz.junit.GuidedFuzzing;
import org.junit.runner.Result;

/**
 * Entry point for fuzzing with DivGuidance. Adapted from {@link ZestGuidance}
 *
 * @author Hoang Lam Nguyen
 */
public class BeDivDriver {

    public static void main(String[] args) {
        if (args.length < 2){
            System.err.println("Usage: java " + BeDivDriver.class + " TEST_CLASS TEST_METHOD [OUTPUT_DIR]");
            System.exit(1);
        }

        String testClassName  = args[0];
        String testMethodName = args[1];
        String outputDirectoryName = args.length > 2 ? args[2] : "fuzz-results";
        File outputDirectory = new File(outputDirectoryName);
        if (args.length > 3) {
            System.err.println("Seed files are not supported.");
            System.exit(1);
        }

        try {
            // Load the guidance
            String title = testClassName+"#"+testMethodName;
            Random rnd = new Random(); // TODO: Support deterministic PRNG
            BeDivFuzzGuidance guidance = new BeDivFuzzGuidance(title, null, null, outputDirectory, rnd);

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

