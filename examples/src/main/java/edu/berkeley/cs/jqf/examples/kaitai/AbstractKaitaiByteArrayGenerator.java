/*
 * Copyright (c) 2017-2018 The Regents of the University of California
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package edu.berkeley.cs.jqf.examples.kaitai;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.generator.Size;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import edu.berkeley.cs.jqf.examples.common.ByteArrayWrapper;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

/**
 * @author Rohan Padhye
 */
public abstract class AbstractKaitaiByteArrayGenerator extends Generator<ByteArrayWrapper> {

    private final AbstractKaitaiGenerator backingGenerator;
    private int capacity = Integer.MAX_VALUE;

    public AbstractKaitaiByteArrayGenerator(AbstractKaitaiGenerator backingGenerator) {
        super(ByteArrayWrapper.class);
        this.backingGenerator = backingGenerator;
    }


    @SuppressWarnings("unused") // invoked by junit-quickcheck for @Size annotation
    public void configure(Size size) {
        this.capacity = size.max();
        backingGenerator.configure(size);
    }

    @Override
    public ByteArrayWrapper generate(SourceOfRandomness random, GenerationStatus status) {
        backingGenerator.buf = ByteBuffer.allocate(this.capacity);
        try {
            // Populate byte buffer
            backingGenerator.populate(random);

        } catch (BufferOverflowException e) {
            // throw new AssumptionViolatedException("Generated input is too large", e);
        }

        // Return the bytes as an inputstream
        ByteBuffer b = backingGenerator.buf;
        int len = b.position();
        b.rewind();
        byte[] bytes = new byte[len];
        b.get(bytes);
        return new ByteArrayWrapper(bytes);
    }

}
