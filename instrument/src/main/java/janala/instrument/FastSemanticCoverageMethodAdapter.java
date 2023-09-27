package janala.instrument;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;


public class FastSemanticCoverageMethodAdapter extends MethodVisitor implements Opcodes {
    private ProbeCounter probeCounter = ProbeCounter.instance;

    private String semanticAnalysisClass;

    public FastSemanticCoverageMethodAdapter(MethodVisitor mv) {
        super(ASM8, mv);

        // TODO:Read from config
        this.semanticAnalysisClass = "edu/berkeley/cs/jqf/instrument/tracing/FastSemanticCoverageSnoop";
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        probeCounter.incrementSemanticProbes();

        // Replace coverage snoop when logging probes of semantic classes
        if (owner.equals(Config.instance.analysisClass) && opcode == INVOKESTATIC) {
            mv.visitMethodInsn(opcode, semanticAnalysisClass, name, desc, itf);
        } else {
            mv.visitMethodInsn(opcode, owner, name, desc, itf);
        }
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        // Probes are only added for the following instructions
        switch (opcode) {
            case IFEQ:
            case IFNE:
            case IFLT:
            case IFGE:
            case IFGT:
            case IFLE:
            case IF_ICMPEQ:
            case IF_ICMPNE:
            case IF_ICMPLT:
            case IF_ICMPGE:
            case IF_ICMPGT:
            case IF_ICMPLE:
            case IF_ACMPEQ:
            case IF_ACMPNE:
            case IFNULL:
            case IFNONNULL:
                probeCounter.addSemanticProbes(2);
                break;
            case GOTO:
            case JSR:
                break;
            default:
                throw new RuntimeException("Unknown jump opcode " + opcode);
        }
        mv.visitJumpInsn(opcode, label);
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
        // Add probes for each label + default case
        probeCounter.addSemanticProbes(labels.length + 1);
        mv.visitTableSwitchInsn(min, max, dflt, labels);
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        // Add probes for each key + default case
        probeCounter.addSemanticProbes(keys.length + 1);
        mv.visitLookupSwitchInsn(dflt, keys, labels);
    }


}
