/*
 * Copyright (c) 2018, The Regents of the University of California
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package edu.berkeley.cs.jqf.examples.bcel;


import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.internal.GeometricDistribution;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import de.hub.se.jqf.fuzz.junit.quickcheck.SplitSourceOfRandomness;
import org.apache.bcel.Const;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.*;
import org.junit.Assume;
import org.junit.AssumptionViolatedException;

/**
 * @author Rohan Padhye
 */
public class SplitJavaClassGenerator extends Generator<JavaClass> {

    private ConstantPoolGen constants;

    private String[] primitiveTypes = {
            "Z", "B", "C", "S", "I", "J", "F", "D"
    };

    private String[] interestingClasses = {
            "java/lang/Object",
            "java/lang/Integer",
            "java/util/List",
            "java/util/Map",
            "example/A",
            "example/B"
    };


    private String[] memberDictionary = {
            "foo",
            "bar",
            "baz",
            "example",
            "A",
            "B",
            "C",
            "D"
    };

    private static GeometricDistribution geom = new GeometricDistribution();


    // Constants
    private static int MEAN_INTERFACE_COUNT = 1;
    private static int MEAN_FIELDS_COUNT = 2;
    private static int MEAN_METHODS_COUNT = 2;
    private static int MEAN_ARRAY_DEPTH = 1;

    public SplitJavaClassGenerator() {
        super(JavaClass.class);
    }

    public JavaClass generate(SourceOfRandomness r, GenerationStatus s) {
        SplitSourceOfRandomness rdm = (SplitSourceOfRandomness) r;

        constants = new ConstantPoolGen();

        // Generate a class with its meta-data
        String className = "example.A";
        String superName = r.nextBoolean() ? "example.B" : "java.lang.Object";
        String fileName = "A.class";
        int flags = rdm.nextInt(0, Short.MAX_VALUE, false);
        int numInterfaces = rdm.nextBoolean(true) ? 0 : rdm.nextInt(2 * MEAN_INTERFACE_COUNT, false);
        String[] interfaces = new String[numInterfaces];
        for (int i = 0; i < numInterfaces; i++) {
            interfaces[i] = "example.I"+i;
        }
        ClassGen classGen = new ClassGen(className, superName, fileName, flags, interfaces, constants);

        // Validate flags
        Assume.assumeFalse(classGen.isFinal() && (classGen.isAbstract() | classGen.isInterface()));

        int numFields = rdm.nextInt(2 * MEAN_FIELDS_COUNT, true);
        for (int i = 0; i < numFields; i++) {
            classGen.addField(generateField(rdm));
        }

        int numMethods = rdm.nextInt(2 * MEAN_METHODS_COUNT, true);
        for (int i = 0; i < numMethods; i++) {
            classGen.addMethod(generateMethod(className, rdm));
        }

        return classGen.getJavaClass();

    }

    private Field generateField(SplitSourceOfRandomness rdm) {
        int flags = rdm.nextInt(0, Short.MAX_VALUE, false);
        Type type = generateType(rdm, true);
        String name = generateMemberName(rdm);
        FieldGen fieldGen = new FieldGen(flags, type, name, constants);
        return fieldGen.getField();
    }

    private Method generateMethod(String className, SplitSourceOfRandomness rdm) {
        int flags = rdm.nextInt(0, Short.MAX_VALUE, false);
        Type returnType = rdm.nextBoolean(true) ? Type.VOID : generateType(rdm, true);
        String methodName = generateMemberName(rdm);
        int numArgs = rdm.nextInt(4, true);
        Type[] argTypes = new Type[numArgs];
        String[] argNames = new String[numArgs];
        for (int i = 0; i < numArgs; i++) {
            argTypes[i] = generateType(rdm, true);
            argNames[i] = generateMemberName(rdm);
        }
        InstructionList code = generateCode(rdm, argTypes, returnType);
        MethodGen methodGen = new MethodGen(flags, returnType, argTypes, argNames, methodName, className, code, constants);
        // Validate flags
        Assume.assumeFalse(methodGen.isFinal() && methodGen.isAbstract());
        return methodGen.getMethod();
    }

    private String generateMemberName(SplitSourceOfRandomness rdm) {
        if (rdm.nextBoolean(true)) {
            return rdm.choose(memberDictionary, false);
        }
        return rdm.nextChar('a', 'z', false) + "_" + rdm.nextInt(10, false);
    }

    private String generateTypeSignature(SplitSourceOfRandomness rdm, boolean arraysAllowed) {
        String typeSig;
        if (rdm.nextBoolean(true)) {
            // Primitive
            typeSig = rdm.choose(primitiveTypes, false);
        } else {
            // Class type
            typeSig = "L" + generateClassName(rdm) + ";";
        }
        if (arraysAllowed && rdm.nextBoolean(true)) {
            // Generate array depth with geometric distribution
            int depth = rdm.nextInt(2 * MEAN_ARRAY_DEPTH, false);
            for (int i = 0; i < depth; i++) {
                typeSig = "[" + typeSig;
            }
        }
        return typeSig;
    }

    private Type generateType(SplitSourceOfRandomness rdm, boolean arraysAllowed) {
        return Type.getType(generateTypeSignature(rdm, arraysAllowed));
    }

    private InstructionList generateCode(SplitSourceOfRandomness rdm, Type[] argTypes, Type returnType) {
        InstructionList code = new InstructionList();

        while (rdm.nextBoolean(true)) {
            Instruction ins = generateInstruction(rdm, argTypes.length+1, code); // TODO: Allocate some locals
            if (ins instanceof BranchInstruction) {
                // Call the overloaded append()
                code.append((BranchInstruction) ins);
            } else {
                code.append(ins);
            }
        }
        return code;
    }

    private Instruction generateInstruction(SplitSourceOfRandomness rdm, int slots, InstructionList code) {

        int opcode = rdm.nextInt(256, true);
        Instruction ins = InstructionConst.getInstruction(opcode);
        if (ins != null) {
            return ins; // Used predefined immutable object, if available
        }

        switch (opcode) {
            case Const.BIPUSH:
                ins = new BIPUSH(rdm.nextByte(Byte.MIN_VALUE, Byte.MAX_VALUE, false));
                break;
            case Const.SIPUSH:
                ins = new SIPUSH(rdm.nextShort(Short.MIN_VALUE, Short.MAX_VALUE, false));
                break;
            case Const.LDC:
                ins = new LDC(constants.addString("?")); // TODO: Generate constants
                break;
            case Const.LDC_W:
                ins = new LDC(constants.addString("?")); // TODO: Generate constants
                break;
            case Const.LDC2_W:
                ins = new LDC2_W(constants.addDouble(Math.PI));
                break;
            case Const.ILOAD:
                ins = new ILOAD(rdm.nextInt(slots, false));
                break;
            case Const.LLOAD:
                ins = new LLOAD(rdm.nextInt(slots, false));
                break;
            case Const.FLOAD:
                ins = new FLOAD(rdm.nextInt(slots, false));
                break;
            case Const.DLOAD:
                ins = new DLOAD(rdm.nextInt(slots, false));
                break;
            case Const.ALOAD:
                ins = new ALOAD(rdm.nextInt(slots, false));
                break;
            case Const.ILOAD_0:
                ins = new ILOAD(0);
                break;
            case Const.ILOAD_1:
                ins = new ILOAD(1);
                break;
            case Const.ILOAD_2:
                ins = new ILOAD(2);
                break;
            case Const.ILOAD_3:
                ins = new ILOAD(3);
                break;
            case Const.LLOAD_0:
                ins = new LLOAD(0);
                break;
            case Const.LLOAD_1:
                ins = new LLOAD(1);
                break;
            case Const.LLOAD_2:
                ins = new LLOAD(2);
                break;
            case Const.LLOAD_3:
                ins = new LLOAD(3);
                break;
            case Const.FLOAD_0:
                ins = new FLOAD(0);
                break;
            case Const.FLOAD_1:
                ins = new FLOAD(1);
                break;
            case Const.FLOAD_2:
                ins = new FLOAD(2);
                break;
            case Const.FLOAD_3:
                ins = new FLOAD(3);
                break;
            case Const.DLOAD_0:
                ins = new DLOAD(0);
                break;
            case Const.DLOAD_1:
                ins = new DLOAD(1);
                break;
            case Const.DLOAD_2:
                ins = new DLOAD(2);
                break;
            case Const.DLOAD_3:
                ins = new DLOAD(3);
                break;
            case Const.ALOAD_0:
                ins = new ALOAD(0);
                break;
            case Const.ALOAD_1:
                ins = new ALOAD(1);
                break;
            case Const.ALOAD_2:
                ins = new ALOAD(2);
                break;
            case Const.ALOAD_3:
                ins = new ALOAD(3);
                break;
            case Const.ISTORE:
                ins = new ISTORE(rdm.nextInt(slots, false));
                break;
            case Const.LSTORE:
                ins = new LSTORE(rdm.nextInt(slots, false));
                break;
            case Const.FSTORE:
                ins = new FSTORE(rdm.nextInt(slots, false));
                break;
            case Const.DSTORE:
                ins = new DSTORE(rdm.nextInt(slots, false));
                break;
            case Const.ASTORE:
                ins = new ASTORE(rdm.nextInt(slots, false));
                break;
            case Const.ISTORE_0:
                ins = new ISTORE(0);
                break;
            case Const.ISTORE_1:
                ins = new ISTORE(1);
                break;
            case Const.ISTORE_2:
                ins = new ISTORE(2);
                break;
            case Const.ISTORE_3:
                ins = new ISTORE(3);
                break;
            case Const.LSTORE_0:
                ins = new LSTORE(0);
                break;
            case Const.LSTORE_1:
                ins = new LSTORE(1);
                break;
            case Const.LSTORE_2:
                ins = new LSTORE(2);
                break;
            case Const.LSTORE_3:
                ins = new LSTORE(3);
                break;
            case Const.FSTORE_0:
                ins = new FSTORE(0);
                break;
            case Const.FSTORE_1:
                ins = new FSTORE(1);
                break;
            case Const.FSTORE_2:
                ins = new FSTORE(2);
                break;
            case Const.FSTORE_3:
                ins = new FSTORE(3);
                break;
            case Const.DSTORE_0:
                ins = new DSTORE(0);
                break;
            case Const.DSTORE_1:
                ins = new DSTORE(1);
                break;
            case Const.DSTORE_2:
                ins = new DSTORE(2);
                break;
            case Const.DSTORE_3:
                ins = new DSTORE(3);
                break;
            case Const.ASTORE_0:
                ins = new ASTORE(0);
                break;
            case Const.ASTORE_1:
                ins = new ASTORE(1);
                break;
            case Const.ASTORE_2:
                ins = new ASTORE(2);
                break;
            case Const.ASTORE_3:
                ins = new ASTORE(3);
                break;
            case Const.IINC:
                ins = new IINC(rdm.nextInt(slots, false), rdm.nextInt(-128, 128, false));
                break;
            case Const.IFEQ:
                ins = new IFEQ(generateLabel(rdm, code));
                break;
            case Const.IFNE:
                ins = new IFNE(generateLabel(rdm, code));
                break;
            case Const.IFLT:
                ins = new IFLT(generateLabel(rdm, code));
                break;
            case Const.IFGE:
                ins = new IFGE(generateLabel(rdm, code));
                break;
            case Const.IFGT:
                ins = new IFGT(generateLabel(rdm, code));
                break;
            case Const.IFLE:
                ins = new IFLE(generateLabel(rdm, code));
                break;
            case Const.IF_ICMPEQ:
                ins = new IF_ICMPEQ(generateLabel(rdm, code));
                break;
            case Const.IF_ICMPNE:
                ins = new IF_ICMPNE(generateLabel(rdm, code));
                break;
            case Const.IF_ICMPLT:
                ins = new IF_ICMPLT(generateLabel(rdm, code));
                break;
            case Const.IF_ICMPGE:
                ins = new IF_ICMPGE(generateLabel(rdm, code));
                break;
            case Const.IF_ICMPGT:
                ins = new IF_ICMPGT(generateLabel(rdm, code));
                break;
            case Const.IF_ICMPLE:
                ins = new IF_ICMPLE(generateLabel(rdm, code));
                break;
            case Const.IF_ACMPEQ:
                ins = new IF_ACMPEQ(generateLabel(rdm, code));
                break;
            case Const.IF_ACMPNE:
                ins = new IF_ACMPNE(generateLabel(rdm, code));
                break;
            case Const.GOTO:
                ins = new GOTO(generateLabel(rdm, code));
                break;
                /*
            case Const.JSR:
                ins = new JSR(generateLabel(r));
                break;
            case Const.RET:
                ins = new RET(r.nextInt(slots));
                break;
            case Const.TABLESWITCH:
                ins = new TABLESWITCH();
                break;
            case Const.LOOKUPSWITCH:
                ins = new LOOKUPSWITCH();
                break;
               */
            case Const.GETSTATIC:
                ins = new GETSTATIC(generateFieldRef(rdm));
                break;
            case Const.PUTSTATIC:
                ins = new PUTSTATIC(generateFieldRef(rdm));
                break;
            case Const.GETFIELD:
                ins = new GETFIELD(generateFieldRef(rdm));
                break;
            case Const.PUTFIELD:
                ins = new PUTFIELD(generateFieldRef(rdm));
                break;
            case Const.INVOKEVIRTUAL:
                ins = new INVOKEVIRTUAL(generateMethodRef(rdm));
                break;
            case Const.INVOKESPECIAL:
                ins = new INVOKESPECIAL(generateMethodRef(rdm));
                break;
            case Const.INVOKESTATIC:
                ins = new INVOKESTATIC(generateMethodRef(rdm));
                break;
            case Const.INVOKEINTERFACE:
                ins = new INVOKEINTERFACE(generateMethodRef(rdm), rdm.nextInt(1, 4, false));
                break;
            case Const.INVOKEDYNAMIC:
                ins = new INVOKEDYNAMIC(generateMethodRef(rdm));
                break;
            case Const.NEW:
                ins = new NEW(generateClassRef(rdm));
                break;
            case Const.NEWARRAY:
                ins = new NEWARRAY(rdm.nextByte((byte) 4, (byte) 12, false)); // TODO: Document basic type codes
                break;
            case Const.ANEWARRAY:
                ins = new ANEWARRAY(generateClassRef(rdm));
                break;
            case Const.CHECKCAST:
                ins = new CHECKCAST(generateClassRef(rdm));
                break;
            case Const.INSTANCEOF:
                ins = new INSTANCEOF(generateClassRef(rdm));
                break;
            case Const.MULTIANEWARRAY:
                ins = new MULTIANEWARRAY(generateClassRef(rdm), rdm.nextShort((short) 1 , Short.MAX_VALUE, false));
                break;
            case Const.IFNULL:
                ins = new IFNULL(generateLabel(rdm, code));
                break;
            case Const.IFNONNULL:
                ins = new IFNONNULL(generateLabel(rdm, code));
                break;
            case Const.GOTO_W:
                ins = new GOTO_W(generateLabel(rdm, code));
                break;
            default:
                throw new AssumptionViolatedException("Invalid opcode");

        }
        return ins;
    }

    InstructionHandle generateLabel(SplitSourceOfRandomness rdm, InstructionList code) {
        InstructionHandle handles[] = code.getInstructionHandles();
        // If no instructions generated so far, emit a NOP to get some label
        if (handles.length == 0) {
            handles = new InstructionHandle[]{ code.append(new NOP()) };
        }
        return rdm.choose(handles, false);
    }

    String generateClassName(SplitSourceOfRandomness rdm)
    {
        if (rdm.nextBoolean(true)) {
            return rdm.choose(interestingClasses, false);
        }
        int numParts = rdm.nextInt(1, 5, true);
        String[] parts = new String[numParts];
        for (int i = 0; i < numParts; i++) {
            parts[i] = generateMemberName(rdm);
        }
        return String.join("/", parts);
    }

    int generateFieldRef(SplitSourceOfRandomness rdm) {
        String sig = "()" + generateTypeSignature(rdm, true);
        return constants.addFieldref(generateClassName(rdm), generateMemberName(rdm), sig);
    }

    int generateMethodRef(SplitSourceOfRandomness rdm) {
        int numParams = rdm.nextInt(4, true);
        String sig = "(";
        for (int i = 0; i < numParams; i++) {
            sig += generateTypeSignature(rdm, true);
        }
        sig += ")";
        sig += generateTypeSignature(rdm, true);
        return constants.addMethodref(generateClassName(rdm), generateMemberName(rdm), sig);
    }

    int generateClassRef(SplitSourceOfRandomness rdm) {
        return constants.addClass(generateClassName(rdm));
    }
}
