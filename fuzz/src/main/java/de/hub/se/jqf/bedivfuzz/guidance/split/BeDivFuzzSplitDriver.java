package de.hub.se.jqf.bedivfuzz.guidance.split;

import java.io.File;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Random;

import edu.berkeley.cs.jqf.fuzz.ei.ZestGuidance;
import edu.berkeley.cs.jqf.fuzz.ei.ZestSplitGuidance;
import edu.berkeley.cs.jqf.fuzz.guidance.GuidanceException;
import edu.berkeley.cs.jqf.fuzz.junit.GuidedFuzzing;
import edu.berkeley.cs.jqf.fuzz.util.IOUtils;
import org.junit.runner.Result;

/**
 * Entry point for fuzzing with BeDivGuidance. Adapted from {@link ZestGuidance}
 *
 */
public class BeDivFuzzSplitDriver {

    public static void main(String[] args) {
        if (args.length < 2){
            System.err.println("Usage: java " + BeDivFuzzSplitDriver.class + " TEST_CLASS TEST_METHOD [OUTPUT_DIR]");
            System.exit(1);
        }

        String testClassName  = args[0];
        String testMethodName = args[1];
        String outputDirectoryName = args.length > 2 ? args[2] : "fuzz-results";
        File outputDirectory = new File(outputDirectoryName);

        Locale.setDefault(Locale.US);

        // For BeDivFuzz, set havoc rate to 0
        // Parse duration to millis and allocate half to Zest
        // Make Zest serialize the hit-counts at the end of the "half-campaign"
        try {
            String campaignTimeout = System.getProperty("jqf.guidance.campaign_timeout");
            Duration duration = null;
            if (campaignTimeout != null && !campaignTimeout.isEmpty()) {
                try {
                    duration = Duration.parse("PT"+campaignTimeout);
                } catch (DateTimeParseException e) {
                    throw new GuidanceException("Invalid time duration: " + campaignTimeout);
                }
            } else {
                throw new GuidanceException("BeDivFuzz-Split requires campaign timeout (-T) parameter");
            }

            // Load the guidance
            String title = testClassName+"#"+testMethodName;
            File zestOutputDirectory = IOUtils.createDirectory(outputDirectory, "zest-results");
            Random rnd = new Random(); // TODO: Support deterministic PRNG
            Duration halfDuration = Duration.ofMillis(duration.toMillis() / 2);
            ZestSplitGuidance zestGuidance = new ZestSplitGuidance(title, halfDuration,  zestOutputDirectory);
            Result zestResult = GuidedFuzzing.run(testClassName, testMethodName, zestGuidance, System.out);

            File zestCorpus = new File(zestOutputDirectory, "corpus");
            System.setProperty("jqf.guidance.bedivfuzz.havoc_rate", "0.0");
            BeDivFuzzSplitGuidance bedivFuzzGuidance = new BeDivFuzzSplitGuidance(
                    testClassName+"#testWithSplitGenerator", halfDuration, null, outputDirectory, zestCorpus, rnd, zestGuidance.exportState()
            );
            Result bedivfuzzResult = GuidedFuzzing.run(testClassName, "testWithSplitGenerator", bedivFuzzGuidance, System.out);

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(2);
        }

    }
}


