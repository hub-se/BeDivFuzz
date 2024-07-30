package de.hub.se.jqf.bedivfuzz.util;

import edu.berkeley.cs.jqf.fuzz.util.*;
import org.eclipse.collections.api.iterator.IntIterator;
import org.eclipse.collections.impl.map.mutable.primitive.IntIntHashMap;

import java.util.*;

/**
 * Utility class to manage branch hit-counts and behavioral diversity metrics.
 */
public class BranchHitCounter {
    /** The last time since the metrics have been computed (to reduce overhead). */
    private Date lastRefreshTime = new Date();

    /** The stat refresh period in ms */
    private final long STATS_REFRESH_TIME_PERIOD = 5000; // Every 5 seconds

    /** The size of the coverage map. */
    private final int COVERAGE_MAP_SIZE = (1 << 8);

    /** The coverage counts for each edge. */
    private final FastNonCollidingCounter counter = new FastNonCollidingCounter(COVERAGE_MAP_SIZE);

    /** The most recently computed behavioral diversity metrics. */
    private final BehavioralDiversityMetrics bedivMetrics = new BehavioralDiversityMetrics();

    /**
     * Creates a new BeDivMetricsCounter instance.
     */
    public BranchHitCounter() {}

    /**
     * Creates a new BeDivMetricsCounter instance with a prepopulated branch hit count map.
     */
    public BranchHitCounter(IntIntHashMap branchHitCounts) {
        branchHitCounts.forEachKeyValue(counter::increment);
    }

    /**
     * Updates the global branch hit counts with the hit counts from the current input
     * @param runCoverage the coverage of the current input
     */
    public void incrementBranchCounts(ICoverage runCoverage) {
        runCoverage.getCovered().primitiveStream()
                .forEach(counter::increment);
    }

    /**
     * Returns the most recently computed behavioral diversity metrics, which are updated periodically
     * @param force force updating stats
     * @return the most recently computed behavioral diversity metrics
     */
    public BehavioralDiversityMetrics getCachedMetrics(boolean force) {
        // Update cached values once every while
        Date now = new Date();
        if (force || (now.getTime() - lastRefreshTime.getTime() >= STATS_REFRESH_TIME_PERIOD)) {
            updateMetrics();
            lastRefreshTime = now;
        }
        return bedivMetrics;
    }

    /**
     * Updates the behavioral diversity metrics based on the current branch hit count distribution.
     */
    public void updateMetrics() {
        long totalBranchHitCount = counter.getNonZeroValues().sum();
        double logN = Math.log(totalBranchHitCount);

        double b1_basicSum = 0;
        double b2_basicSum = 0;

        IntIterator it = counter.getNonZeroValues().intIterator();
        while (it.hasNext()) {
            int hitcount = it.next();

            b1_basicSum += hitcount * Math.log(hitcount);
            b2_basicSum += Math.exp(2.0 * (Math.log(hitcount) - logN));

        }
        bedivMetrics.b0 = counter.getNonZeroSize(); //Math.pow(h_0, 1): Hill-Number of order 0

        double entropy = Math.log(totalBranchHitCount) - 1.0/totalBranchHitCount * b1_basicSum;
        bedivMetrics.b1 = Math.exp(entropy); // Hill-number of order 1 (= exp(shannon index))

        double logSumExp = Math.log(b2_basicSum);
        bedivMetrics.b2 = Math.exp(-logSumExp);
    }

    /**
     * Returns the map of branch hit counts (for serialization).
     * @return map of branch hit counts.
     */
    public IntIntHashMap getHitCounts() {
        return counter.getCounts();
    }

}
