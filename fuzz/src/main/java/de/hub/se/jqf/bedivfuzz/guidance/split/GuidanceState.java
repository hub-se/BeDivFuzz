package de.hub.se.jqf.bedivfuzz.guidance.split;

import de.hub.se.jqf.bedivfuzz.util.BranchHitCounter;
import edu.berkeley.cs.jqf.fuzz.util.ICoverage;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;

import java.util.Set;

public class GuidanceState {
    public int numSavedInputs;
    public long numTrials; // also update lastNumTrials
    public long numValid;
    public int cyclesCompleted;
    public ICoverage totalCoverage;
    public ICoverage validCoverage;
    public ICoverage semanticTotalCoverage;
    public IntHashSet uniquePaths;
    public BranchHitCounter branchHitCounter;
    public int maxCoverage;
    public Set<String> uniqueFailures;
}
