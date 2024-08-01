package edu.berkeley.cs.jqf.examples.nashorn;

import com.pholser.junit.quickcheck.From;
import de.hub.se.jqf.bedivfuzz.BeDivFuzz;
import de.hub.se.jqf.bedivfuzz.examples.js.SplitJavaScriptCodeGenerator;
import edu.berkeley.cs.jqf.examples.js.JavaScriptCodeGenerator;
import edu.berkeley.cs.jqf.fuzz.Fuzz;
import org.junit.Assume;
import org.junit.runner.RunWith;

import javax.script.*;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;


@RunWith(BeDivFuzz.class)
public class CompilerTest {

    private static final ScriptEngineManager factory = new ScriptEngineManager();
    private static final ScriptEngine engine = factory.getEngineByName("nashorn");

    public void testWithReader(Reader reader) {
        try {
            ((Compilable) engine).compile(reader);
        } catch (ScriptException e) {
            Assume.assumeNoException(e);
        }
    }

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
