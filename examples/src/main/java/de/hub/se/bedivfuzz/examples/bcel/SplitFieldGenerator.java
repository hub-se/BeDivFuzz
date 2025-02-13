package de.hub.se.bedivfuzz.examples.bcel;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.generator.java.lang.StringGenerator;
import de.hub.se.bedivfuzz.junit.quickcheck.SplitGenerator;
import de.hub.se.bedivfuzz.junit.quickcheck.SplitRandom;
import org.apache.bcel.Const;
import org.apache.bcel.classfile.AccessFlags;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.generic.BasicType;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.FieldGen;
import org.apache.bcel.generic.Type;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Based on:
 * <a href="https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html">
 * Java Virtual Machine Specification 4
 * </a>
 */
public final class SplitFieldGenerator {
    private static final List<Consumer<? super AccessFlags>> VISIBILITY_SETTERS =
            Arrays.asList((f) -> f.isPrivate(true), (f) -> f.isPublic(true), (f) -> f.isProtected(true), (f) -> {
            });
    private final SplitGenerator<String> identifierGenerator = new SplitJavaIdentifierGenerator();
    private final Generator<String> stringConstantGenerator = new StringGenerator();
    private final SplitTypeGenerator typeGenerator;
    private final SplitRandom random;
    private final GenerationStatus status;
    private final ClassGen clazz;

    public SplitFieldGenerator(SplitRandom random, GenerationStatus status, ClassGen clazz) {
        this.random = random;
        this.status = status;
        this.clazz = clazz;
        this.typeGenerator = new SplitTypeGenerator(random, status);
    }

    public Field generate() {
        Type type = typeGenerator.generate();
        String name = identifierGenerator.generate(random, status);
        FieldGen field = new FieldGen(0, type, name, clazz.getConstantPool());
        setAccessFlags(field);
        if (field.isFinal()) {
            setInitValue(type, field);
        }
        return field.getField();
    }

    private void setInitValue(Type type, FieldGen field) {
        if (type instanceof BasicType) {
            if (random.nextStructureBoolean()) {
                switch (type.getType()) {
                    case Const.T_BOOLEAN:
                        field.setInitValue(random.nextValueBoolean());
                        break;
                    case Const.T_BYTE:
                        field.setInitValue(random.nextValueByte(Byte.MIN_VALUE, Byte.MAX_VALUE));
                        break;
                    case Const.T_SHORT:
                        field.setInitValue(random.nextValueShort(Short.MIN_VALUE, Short.MAX_VALUE));
                        break;
                    case Const.T_CHAR:
                        field.setInitValue(random.nextValueChar(Character.MIN_VALUE, Character.MAX_VALUE));
                        break;
                    case Const.T_INT:
                        field.setInitValue(random.nextValueInt());
                        break;
                    case Const.T_LONG:
                        field.setInitValue(random.nextValueLong());
                        break;
                    case Const.T_DOUBLE:
                        field.setInitValue(random.nextValueDouble());
                        break;
                    case Const.T_FLOAT:
                        field.setInitValue(random.nextValueFloat());
                        break;
                }
            }
        } else if (type.equals(Type.STRING)) {
            if (random.nextStructureBoolean()) {
                field.setInitValue(stringConstantGenerator.generate(random.getValueDelegate(), status));
            }
        }
    }

    void setAccessFlags(FieldGen field) {
        if (random.nextValueBoolean()) {
            field.isSynthetic(true);
        }
        if (clazz.isInterface()) {
            field.isPublic(true);
            field.isStatic(true);
            field.isFinal(true);
        } else {
            random.chooseValue(VISIBILITY_SETTERS).accept(field);
            if (random.nextValueBoolean()) {
                field.isStatic(true);
            }
            if (random.nextValueBoolean()) {
                field.isTransient(true);
            }
            if (random.nextValueBoolean()) {
                field.isEnum(true);
            }
            switch (random.nextValueInt(3)) {
                case 0:
                    field.isFinal(true);
                    break;
                case 1:
                    field.isVolatile(true);
            }
        }
    }
}
