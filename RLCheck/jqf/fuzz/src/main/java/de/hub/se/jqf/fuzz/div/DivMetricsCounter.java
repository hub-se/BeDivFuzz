package de.hub.se.jqf.fuzz.div;

import edu.berkeley.cs.jqf.fuzz.util.Counter;
import edu.berkeley.cs.jqf.fuzz.util.Coverage;
import edu.berkeley.cs.jqf.fuzz.util.NonZeroCachingCounter;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class DivMetricsCounter {
    /** The last time since the metrics have been computed (to reduce overhead). */
    private Date lastRefreshTime;

    /** The stat refresh period in ms */
    protected final long STATS_REFRESH_TIME_PERIOD = 5000; // Every 5 seconds

    /** The size of the coverage map. */
    private final int COVERAGE_MAP_SIZE = (1 << 16) - 1; // Minus one to reduce collisions

    /** The coverage counts for each edge. */
    private final Counter counter = new NonZeroCachingCounter(COVERAGE_MAP_SIZE);

    /** Total number of branches executed*/
    private long totalBranchHitCount;

    /** Total number of valid executions */
    private long numExecutions;

    /** Cached values for diversity indices. */
    // H_0, H_1, H_2
    private double[] cachedMetrics;

    public DivMetricsCounter() {
        lastRefreshTime = new Date();
        totalBranchHitCount = 0;
        numExecutions = 0;
        cachedMetrics = new double[]{0, 0, 0};
    }

    public void incrementBranchCounts(Coverage runCoverage) {
        // First fix the covered branches
        Set<Integer> covered = new HashSet<>(runCoverage.getCovered());
        for (Integer idx : covered) {
            counter.increment(idx);
            totalBranchHitCount++;
        }
        numExecutions += 1;
    }

    public double[] getCachedMetrics(Date now) {

        // Update cached values every while
        if (now.getTime() - lastRefreshTime.getTime() >= STATS_REFRESH_TIME_PERIOD) {
            updateMetrics();
            lastRefreshTime = now;
        }
        return cachedMetrics;
    }

    public void updateMetrics() {
        HashSet<Integer> coveredBranches = new HashSet<>(counter.getNonZeroIndices());
        double simpson = 0;
        double shannon = 0;
        double h_0 = 0;
        double h_2 = 0;
        double singleton_branches = 0;

        for (Integer idx : coveredBranches) {
            int hit_count = counter.getAtIndex(idx);

            /*
            if (hit_count == 1) {
                singleton_branches += 1;
            }

            simpson += ((double)(hit_count * (hit_count-1))) /
                    (totalBranchHitCount * (totalBranchHitCount - 1));

             */

            double  p_i = ((double) hit_count) / totalBranchHitCount;
            shannon += p_i * Math.log(p_i);

            h_0 += Math.pow(p_i, 0);
            h_2 += Math.pow(p_i, 2);

        }
        cachedMetrics[0] = Math.pow(h_0, 1); // Hill-Number of order 0
        cachedMetrics[1] = Math.exp(-shannon); // Hill-number of order 1 (= exp(shannon_index))
        cachedMetrics[2] = Math.pow(h_2, 1/(1-2)); // Hill-number of order 2

        /*
        assert(cachedMetrics[0] == simpsonIndex()) : "expected: " + simpsonIndex() + " but was: " + cachedMetrics[0];
        assert(cachedMetrics[1] == shannonIndex()) : "expected: " + shannonIndex() + " but was: " + cachedMetrics[1];
        assert(cachedMetrics[2] == hill_number(0)) : "expected: " + hill_number(0) + " but was: " + cachedMetrics[2];
        assert(cachedMetrics[3] == hill_number(1)) : "expected: " + hill_number(1) + " but was: " + cachedMetrics[3];
        assert(cachedMetrics[4] == hill_number(2)) : "expected: " + hill_number(2) + " but was: " + cachedMetrics[4];
         */
    }

    public double simpsonIndex() {
        double sum = 0;
        HashSet<Integer> coveredBranches = new HashSet<>(counter.getNonZeroIndices());
        for (Integer idx : coveredBranches) {
            sum += ((double)(counter.getAtIndex(idx) * (counter.getAtIndex(idx)-1))) /
                    (totalBranchHitCount * (totalBranchHitCount - 1));
        }
        return 1 - sum;
    }

    public double shannonIndex() {
        double sum = 0;
        HashSet<Integer> coveredBranches = new HashSet<>(counter.getNonZeroIndices());
        for (Integer idx : coveredBranches) {
            double  p_i = ((double)counter.getAtIndex(idx))/totalBranchHitCount;
            sum += p_i * Math.log(p_i);
        }
        return - sum;
    }

    public double hill_number(int order) {
        if (order == 1) {
            return Math.exp(shannonIndex());
        }
        double sum = 0;
        HashSet<Integer> coveredBranches = new HashSet<>(counter.getNonZeroIndices());
        for (Integer idx : coveredBranches) {
            double  p_i = ((double)counter.getAtIndex(idx))/totalBranchHitCount;
            sum += Math.pow(p_i, order);
        }
        return Math.pow(sum, 1/(1-order));
    }
}
