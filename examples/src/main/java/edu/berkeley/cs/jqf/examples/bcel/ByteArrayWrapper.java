package edu.berkeley.cs.jqf.examples.bcel;

import java.io.Serializable;

public class ByteArrayWrapper implements Serializable {

    private static final long serialVersionUID = -6531702242463390580L;
    byte[] array;
    public ByteArrayWrapper(byte[] array) {
        this.array = array;
    }

    public byte[] getByteArray() {
        return array;
    }
}
