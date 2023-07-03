package de.hub.se.jqf.bedivfuzz.examples.bcel;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import de.hub.se.jqf.bedivfuzz.junit.quickcheck.SplitGenerator;
import de.hub.se.jqf.bedivfuzz.junit.quickcheck.SplitSourceOfRandomness;

public class SplitJavaClassNameGenerator extends SplitGenerator<String> {
    private static final String[] BASIC_CLASS_NAMES = {"java/lang/Object",
            "java/util/List",
            "java/util/Map",
            "java/lang/String",
            "example/A",
            "example/B",
            "java/lang/Throwable",
            "java/lang/RuntimeException"};
    private final SplitGenerator<String> identifierGenerator = new SplitJavaIdentifierGenerator();
    private final String delimiter;

    public SplitJavaClassNameGenerator() {
        this("/");
    }

    public SplitJavaClassNameGenerator(String delimiter) {
        super(String.class);
        this.delimiter = delimiter;
    }

    @Override
    public String generate(SplitSourceOfRandomness random, GenerationStatus status) {
        if (random.structure.nextBoolean()) {
            return random.value.choose(BASIC_CLASS_NAMES);
        }
        String[] parts = new String[random.structure.nextInt(1, 5)];
        for (int i = 0; i < parts.length; i++) {
            parts[i] = identifierGenerator.generate(random, status);
        }
        return String.join(delimiter, parts);
    }
}
