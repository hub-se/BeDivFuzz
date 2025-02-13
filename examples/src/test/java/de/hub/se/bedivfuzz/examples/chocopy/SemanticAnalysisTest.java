package de.hub.se.bedivfuzz.examples.chocopy;

import chocopy.common.astnodes.Program;
import chocopy.reference.RefAnalysis;
import chocopy.reference.RefParser;
import com.pholser.junit.quickcheck.From;
import de.hub.se.bedivfuzz.BeDivFuzz;
import edu.berkeley.cs.jqf.fuzz.Fuzz;
import org.junit.runner.RunWith;

import static org.junit.Assume.assumeTrue;

@RunWith(BeDivFuzz.class)
public class SemanticAnalysisTest {

    /** Entry point for fuzzing reference ChocoPy semantic analysis with ChocoPy code generator */
    @Fuzz
    public void testWithSplitGenerator(@From(SplitChocoPySemanticGenerator.class) String code) {
        Program program = RefParser.process(code, false);
        assumeTrue(!program.hasErrors());
        RefAnalysis.process(program);
    }
}
