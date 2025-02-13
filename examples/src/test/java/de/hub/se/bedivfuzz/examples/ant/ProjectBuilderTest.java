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
package de.hub.se.bedivfuzz.examples.ant;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import com.pholser.junit.quickcheck.From;
import com.pholser.junit.quickcheck.generator.Size;
import de.hub.se.bedivfuzz.BeDivFuzz;
import de.hub.se.bedivfuzz.examples.xml.SplitXmlDocumentGenerator;
import edu.berkeley.cs.jqf.examples.xml.XMLDocumentUtils;
import edu.berkeley.cs.jqf.examples.common.Dictionary;
import edu.berkeley.cs.jqf.fuzz.Fuzz;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.helper.ProjectHelperImpl;
import org.junit.Assume;
import org.junit.runner.RunWith;
import org.w3c.dom.Document;

@RunWith(BeDivFuzz.class)
public class ProjectBuilderTest {
    private static final File file;

    static {
        try {
            file = Files.createTempFile("build", ".xml").toFile();
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Fuzz
    public void testWithInputStream(InputStream in) {
        try {
            Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            ProjectHelperImpl p = new ProjectHelperImpl();
            p.parse(new Project(), file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (BuildException e) {
            Assume.assumeNoException(e);
        }
    }

    @Fuzz
    public void testWithSplitGenerator(@From(SplitXmlDocumentGenerator.class)
                                           @Size(max = 10)
                                           @Dictionary("dictionaries/ant-project.dict") Document dom) {
        testWithInputStream(XMLDocumentUtils.documentToInputStream(dom));
    }
}
