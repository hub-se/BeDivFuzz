package de.hub.se.jqf.bedivfuzz.examples.bcel;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import de.hub.se.jqf.bedivfuzz.junit.quickcheck.SplitGenerator;
import de.hub.se.jqf.bedivfuzz.junit.quickcheck.SplitSourceOfRandomness;
import org.apache.bcel.generic.*;

import java.util.Arrays;
import java.util.List;

public class SplitTypeGenerator {
    private final List<ObjectType> COMMON_TYPES =
            Arrays.asList(Type.OBJECT, Type.CLASS, Type.STRING, Type.STRINGBUFFER, Type.THROWABLE);
    private final List<BasicType> PRIMITIVE_TYPES =
            Arrays.asList(Type.BOOLEAN, Type.INT, Type.SHORT, Type.BYTE, Type.LONG, Type.DOUBLE, Type.FLOAT, Type.CHAR);
    private final SplitGenerator<String> classNameGenerator = new SplitJavaClassNameGenerator("/");
    private final SplitSourceOfRandomness random;
    private final GenerationStatus status;

    public SplitTypeGenerator(SplitSourceOfRandomness random, GenerationStatus status) {
        this.random = random;
        this.status = status;
    }

    public Type generate() {
        switch (random.structure.nextInt(3)) {
            case 0:
                return generateArrayType();
            case 1:
                return generateObjectType();
            default:
                return generatePrimitiveType();
        }
    }

    public BasicType generatePrimitiveType() {
        return random.value.choose(PRIMITIVE_TYPES);
    }

    public ArrayType generateArrayType() {
        Type type = random.structure.nextBoolean() ? generatePrimitiveType() : generateObjectType();
        return new ArrayType(type, random.value.nextInt(1, 10));
    }

    public ObjectType generateObjectType() {
        if (random.structure.nextBoolean()) {
            return random.value.choose(COMMON_TYPES);
        } else {
            return new ObjectType(classNameGenerator.generate(random, status));
        }
    }

    public ReferenceType generateReferenceType() {
        return random.structure.nextBoolean() ? generateArrayType() : generateObjectType();
    }
}
