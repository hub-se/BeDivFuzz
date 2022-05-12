/*
 * Copyright (c) 2017-2018 The Regents of the University of California
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
package de.hub.se.jqf.examples.js;

import java.util.*;
import java.util.function.*;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

import de.hub.se.jqf.examples.common.SplitAsciiStringGenerator;
import de.hub.se.jqf.fuzz.junit.quickcheck.NonTrackingSplitGenerationStatus;
import de.hub.se.jqf.fuzz.junit.quickcheck.SplitSourceOfRandomness;

/* Generates random strings that are syntactically valid JavaScript */
public class SplitJavaScriptCodeGenerator extends Generator<String> {
    public SplitJavaScriptCodeGenerator() {
        super(String.class); // Register type of generated object
    }

    private GenerationStatus status; // saved state object when generating

    private static int MAX_IDENTIFIERS = 100;
    private static int MAX_EXPRESSION_DEPTH = 10;
    private static int MAX_STATEMENT_DEPTH = 6;
    private static Set<String> identifiers; // Stores generated IDs, to promote re-use
    private int statementDepth; // Keeps track of how deep the AST is at any point
    private int expressionDepth; // Keeps track of how nested an expression is at any point

    private static final String[] UNARY_TOKENS = {
            "!", "++", "--", "~",
            "delete", "new", "typeof"
    };

    private static final String[] BINARY_TOKENS = {
            "!=", "!==", "%", "%=", "&", "&&", "&=", "*", "*=", "+", "+=", ",",
            "-", "-=", "/", "/=", "<", "<<", ">>=", "<=", "=", "==", "===",
            ">", ">=", ">>", ">>=", ">>>", ">>>=", "^", "^=", "|", "|=", "||",
            "in", "instanceof"
    };

    /** Main entry point. Called once per test case. Returns a random JS program. */
    @Override
    public String generate(SourceOfRandomness random, GenerationStatus status) {
        assert(status instanceof NonTrackingSplitGenerationStatus);
        this.status = status; // we save this so that we can pass it on to other generators
        this.identifiers = new HashSet<>();
        this.statementDepth = 0;
        this.expressionDepth = 0;

        SplitSourceOfRandomness rdm = (SplitSourceOfRandomness) random;
        return generateStatement(rdm).toString();
    }

    /** Utility method for generating a random list of items (e.g. statements, arguments, attributes) */
    private List<String> generateItems(Function<SplitSourceOfRandomness, String> genMethod, SplitSourceOfRandomness random,
                                       int max) {
        int len = random.nextInt(max + 1, true); // Generate random number in [0, mean*2)
        List<String> items = new ArrayList<>(len);
        for (int i = 0; i < len; i++) {
            items.add(genMethod.apply(random));
        }
        return items;
    }

    /** Generates a random JavaScript expression using recursive calls */
    private String generateExpression(SplitSourceOfRandomness random) {
        expressionDepth++;
        String result;
        // Choose terminal if nesting depth is too high or based on a random flip of a coin
        if (expressionDepth >= MAX_EXPRESSION_DEPTH || random.nextBoolean(true)) {
            result = random.choose(Arrays.<Function<SplitSourceOfRandomness, String>>asList(
                    this::generateLiteralNode,
                    this::generateIdentNode
            ), true).apply(random);
        } else {
            // Otherwise, choose a non-terminal generating function
            result = random.choose(Arrays.<Function<SplitSourceOfRandomness, String>>asList(
                    this::generateBinaryNode,
                    this::generateUnaryNode,
                    this::generateTernaryNode,
                    this::generateCallNode,
                    this::generateFunctionNode,
                    this::generatePropertyNode,
                    this::generateIndexNode,
                    this::generateArrowFunctionNode
            ), true).apply(random);
        }
        expressionDepth--;
        return "(" + result + ")";
    }

    /** Generates a random JavaScript statement */
    private String generateStatement(SplitSourceOfRandomness random) {
        statementDepth++;
        String result;
        // If depth is too high, then generate only simple statements to prevent infinite recursion
        // If not, generate simple statements after the flip of a coin
        if (statementDepth >= MAX_STATEMENT_DEPTH || random.nextBoolean(true)) {
            // Choose a random private method from this class, and then call it with `random`
            result = random.choose(Arrays.<Function<SplitSourceOfRandomness, String>>asList(
                    this::generateExpressionStatement,
                    this::generateBreakNode,
                    this::generateContinueNode,
                    this::generateReturnNode,
                    this::generateThrowNode,
                    this::generateVarNode,
                    this::generateEmptyNode
            ), true).apply(random);
        } else {
            // If depth is low and we won the flip, then generate compound statements
            // (that is, statements that contain other statements)
            result = random.choose(Arrays.<Function<SplitSourceOfRandomness, String>>asList(
                    this::generateIfNode,
                    this::generateForNode,
                    this::generateWhileNode,
                    this::generateNamedFunctionNode,
                    this::generateSwitchNode,
                    this::generateTryNode,
                    this::generateBlockStatement
            ), true).apply(random);
        }
        statementDepth--; // Reset statement depth when going up the recursive tree
        return result;
    }

    /** Generates a random binary expression (e.g. A op B) */
    private String generateBinaryNode(SplitSourceOfRandomness random) {
        String token = random.choose(BINARY_TOKENS, false); // Choose a binary operator at random
        String lhs = generateExpression(random);
        String rhs = generateExpression(random);

        return lhs + " " + token + " " + rhs;
    }

    /** Generates a block of statements delimited by ';' and enclosed by '{' '}' */
    private String generateBlock(SplitSourceOfRandomness random) {
        return "{ " + String.join(";", generateItems(this::generateStatement, random, 4)) + " }";
    }

    private String generateBlockStatement(SplitSourceOfRandomness random) {
        return generateBlock(random);
    }

    private String generateBreakNode(SplitSourceOfRandomness random) {
        return "break";
    }

    private String generateCallNode(SplitSourceOfRandomness random) {
        String func = generateExpression(random);
        String args = String.join(",", generateItems(this::generateExpression, random, 3));

        String call = func + "(" + args + ")";
        if (random.nextBoolean(false)) {
            return call;
        } else {
            return "new " + call;
        }
    }

    private String generateCaseNode(SplitSourceOfRandomness random) {
        return "case " + generateExpression(random) + ": " +  generateBlock(random);
    }

    private String generateCatchNode(SplitSourceOfRandomness random) {
        return "catch (" + generateIdentNode(random) + ") " +
                generateBlock(random);
    }

    private String generateContinueNode(SplitSourceOfRandomness random) {
        return "continue";
    }

    private String generateEmptyNode(SplitSourceOfRandomness random) {
        return "";
    }

    private String generateExpressionStatement(SplitSourceOfRandomness random) {
        return generateExpression(random);
    }

    private String generateForNode(SplitSourceOfRandomness random) {
        String s = "for(";
        if (random.nextBoolean(true)) {
            s += generateExpression(random);
        }
        s += ";";
        if (random.nextBoolean(true)) {
            s += generateExpression(random);
        }
        s += ";";
        if (random.nextBoolean(true)) {
            s += generateExpression(random);
        }
        s += ")";
        s += generateBlock(random);
        return s;
    }

    private String generateFunctionNode(SplitSourceOfRandomness random) {
        return "function(" + String.join(", ", generateItems(this::generateIdentNode, random, 5)) + ")" + generateBlock(random);
    }

    private String generateNamedFunctionNode(SplitSourceOfRandomness random) {
        return "function " + generateIdentNode(random) + "(" + String.join(", ", generateItems(this::generateIdentNode, random, 5)) + ")" + generateBlock(random);
    }

    private String generateArrowFunctionNode(SplitSourceOfRandomness random) {
        String params = "(" + String.join(", ", generateItems(this::generateIdentNode, random, 3)) + ")";
        if (random.nextBoolean(true)) {
            return params + " => " + generateBlock(random);
        } else {
            return params + " => " + generateExpression(random);
        }

    }

    private String generateIdentNode(SplitSourceOfRandomness random) {
        // Either generate a new identifier or use an existing one
        String identifier;
        if (identifiers.isEmpty() || (identifiers.size() < MAX_IDENTIFIERS && random.nextBoolean(true))) {
            identifier = random.nextChar('a', 'z', false) + "_" + identifiers.size();
            identifiers.add(identifier);
        } else {
            identifier = random.choose(identifiers, false);
        }

        return identifier;
    }

    private String generateIfNode(SplitSourceOfRandomness random) {
        return "if (" +
                generateExpression(random) + ") " +
                generateBlock(random) +
                (random.nextBoolean(true) ? generateBlock(random) : "");
    }

    private String generateIndexNode(SplitSourceOfRandomness random) {
        return generateExpression(random) + "[" + generateExpression(random) + "]";
    }

    private String generateObjectProperty(SplitSourceOfRandomness random) {
        return generateIdentNode(random) + ": " + generateExpression(random);
    }

    private String generateLiteralNode(SplitSourceOfRandomness random) {
        // If we are not too deeply nested, then it is okay to generate array/object literals
        if (expressionDepth < MAX_EXPRESSION_DEPTH && random.nextBoolean(true)) {
            if (random.nextBoolean(true)) {
                // Array literal
                return "[" + String.join(", ", generateItems(this::generateExpression, random, 3)) + "]";
            } else {
                // Object literal
                return "{" + String.join(", ", generateItems(this::generateObjectProperty, random, 3)) + "}";

            }
        } else {
            // Otherwise, generate primitive literals
            return random.choose(Arrays.<Supplier<String>>asList(
                    () -> {
                        return String.valueOf(random.nextInt(-10, 1000, false));
                    }, // int literal
                    () -> {
                        return String.valueOf(random.nextBoolean(false));
                    },      // bool literal
                    () -> '"' + new SplitAsciiStringGenerator().generate(random, status) + '"',
                    () -> "undefined",
                    () -> "null",
                    () -> "this"
            ), true).get();
        }
    }

    private String generateStringLiteral(SplitSourceOfRandomness random) {
        // Generate an arbitrary string using the default string generator, and quote it
        String result = '"' + gen().type(String.class).generate(random.getSecondarySource(), status) + '"';
        return result;
    }

    private String generatePropertyNode(SplitSourceOfRandomness random) {
        return generateExpression(random) + "." + generateIdentNode(random);
    }

    private String generateReturnNode(SplitSourceOfRandomness random) {
        return random.nextBoolean(true) ? "return" : "return " + generateExpression(random);
    }

    private String generateSwitchNode(SplitSourceOfRandomness random) {
        return "switch(" + generateExpression(random) + ") {"
                + String.join(" ", generateItems(this::generateCaseNode, random, 2)) + "}";
    }

    private String generateTernaryNode(SplitSourceOfRandomness random) {
        return generateExpression(random) + " ? " + generateExpression(random) +
                " : " + generateExpression(random);
    }

    private String generateThrowNode(SplitSourceOfRandomness random) {
        return "throw " + generateExpression(random);
    }

    private String generateTryNode(SplitSourceOfRandomness random) {
        return "try " + generateBlock(random) + generateCatchNode(random);
    }

    private String generateUnaryNode(SplitSourceOfRandomness random) {
        String token = random.choose(UNARY_TOKENS, false);
        return token + " " + generateExpression(random);
    }

    private String generateVarNode(SplitSourceOfRandomness random) {
        return "var " + generateIdentNode(random);
    }

    private String generateWhileNode(SplitSourceOfRandomness random) {
        return "while (" + generateExpression(random) + ")" + generateBlock(random);
    }

    private int nextInt(SplitSourceOfRandomness random, int bound) {
        return random.nextInt(bound);
    }

    private int nextInt(SplitSourceOfRandomness random, int lower, int upper) {
        return random.nextInt(lower, upper);
    }

    private boolean nextBoolean(SplitSourceOfRandomness random) {
        return random.nextBoolean();
    }

    private char nextChar(SplitSourceOfRandomness random, char min, char max) {
        return random.nextChar(min, max);
    }

    private <T> T choose(SplitSourceOfRandomness random, Collection<T> items) {
        return random.choose(items);
    }

    private <T> T choose(SplitSourceOfRandomness random, T[] items) {
        return items[random.nextInt(items.length)];
    }

}

