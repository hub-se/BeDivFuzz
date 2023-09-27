
package janala.instrument;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Arrays;
import java.util.regex.Pattern;


public class SnoopInstructionClassAdapter extends ClassVisitor {
  private final String className;
  private String superName;
  private boolean trackSemanticAnalysis;

  public SnoopInstructionClassAdapter(ClassVisitor cv, String className, Pattern[] semanticAnalysisClasses) {
    super(Opcodes.ASM8, cv);
    this.className = className;
    this.trackSemanticAnalysis = false;
    if (Config.instance.trackSemanticCoverage
            && (Arrays.stream(semanticAnalysisClasses).anyMatch(pattern -> pattern.matcher(className).matches()))) {
        this.trackSemanticAnalysis = true;
    }
  }

  @Override
  public void visit(int version,
                    int access,
                    String name,
                    String signature,
                    String superName,
                    String[] interfaces) {
    assert name.equals(this.className);
    this.superName = superName;
    cv.visit(version, access, name, signature, superName, interfaces);
  }

  @Override
  public MethodVisitor visitMethod(int access, String name, String desc,
      String signature, String[] exceptions) {
    MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
    if (mv != null) {
      if (Config.instance.useFastCoverageInstrumentation){
        if (trackSemanticAnalysis) {
          MethodVisitor smv = new FastSemanticCoverageMethodAdapter(mv);
          return new FastCoverageMethodAdapter(smv, className, name, desc, superName, GlobalStateForInstrumentation.instance);
        } else {
          return new FastCoverageMethodAdapter(mv, className, name, desc, superName, GlobalStateForInstrumentation.instance);
        }
      }else {
        return new SnoopInstructionMethodAdapter(mv, className, name, desc, superName,
                GlobalStateForInstrumentation.instance, (access & Opcodes.ACC_STATIC) != 0);
      }
    }
    return null;
  }
}
