package edu.berkeley.cs.jqf.examples.nashorn;

import com.pholser.junit.quickcheck.From;
import edu.berkeley.cs.jqf.examples.js.JavaScriptCodeGenerator;
import edu.berkeley.cs.jqf.examples.js.JavaScriptRLGenerator;
import edu.berkeley.cs.jqf.fuzz.Fuzz;
import edu.berkeley.cs.jqf.fuzz.JQF;
import org.apache.commons.io.IOUtils;
import org.junit.Assume;
import org.junit.runner.RunWith;

import javax.script.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;


@RunWith(JQF.class)
public class CompilerTest {

    private ScriptEngineManager engineManager = new ScriptEngineManager();
    private ScriptEngine engine = engineManager.getEngineByName("nashorn");

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
    public void testWithInputStream(InputStream in) throws IOException {
        //System.setProperty("nashorn.args", "--no-deprecation-warning");
        String code = IOUtils.toString(in, StandardCharsets.UTF_8);
        try {
            CompiledScript compiled = ((Compilable) engine).compile(code);
        }
        catch (ScriptException e) {
            Assume.assumeNoException(e);
        }
    }
}
