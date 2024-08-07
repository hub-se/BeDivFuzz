package de.hub.se.jqf.bedivfuzz.junit.quickcheck.tracking;

import edu.berkeley.cs.jqf.fuzz.guidance.StreamBackedRandom;

import java.util.List;

public class ChoiceTrackingState {

    private final StreamBackedRandom delegate;
    private int choiceOffset = 0;

    public ChoiceTrackingState(StreamBackedRandom delegate) {
        this.delegate = delegate;
    }

    public void appendChoiceIndex(List<Choice> choiceIndices) {
        int bytesRead = delegate.getTotalBytesRead() - choiceOffset;
        choiceIndices.add(new Choice(choiceOffset, bytesRead));
        choiceOffset += bytesRead;
    }

    public void appendChoiceIndex(List<Choice> choiceIndices, int length) {
        int bytesRead = delegate.getTotalBytesRead() - choiceOffset;
        choiceIndices.add(new Choice(choiceOffset, length));
        choiceOffset += bytesRead;
    }

    public StreamBackedRandom getDelegate() {
        return delegate;
    }

    public int getChoiceOffset() {
        return choiceOffset;
    }
}
