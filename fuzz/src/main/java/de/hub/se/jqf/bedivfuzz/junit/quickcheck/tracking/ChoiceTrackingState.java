package de.hub.se.jqf.bedivfuzz.junit.quickcheck.tracking;

import de.hub.se.jqf.bedivfuzz.util.IntPair;
import edu.berkeley.cs.jqf.fuzz.guidance.StreamBackedRandom;

import java.util.List;

public class ChoiceTrackingState {

    private final StreamBackedRandom delegate;
    private int choiceOffset = 0;

    public ChoiceTrackingState(StreamBackedRandom delegate) {
        this.delegate = delegate;
    }

    public void appendChoiceIndex(List<IntPair> choiceIndices) {
        int bytesRead = delegate.getTotalBytesRead() - choiceOffset;
        choiceIndices.add(new IntPair(choiceOffset, bytesRead));
        choiceOffset += bytesRead;
    }

    public void appendChoiceIndex(List<IntPair> choiceIndices, int length) {
        int bytesRead = delegate.getTotalBytesRead() - choiceOffset;
        choiceIndices.add(new IntPair(choiceOffset, length));
        choiceOffset += bytesRead;
    }

    public StreamBackedRandom getDelegate() {
        return delegate;
    }

    public int getChoiceOffset() {
        return choiceOffset;
    }
}
