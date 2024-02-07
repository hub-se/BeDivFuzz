package de.hub.se.jqf.bedivfuzz.util;

import org.eclipse.collections.api.tuple.primitive.IntIntPair;

public class IntPair implements IntIntPair {
    private static final long serialVersionUID = 1L;
    private final int one;
    private int two;

    public IntPair(int newOne, int newTwo) {
        this.one = newOne;
        this.two = newTwo;
    }

    public int getOne() {
        return this.one;
    }

    public int getTwo() {
        return this.two;
    }

    public void incrementTwo() {
        this.two++;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof IntIntPair)) {
            return false;
        } else {
            IntIntPair that = (IntIntPair)o;
            return this.one == that.getOne() && this.two == that.getTwo();
        }
    }

    public int hashCode() {
        return 29 * this.one + this.two;
    }

    public String toString() {
        return this.one + ":" + this.two;
    }

    public int compareTo(IntIntPair that) {
        int i = this.one < that.getOne() ? -1 : (this.one > that.getOne() ? 1 : 0);
        if (i != 0) {
            return i;
        } else {
            return this.two < that.getTwo() ? -1 : (this.two > that.getTwo() ? 1 : 0);
        }
    }
}
