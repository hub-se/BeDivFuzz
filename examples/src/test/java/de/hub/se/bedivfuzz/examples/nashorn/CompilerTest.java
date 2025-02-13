package de.hub.se.bedivfuzz.examples.nashorn;


import com.pholser.junit.quickcheck.From;
import de.hub.se.bedivfuzz.BeDivFuzz;
import de.hub.se.bedivfuzz.examples.js.SplitJavaScriptCodeGenerator;
import edu.berkeley.cs.jqf.fuzz.Fuzz;
import org.junit.Assume;
import org.junit.runner.RunWith;
import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory;

import javax.script.Compilable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
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
    public void testWithSplitGenerator(@From(SplitJavaScriptCodeGenerator.class) String code) {
        testWithReader(new StringReader(code));
    }
}
