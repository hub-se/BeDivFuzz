package de.hub.se.bedivfuzz.junit.quickcheck.tracing;

/**
 * Size and offset of an untyped choice performed by the random generator.
 */
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Choice choice = (Choice) o;
        return offset == choice.offset && size == choice.size;
    }

    @Override
    public int hashCode() {
        int result = offset;
        result = 31 * result + size;
        return result;
    }
}
