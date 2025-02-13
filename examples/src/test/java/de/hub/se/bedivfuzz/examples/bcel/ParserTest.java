/*
 * Copyright (c) 2018, The Regents of the University of California
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
package de.hub.se.bedivfuzz.examples.bcel;

import java.io.*;

import com.pholser.junit.quickcheck.From;
import de.hub.se.bedivfuzz.BeDivFuzz;
import edu.berkeley.cs.jqf.fuzz.Fuzz;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.ClassFormatException;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.verifier.VerificationResult;
import org.apache.bcel.verifier.Verifier;
import org.apache.bcel.verifier.VerifierFactory;
import org.junit.Assume;
import org.junit.runner.RunWith;


@RunWith(BeDivFuzz.class)
public class ParserTest {

    static {
        System.setErr(new PrintStream(new OutputStream() {
            public void write(int b) {
            }
        }));
    }

    @Fuzz
    public void testWithInputStream(InputStream in) throws IOException {
        JavaClass clazz;
        try {
            clazz = new ClassParser(in, "Hello.class").parse();
        } catch (ClassFormatException e) {
            // ClassFormatException thrown by the parser is just invalid input
            Assume.assumeNoException(e);
            return;
        }
        // Any non-IOException thrown here should be marked a failure (including ClassFormatException)
        try {
            Repository.addClass(clazz);
            Verifier verifier = VerifierFactory.getVerifier(clazz.getClassName());
            Assume.assumeTrue(VerificationResult.VERIFIED_OK == verifier.doPass1().getStatus());
            Assume.assumeTrue(VerificationResult.VERIFIED_OK == verifier.doPass2().getStatus());
            for (int i = 0; i < clazz.getMethods().length; i++) {
                Assume.assumeTrue(VerificationResult.VERIFIED_OK == verifier.doPass3a(i).getStatus());
                Assume.assumeTrue(VerificationResult.VERIFIED_OK == verifier.doPass3b(i).getStatus());
            }
        } finally {
            VerifierFactory.clear();
            Repository.clearCache();
        }
    }

    @Fuzz
    public void testWithSplitGenerator(@From(SplitJavaClassGenerator.class) JavaClass javaClass) throws IOException {
        testWithInputStream(new ByteArrayInputStream(javaClass.getBytes()));
    }
}
