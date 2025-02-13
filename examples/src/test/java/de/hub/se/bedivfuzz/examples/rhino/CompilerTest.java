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
package de.hub.se.bedivfuzz.examples.rhino;


import java.io.*;

import com.pholser.junit.quickcheck.From;
import de.hub.se.bedivfuzz.BeDivFuzz;
import de.hub.se.bedivfuzz.examples.js.SplitJavaScriptCodeGenerator;
import edu.berkeley.cs.jqf.fuzz.Fuzz;
import org.junit.Assume;
import org.junit.runner.RunWith;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EvaluatorException;

@RunWith(BeDivFuzz.class)
public class CompilerTest {

    @Fuzz
    public void testWithReader(Reader reader) throws IOException {
        PrintStream err = suppressStandardErr();
        try {
            Context context = Context.enter();
            try {
                context.compileReader(reader, "input", 0, null);
            } catch (EvaluatorException e) {
                Assume.assumeNoException(e);
            } finally {
                Context.exit();
            }
        } finally {
            System.setErr(err);
        }
    }

    public static PrintStream suppressStandardErr() {
        PrintStream result = System.err;
        System.setErr(new PrintStream(new OutputStream() {
            @Override
            public void write(int b) {
            }
        }));
        return result;
    }

    @Fuzz
    public void testWithSplitGenerator(@From(SplitJavaScriptCodeGenerator.class) String code) throws IOException {
        testWithReader(new StringReader(code));
    }

}
