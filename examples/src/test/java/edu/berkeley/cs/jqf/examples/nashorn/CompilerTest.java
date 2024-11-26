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
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

@RunWith(BeDivFuzz.class)
public class CompilerTest {
    private NashornScriptEngineFactory factory = new NashornScriptEngineFactory();
    private ScriptEngine engine = factory.getScriptEngine("-strict", "--language=es6");

    @Fuzz
    public void testWithGenerator(@From(JavaScriptCodeGenerator.class) String code) {
        //System.setProperty("nashorn.args", "--no-deprecation-warning");
        try {
            CompiledScript compiled = ((Compilable) engine).compile(code);
        }
        catch (ScriptException e) {
            Assume.assumeNoException(e);
        }
    }

    @Fuzz
    public void testWithSplitGenerator(@From(SplitJavaScriptCodeGenerator.class) String code) {
        //System.setProperty("nashorn.args", "--no-deprecation-warning");
        try {
            CompiledScript compiled = ((Compilable) engine).compile(code);
        }
        catch (ScriptException e) {
            Assume.assumeNoException(e);
        }
    }

    @Fuzz
    public void testWithInputStream(InputStream in) throws IOException {
        //System.setProperty("nashorn.args", "--no-deprecation-warning");
        try {
            String code = IOUtils.toString(in, StandardCharsets.UTF_8);
            CompiledScript compiled = ((Compilable) engine).compile(code);
        }
        catch (ScriptException e) {
            Assume.assumeNoException(e);
        }
    }
}
