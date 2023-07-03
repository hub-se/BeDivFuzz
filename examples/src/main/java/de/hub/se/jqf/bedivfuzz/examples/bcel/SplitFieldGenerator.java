package de.hub.se.jqf.bedivfuzz.examples.bcel;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.generator.java.lang.StringGenerator;
import de.hub.se.jqf.bedivfuzz.junit.quickcheck.SplitGenerator;
import de.hub.se.jqf.bedivfuzz.junit.quickcheck.SplitSourceOfRandomness;
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
    private final SplitSourceOfRandomness random;
    private final GenerationStatus status;
    private final ClassGen clazz;

    public SplitFieldGenerator(SplitSourceOfRandomness random, GenerationStatus status, ClassGen clazz) {
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
            if (random.structure.nextBoolean()) {
                switch (type.getType()) {
                    case Const.T_BOOLEAN:
                        field.setInitValue(random.value.nextBoolean());
                        break;
                    case Const.T_BYTE:
                        field.setInitValue(random.value.nextByte(Byte.MIN_VALUE, Byte.MAX_VALUE));
                        break;
                    case Const.T_SHORT:
                        field.setInitValue(random.value.nextShort(Short.MIN_VALUE, Short.MAX_VALUE));
                        break;
                    case Const.T_CHAR:
                        field.setInitValue(random.value.nextChar(Character.MIN_VALUE, Character.MAX_VALUE));
                        break;
                    case Const.T_INT:
                        field.setInitValue(random.value.nextInt());
                        break;
                    case Const.T_LONG:
                        field.setInitValue(random.value.nextLong());
                        break;
                    case Const.T_DOUBLE:
                        field.setInitValue(random.value.nextDouble());
                        break;
                    case Const.T_FLOAT:
                        field.setInitValue(random.value.nextFloat());
                        break;
                }
            }
        } else if (type.equals(Type.STRING)) {
            if (random.structure.nextBoolean()) {
                field.setInitValue(stringConstantGenerator.generate(random.value, status));
            }
        }
    }

    void setAccessFlags(FieldGen field) {
        if (random.value.nextBoolean()) {
            field.isSynthetic(true);
        }
        if (clazz.isInterface()) {
            field.isPublic(true);
            field.isStatic(true);
            field.isFinal(true);
        } else {
            random.value.choose(VISIBILITY_SETTERS).accept(field);
            if (random.value.nextBoolean()) {
                field.isStatic(true);
            }
            if (random.value.nextBoolean()) {
                field.isTransient(true);
            }
            if (random.value.nextBoolean()) {
                field.isEnum(true);
            }
            switch (random.value.nextInt(3)) {
                case 0:
                    field.isFinal(true);
                    break;
                case 1:
                    field.isVolatile(true);
            }
        }
    }
}
