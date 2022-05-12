package de.hub.se.jqf.fuzz.div;

import edu.berkeley.cs.jqf.fuzz.util.*;
import org.eclipse.collections.api.iterator.IntIterator;
import org.eclipse.collections.api.list.primitive.IntList;

import java.util.*;

public class DivMetricsCounter {
    /** The last time since the metrics have been computed (to reduce overhead). */
    private Date lastRefreshTime;

    /** The stat refresh period in ms */
    protected final long STATS_REFRESH_TIME_PERIOD = 5000; // Every 5 seconds

    /** The size of the coverage map. */
    private final int COVERAGE_MAP_SIZE = (1 << 8); // Map size taken from FastNonCollidingCoverage

    /** The coverage counts for each edge. */
    private final FastNonCollidingCounter counter = new FastNonCollidingCounter(COVERAGE_MAP_SIZE);

    /** Cached values for diversity indices. */
    // H_0, H_1, H_2
    private double[] cachedMetrics;

    public DivMetricsCounter() {
        lastRefreshTime = new Date();
        cachedMetrics = new double[]{0, 0, 0};
    }

    public void incrementBranchCounts(ICoverage runCoverage) {
        IntList coveredBranches = runCoverage.getCovered();
        IntIterator it = coveredBranches.intIterator();
        while (it.hasNext()) {
            int idx = it.next();
            counter.increment(idx);
        }
    }

    public double[] getCachedMetrics(Date now) {
        // Update cached values once every while
        if (now.getTime() - lastRefreshTime.getTime() >= STATS_REFRESH_TIME_PERIOD) {
            updateMetrics();
            lastRefreshTime = now;
        }
        return cachedMetrics;
    }

    public void updateMetrics() {
        IntList coveredBranches = counter.getNonZeroIndices();
        int totalBranchHitCount = counter.getNonZeroValues().primitiveStream().sum();

        double shannon = 0;
        double h_2 = 0;

        IntIterator it = coveredBranches.intIterator();
        while (it.hasNext()) {
            int idx = it.next();
            int hit_count = counter.get(idx);

            double  p_i = ((double) hit_count) / totalBranchHitCount;
            shannon += p_i * Math.log(p_i);
            h_2 += Math.pow(p_i, 2);

        }
        cachedMetrics[0] = counter.getNonZeroSize(); //Math.pow(h_0, 1): Hill-Number of order 0
        cachedMetrics[1] = Math.exp(-shannon); // Hill-number of order 1 (= exp(shannon_index))
        cachedMetrics[2] = Math.pow(h_2, -1); // Hill-number of order 2
    }
}
