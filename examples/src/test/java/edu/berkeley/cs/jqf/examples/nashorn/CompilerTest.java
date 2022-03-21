package edu.berkeley.cs.jqf.examples.nashorn;

import com.pholser.junit.quickcheck.From;
import de.hub.se.jqf.examples.js.SplitJavaScriptCodeGenerator;
import edu.berkeley.cs.jqf.examples.js.JavaScriptCodeGenerator;
import edu.berkeley.cs.jqf.fuzz.Fuzz;
import edu.berkeley.cs.jqf.fuzz.JQF;
import org.junit.Assume;
import org.junit.runner.RunWith;

import javax.script.*;


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
    public void testWithSplitGenerator(@From(SplitJavaScriptCodeGenerator.class) String code) {
        //System.setProperty("nashorn.args", "--no-deprecation-warning");
        try {
            CompiledScript compiled = ((Compilable) engine).compile(code);
        }
        catch (ScriptException e) {
            Assume.assumeNoException(e);
        }
    }
}
