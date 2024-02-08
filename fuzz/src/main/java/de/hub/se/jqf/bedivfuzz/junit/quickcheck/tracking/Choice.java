package de.hub.se.jqf.bedivfuzz.junit.quickcheck.tracking;

public class Choice {
    private final int offset;
    private final int size;

    public Choice(int offset, int size) {
        this.offset = offset;
        this.size = size;
    }

    public int getOffset() {
        return this.offset;
    }

    public int getSize() {
        return this.size;
    }

    public String toString() {
        return this.offset + ":" + this.size;
    }

}
