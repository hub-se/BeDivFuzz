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
package de.hub.se.jqf.bedivfuzz.examples.js;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import de.hub.se.jqf.bedivfuzz.junit.quickcheck.SplitGenerator;
import de.hub.se.jqf.bedivfuzz.junit.quickcheck.SplitSourceOfRandomness;
import edu.berkeley.cs.jqf.examples.common.AsciiStringGenerator;

import static java.lang.Math.ceil;
import static java.lang.Math.log;

/**
 * A generator producing syntactically valid JavaScript using structural and value random choices,
 * based on {@linkplain edu.berkeley.cs.jqf.examples.js.JavaScriptCodeGenerator JavaScriptCodeGenerator}.
 */
public class SplitJavaScriptCodeGenerator extends SplitGenerator<String> {

    public SplitJavaScriptCodeGenerator() {
        super(String.class);
    }

    private GenerationStatus status;

    private static final int MAX_IDENTIFIERS = 100;
    private static final int MAX_EXPRESSION_DEPTH = 10;
    private static final int MAX_STATEMENT_DEPTH = 6;
    private static Set<String> identifiers;
    private int statementDepth;
    private int expressionDepth;


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

    @Override
    public String generate(SplitSourceOfRandomness random, GenerationStatus status) {
        this.status = status;
        this.identifiers = new HashSet<>();
        this.statementDepth = 0;
        this.expressionDepth = 0;
        return generateStatement(random).toString();
    }

    private static int sampleGeometric(SplitSourceOfRandomness random, double mean, boolean structural) {
        double p = 1 / mean;
        double uniform = structural ? random.structure.nextDouble() : random.value.nextDouble();
        return (int) ceil(log(1 - uniform) / log(1 - p));
    }

    private static <T> List<T> generateItems(Function<SplitSourceOfRandomness, T> generator, SplitSourceOfRandomness random,
                                             double mean) {
        int len = sampleGeometric(random, mean, true);
        List<T> items = new ArrayList<>(len);
        for (int i = 0; i < len; i++) {
            items.add(generator.apply(random));
        }
        return items;
    }

    private String generateExpression(SplitSourceOfRandomness random) {
        expressionDepth++;
        // Choose between terminal or non-terminal
        String result;
        if (expressionDepth >= MAX_EXPRESSION_DEPTH || random.structure.nextBoolean()) {
            result = random.structure.choose(Arrays.<Function<SplitSourceOfRandomness, String>>asList(
                    this::generateLiteralNode,
                    this::generateIdentNode
            )).apply(random);
        } else {
            result = random.structure.choose(Arrays.<Function<SplitSourceOfRandomness, String>>asList(
                    this::generateBinaryNode,
                    this::generateUnaryNode,
                    this::generateTernaryNode,
                    this::generateCallNode,
                    this::generateFunctionNode,
                    this::generatePropertyNode,
                    this::generateIndexNode,
                    this::generateArrowFunctionNode
            )).apply(random);
        }
        expressionDepth--;
        return "(" + result + ")";
    }

    private String generateStatement(SplitSourceOfRandomness random) {
        statementDepth++;
        String result;
        if (statementDepth >= MAX_STATEMENT_DEPTH || random.structure.nextBoolean()) {
            result = random.structure.choose(Arrays.<Function<SplitSourceOfRandomness, String>>asList(
                    this::generateExpressionStatement,
                    this::generateBreakNode,
                    this::generateContinueNode,
                    this::generateReturnNode,
                    this::generateThrowNode,
                    this::generateVarNode,
                    this::generateEmptyNode
            )).apply(random);
        } else {
            result = random.structure.choose(Arrays.<Function<SplitSourceOfRandomness, String>>asList(
                    this::generateIfNode,
                    this::generateForNode,
                    this::generateWhileNode,
                    this::generateNamedFunctionNode,
                    this::generateSwitchNode,
                    this::generateTryNode,
                    this::generateBlockStatement
            )).apply(random);
        }
        statementDepth--;
        return result;
    }


    private String generateBinaryNode(SplitSourceOfRandomness random) {
        String token = random.value.choose(BINARY_TOKENS);
        String lhs = generateExpression(random);
        String rhs = generateExpression(random);

        return lhs + " " + token + " " + rhs;
    }

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
        if (random.value.nextBoolean()) {
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
        if (random.structure.nextBoolean()) {
            s += generateExpression(random);
        }
        s += ";";
        if (random.structure.nextBoolean()) {
            s += generateExpression(random);
        }
        s += ";";
        if (random.structure.nextBoolean()) {
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
        if (random.structure.nextBoolean()) {
            return params + " => " + generateBlock(random);
        } else {
            return params + " => " + generateExpression(random);
        }

    }

    private String generateIdentNode(SplitSourceOfRandomness random) {
        // Either generate a new identifier or use an existing one
        String identifier;
        if (identifiers.isEmpty() || (identifiers.size() < MAX_IDENTIFIERS && random.structure.nextBoolean())) {
            identifier = random.value.nextChar('a', 'z') + "_" + identifiers.size();
            identifiers.add(identifier);
        } else {
            identifier = random.value.choose(identifiers);
        }

        return identifier;
    }

    private String generateIfNode(SplitSourceOfRandomness random) {
        return "if (" +
                generateExpression(random) + ") " +
                generateBlock(random) +
                (random.structure.nextBoolean() ? " else " + generateBlock(random) : "");
    }

    private String generateIndexNode(SplitSourceOfRandomness random) {
        return generateExpression(random) + "[" + generateExpression(random) + "]";
    }

    private String generateObjectProperty(SplitSourceOfRandomness random) {
        return generateIdentNode(random) + ": " + generateExpression(random);
    }

    private String generateLiteralNode(SplitSourceOfRandomness random) {
        if (expressionDepth < MAX_EXPRESSION_DEPTH && random.structure.nextBoolean()) {
            if (random.structure.nextBoolean()) {
                // Array literal
                return "[" + String.join(", ", generateItems(this::generateExpression, random, 3)) + "]";
            } else {
                // Object literal
                return "{" + String.join(", ", generateItems(this::generateObjectProperty, random, 3)) + "}";

            }
        } else {
            return random.structure.choose(Arrays.<Supplier<String>>asList(
                    () -> String.valueOf(random.value.nextInt(-10, 1000)),
                    () -> String.valueOf(random.value.nextBoolean()),
                    () -> '"' + new AsciiStringGenerator().generate(random.value, status) + '"',
                    () -> "undefined",
                    () -> "null",
                    () -> "this"
            )).get();
        }
    }

    private String generatePropertyNode(SplitSourceOfRandomness random) {
        return generateExpression(random) + "." + generateIdentNode(random);
    }

    private String generateReturnNode(SplitSourceOfRandomness random) {
        return random.structure.nextBoolean() ? "return" : "return " + generateExpression(random);
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
        String token = random.value.choose(UNARY_TOKENS);
        return token + " " + generateExpression(random);
    }

    private String generateVarNode(SplitSourceOfRandomness random) {
        return "var " + generateIdentNode(random);
    }

    private String generateWhileNode(SplitSourceOfRandomness random) {
        return "while (" + generateExpression(random) + ")" + generateBlock(random);
    }
}
