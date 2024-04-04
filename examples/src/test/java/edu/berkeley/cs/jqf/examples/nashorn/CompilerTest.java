package edu.berkeley.cs.jqf.examples.nashorn;

import com.pholser.junit.quickcheck.From;
import de.hub.se.jqf.bedivfuzz.BeDivFuzz;
import de.hub.se.jqf.bedivfuzz.examples.js.SplitJavaScriptCodeGenerator;
import edu.berkeley.cs.jqf.examples.js.JavaScriptCodeGenerator;
import edu.berkeley.cs.jqf.fuzz.Fuzz;
import org.junit.Assume;
import org.junit.runner.RunWith;

import javax.script.*;


@RunWith(BeDivFuzz.class)
public class CompilerTest {

    // https://stackoverflow.com/questions/25332640/getenginebynamenashorn-returns-null
    private ScriptEngineManager engineManager = new ScriptEngineManager(null);
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
