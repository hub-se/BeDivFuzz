package de.hub.se.jqf.bedivfuzz.junit.quickcheck.generator;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import de.hub.se.jqf.bedivfuzz.junit.quickcheck.SplitRandom;

public class SplitBinaryTreeGenerator extends Generator<Node> {
    private final int MAX_DEPTH = 5;

    public SplitBinaryTreeGenerator() {
        super(Node.class);
    }

    @Override
    public Node generate(SourceOfRandomness random, GenerationStatus status) {
        SplitRandom r = (SplitRandom) random;
        return generate(r, 0);
    }

    public Node generate(SplitRandom random, int currentDepth) {
        Node node = new Node(random.nextValueInt());
        if (currentDepth > MAX_DEPTH) {
            return node;
        }
        if (random.nextStructureBoolean()) {
            node.left = generate(random, currentDepth + 1);
        }
        if (random.nextStructureBoolean()) {
            node.right = generate(random, currentDepth + 1);
        }
        return node;
    }
}
