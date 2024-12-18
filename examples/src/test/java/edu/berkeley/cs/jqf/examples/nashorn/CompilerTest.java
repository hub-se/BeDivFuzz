package edu.berkeley.cs.jqf.examples.nashorn;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.pholser.junit.quickcheck.From;
import de.hub.se.jqf.bedivfuzz.BeDivFuzz;
import de.hub.se.jqf.bedivfuzz.examples.js.SplitJavaScriptCodeGenerator;
import edu.berkeley.cs.jqf.examples.js.JavaScriptCodeGenerator;
import edu.berkeley.cs.jqf.fuzz.Fuzz;
import org.apache.commons.io.IOUtils;
import org.junit.Assume;
import org.junit.runner.RunWith;
import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory;

import javax.script.Compilable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;


@RunWith(BeDivFuzz.class)
public class CompilerTest {

    private static final NashornScriptEngineFactory factory = new NashornScriptEngineFactory();
    private static final ScriptEngine engine = factory.getScriptEngine("-strict", "--language=es6");

    public void testWithReader(Reader reader) {
        try {
            ((Compilable) engine).compile(reader);
        } catch (ScriptException e) {
            Assume.assumeNoException(e);
        }
    }

    @Fuzz
    public void testWithInputStream(InputStream in) {
        testWithReader(new InputStreamReader(in));
    }

    @Fuzz
    public void testWithGenerator(@From(JavaScriptCodeGenerator.class) String code) {
        testWithReader(new StringReader(code));
    }

    @Fuzz
    public void testWithSplitGenerator(@From(SplitJavaScriptCodeGenerator.class) String code) {
        testWithReader(new StringReader(code));
    }
}
