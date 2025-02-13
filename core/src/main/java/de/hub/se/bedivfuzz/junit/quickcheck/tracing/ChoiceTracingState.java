package de.hub.se.bedivfuzz.junit.quickcheck.tracing;

import edu.berkeley.cs.jqf.fuzz.guidance.StreamBackedRandom;

import java.util.List;

public class ChoiceTracingState {

    private final StreamBackedRandom delegate;
    private int choiceOffset = 0;

    public ChoiceTracingState(StreamBackedRandom delegate) {
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
