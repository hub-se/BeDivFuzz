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
package edu.berkeley.cs.jqf.examples.maven;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.introspect.TypeResolutionContext;
import com.pholser.junit.quickcheck.From;
import com.pholser.junit.quickcheck.generator.Size;
import de.hub.se.jqf.bedivfuzz.BeDivFuzz;
import de.hub.se.jqf.bedivfuzz.examples.xml.SplitXmlDocumentGenerator;
import edu.berkeley.cs.jqf.examples.xml.XMLDocumentUtils;
import edu.berkeley.cs.jqf.examples.xml.XmlDocumentGenerator;
import edu.berkeley.cs.jqf.examples.common.Dictionary;
import edu.berkeley.cs.jqf.fuzz.Fuzz;
import org.apache.maven.model.InputLocation;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.*;
import org.apache.maven.model.io.DefaultModelReader;
import org.apache.maven.model.io.ModelReader;
import org.apache.maven.model.validation.DefaultModelValidator;
import org.apache.maven.model.validation.ModelValidator;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.w3c.dom.Document;

@RunWith(BeDivFuzz.class)
public class ModelReaderTest {

    @Fuzz
    public void testWithInputStream(InputStream in) {
        ModelReader reader = new DefaultModelReader();
        try {
            Model model = reader.read(in, null);
            Assert.assertNotNull(model);
        } catch (IOException e) {
            Assume.assumeNoException(e);
        }
    }

    class BasicModelProblemCollector implements ModelProblemCollector {
        List<ModelProblem.Severity> severities = new ArrayList<>();
        List<ModelProblem.Version> versions = new ArrayList<>();
        List<String> messages = new ArrayList<>();
        List<InputLocation> locations = new ArrayList<>();
        List<Exception> causes = new ArrayList<>();

        @Override
        public void add(ModelProblemCollectorRequest request) {
            severities.add(request.getSeverity());
            versions.add(request.getVersion());
            messages.add(request.getMessage());
            locations.add(request.getLocation());
            causes.add(request.getException());
        }
    }


    @Fuzz
    public void validateWithInputStream(InputStream in) {
        ModelReader reader = new DefaultModelReader();
        ModelValidator validator = new DefaultModelValidator();
        ModelBuildingRequest request = new DefaultModelBuildingRequest();
        ModelProblemCollector collector = new BasicModelProblemCollector();

        try {
            Model model = reader.read(in, null);
            Assert.assertNotNull(model);
            validator.validateRawModel(model, request, collector);
        } catch (IOException e) {
            Assume.assumeNoException(e);
        }
    }

    @Fuzz
    public void testWithGenerator(@From(XmlDocumentGenerator.class)
                                      @Size(min = 0, max = 10)
                                      @Dictionary("dictionaries/maven-model.dict") Document dom) {
        testWithInputStream(XMLDocumentUtils.documentToInputStream(dom));
    }

    @Fuzz
    public void validateWithGenerator(@From(XmlDocumentGenerator.class)
                                  @Size(min = 0, max = 10)
                                  @Dictionary("dictionaries/maven-model.dict") Document dom) {
        validateWithInputStream(XMLDocumentUtils.documentToInputStream(dom));
    }

    @Fuzz
    public void testWithSplitGenerator(@From(SplitXmlDocumentGenerator.class)
								  @Size(min = 0, max = 10)
                                  @Dictionary("dictionaries/maven-model.dict") Document dom) {
        testWithInputStream(XMLDocumentUtils.documentToInputStream(dom));
    }

    @Fuzz
    public void debugWithGenerator(@From(XmlDocumentGenerator.class)
									   @Size(min = 0, max = 10)
                                       @Dictionary("dictionaries/maven-model.dict") Document dom) {
        System.out.println(XMLDocumentUtils.documentToString(dom));
        testWithGenerator(dom);
    }

    @Fuzz
    public void testWithString(String input) {
        testWithInputStream(new ByteArrayInputStream(input.getBytes()));
    }

    @Test
    public void testSmall() throws IOException {
        testWithString("<Y");
    }

}
