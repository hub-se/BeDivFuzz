package edu.berkeley.cs.jqf.examples.plexus;

import com.pholser.junit.quickcheck.From;
import edu.berkeley.cs.jqf.examples.common.Dictionary;
import de.hub.se.jqf.examples.xml.SplitXmlDocumentGenerator;
import edu.berkeley.cs.jqf.examples.xml.XMLDocumentUtils;
import edu.berkeley.cs.jqf.examples.xml.XmlDocumentGenerator;
import edu.berkeley.cs.jqf.fuzz.Fuzz;
import edu.berkeley.cs.jqf.fuzz.JQF;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Assume;
import org.junit.runner.RunWith;
import org.w3c.dom.Document;

import java.io.IOException;
import java.io.InputStream;


@RunWith(JQF.class)
public class ModelReaderTest {

    @Fuzz
    public void testWithInputStream(InputStream in) {
        MavenXpp3Reader mavenXpp3Reader = new MavenXpp3Reader();
        try {
            Model generated = mavenXpp3Reader.read(in);
        } catch (XmlPullParserException e) {
            Assume.assumeNoException(e);
        } catch (IOException e) {
            Assume.assumeNoException(e);
        }
    }

    @Fuzz
    public void testWithGenerator(@From(XmlDocumentGenerator.class)
                                  @Dictionary("dictionaries/maven-model.dict") Document dom) {
        testWithInputStream(XMLDocumentUtils.documentToInputStream(dom));
    }

    @Fuzz
    public void testWithSplitGenerator(@From(SplitXmlDocumentGenerator.class)
                                  @Dictionary("dictionaries/maven-model.dict") Document dom) {
        testWithInputStream(XMLDocumentUtils.documentToInputStream(dom));
    }
}
