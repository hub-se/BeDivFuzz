package de.hub.se.bedivfuzz.junit.quickcheck.generator;

/**
 * A node in a binary tree.
 */
public class Node {
    int value;
    Node left;
    Node right;

    Node(int value) {
        this.value = value;
        right = null;
        left = null;
    }
}
