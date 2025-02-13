package de.hub.se.bedivfuzz.junit.quickcheck.generator;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import de.hub.se.bedivfuzz.junit.quickcheck.SplitGenerator;
import de.hub.se.bedivfuzz.junit.quickcheck.SplitRandom;

/**
 * A generator that produces binary trees.
 */
public class SplitBinaryTreeGenerator extends SplitGenerator<Node> {
    private final int MAX_DEPTH = 5;

    public SplitBinaryTreeGenerator() {
        super(Node.class);
    }

    @Override
    public Node generate(SplitRandom random, GenerationStatus status) {
        return generate(random, 0);
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
