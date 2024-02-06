package de.hub.se.jqf.bedivfuzz.examples.bcel;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import de.hub.se.jqf.bedivfuzz.junit.quickcheck.SplitGenerator;
import de.hub.se.jqf.bedivfuzz.junit.quickcheck.SplitRandom;
import org.apache.bcel.generic.*;

import java.util.Arrays;
import java.util.List;

public class SplitTypeGenerator {
    private final List<ObjectType> COMMON_TYPES =
            Arrays.asList(Type.OBJECT, Type.CLASS, Type.STRING, Type.STRINGBUFFER, Type.THROWABLE);
    private final List<BasicType> PRIMITIVE_TYPES =
            Arrays.asList(Type.BOOLEAN, Type.INT, Type.SHORT, Type.BYTE, Type.LONG, Type.DOUBLE, Type.FLOAT, Type.CHAR);
    private final SplitGenerator<String> classNameGenerator = new SplitJavaClassNameGenerator("/");
    private final SplitRandom random;
    private final GenerationStatus status;

    public SplitTypeGenerator(SplitRandom random, GenerationStatus status) {
        this.random = random;
        this.status = status;
    }

    public Type generate() {
        switch (random.nextStructureInt(3)) {
            case 0:
                return generateArrayType();
            case 1:
                return generateObjectType();
            default:
                return generatePrimitiveType();
        }
    }

    public BasicType generatePrimitiveType() {
        return random.chooseValue(PRIMITIVE_TYPES);
    }

    public ArrayType generateArrayType() {
        Type type = random.nextStructureBoolean() ? generatePrimitiveType() : generateObjectType();
        return new ArrayType(type, random.nextValueInt(1, 10));
    }

    public ObjectType generateObjectType() {
        if (random.nextStructureBoolean()) {
            return random.chooseValue(COMMON_TYPES);
        } else {
            return new ObjectType(classNameGenerator.generate(random, status));
        }
    }

    public ReferenceType generateReferenceType() {
        return random.nextStructureBoolean() ? generateArrayType() : generateObjectType();
    }
}
