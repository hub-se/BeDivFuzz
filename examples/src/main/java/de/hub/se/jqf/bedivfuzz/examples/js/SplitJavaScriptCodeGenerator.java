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
import de.hub.se.jqf.bedivfuzz.junit.quickcheck.SplitRandom;
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
    public String generate(SplitRandom random, GenerationStatus status) {
        this.status = status;
        this.identifiers = new HashSet<>();
        this.statementDepth = 0;
        this.expressionDepth = 0;
        return generateStatement(random).toString();
    }

    private static int sampleGeometric(SplitRandom random, double mean, boolean structural) {
        double p = 1 / mean;
        double uniform = structural ? random.nextStructureDouble() : random.nextValueDouble();
        return (int) ceil(log(1 - uniform) / log(1 - p));
    }

    private static <T> List<T> generateItems(Function<SplitRandom, T> generator, SplitRandom random,
                                             double mean) {
        int len = sampleGeometric(random, mean, true);
        List<T> items = new ArrayList<>(len);
        for (int i = 0; i < len; i++) {
            items.add(generator.apply(random));
        }
        return items;
    }

    private String generateExpression(SplitRandom random) {
        expressionDepth++;
        // Choose between terminal or non-terminal
        String result;
        if (expressionDepth >= MAX_EXPRESSION_DEPTH || random.nextStructureBoolean()) {
            result = random.chooseStructure(Arrays.<Function<SplitRandom, String>>asList(
                    this::generateLiteralNode,
                    this::generateIdentNode
            )).apply(random);
        } else {
            result = random.chooseStructure(Arrays.<Function<SplitRandom, String>>asList(
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

    private String generateStatement(SplitRandom random) {
        statementDepth++;
        String result;
        if (statementDepth >= MAX_STATEMENT_DEPTH || random.nextStructureBoolean()) {
            result = random.chooseStructure(Arrays.<Function<SplitRandom, String>>asList(
                    this::generateExpressionStatement,
                    this::generateBreakNode,
                    this::generateContinueNode,
                    this::generateReturnNode,
                    this::generateThrowNode,
                    this::generateVarNode,
                    this::generateEmptyNode
            )).apply(random);
        } else {
            result = random.chooseStructure(Arrays.<Function<SplitRandom, String>>asList(
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


    private String generateBinaryNode(SplitRandom random) {
        String token = random.chooseValue(BINARY_TOKENS);
        String lhs = generateExpression(random);
        String rhs = generateExpression(random);

        return lhs + " " + token + " " + rhs;
    }

    private String generateBlock(SplitRandom random) {
        return "{ " + String.join(";", generateItems(this::generateStatement, random, 4)) + " }";
    }

    private String generateBlockStatement(SplitRandom random) {
        return generateBlock(random);
    }

    private String generateBreakNode(SplitRandom random) {
        return "break";
    }

    private String generateCallNode(SplitRandom random) {
        String func = generateExpression(random);
        String args = String.join(",", generateItems(this::generateExpression, random, 3));

        String call = func + "(" + args + ")";
        if (random.nextValueBoolean()) {
            return call;
        } else {
            return "new " + call;
        }
    }

    private String generateCaseNode(SplitRandom random) {
        return "case " + generateExpression(random) + ": " +  generateBlock(random);
    }

    private String generateCatchNode(SplitRandom random) {
        return "catch (" + generateIdentNode(random) + ") " +
                generateBlock(random);
    }

    private String generateContinueNode(SplitRandom random) {
        return "continue";
    }

    private String generateEmptyNode(SplitRandom random) {
        return "";
    }

    private String generateExpressionStatement(SplitRandom random) {
        return generateExpression(random);
    }

    private String generateForNode(SplitRandom random) {
        String s = "for(";
        if (random.nextStructureBoolean()) {
            s += generateExpression(random);
        }
        s += ";";
        if (random.nextStructureBoolean()) {
            s += generateExpression(random);
        }
        s += ";";
        if (random.nextStructureBoolean()) {
            s += generateExpression(random);
        }
        s += ")";
        s += generateBlock(random);
        return s;
    }

    private String generateFunctionNode(SplitRandom random) {
        return "function(" + String.join(", ", generateItems(this::generateIdentNode, random, 5)) + ")" + generateBlock(random);
    }

    private String generateNamedFunctionNode(SplitRandom random) {
        return "function " + generateIdentNode(random) + "(" + String.join(", ", generateItems(this::generateIdentNode, random, 5)) + ")" + generateBlock(random);
    }

    private String generateArrowFunctionNode(SplitRandom random) {
        String params = "(" + String.join(", ", generateItems(this::generateIdentNode, random, 3)) + ")";
        if (random.nextStructureBoolean()) {
            return params + " => " + generateBlock(random);
        } else {
            return params + " => " + generateExpression(random);
        }

    }

    private String generateIdentNode(SplitRandom random) {
        // Either generate a new identifier or use an existing one
        String identifier;
        if (identifiers.isEmpty() || (identifiers.size() < MAX_IDENTIFIERS && random.nextStructureBoolean())) {
            identifier = random.nextValueChar('a', 'z') + "_" + identifiers.size();
            identifiers.add(identifier);
        } else {
            identifier = random.chooseValue(identifiers);
        }

        return identifier;
    }

    private String generateIfNode(SplitRandom random) {
        return "if (" +
                generateExpression(random) + ") " +
                generateBlock(random) +
                (random.nextStructureBoolean() ? " else " + generateBlock(random) : "");
    }

    private String generateIndexNode(SplitRandom random) {
        return generateExpression(random) + "[" + generateExpression(random) + "]";
    }

    private String generateObjectProperty(SplitRandom random) {
        return generateIdentNode(random) + ": " + generateExpression(random);
    }

    private String generateLiteralNode(SplitRandom random) {
        if (expressionDepth < MAX_EXPRESSION_DEPTH && random.nextStructureBoolean()) {
            if (random.nextStructureBoolean()) {
                // Array literal
                return "[" + String.join(", ", generateItems(this::generateExpression, random, 3)) + "]";
            } else {
                // Object literal
                return "{" + String.join(", ", generateItems(this::generateObjectProperty, random, 3)) + "}";

            }
        } else {
            return random.chooseStructure(Arrays.<Supplier<String>>asList(
                    () -> String.valueOf(random.nextValueInt(-10, 1000)),
                    () -> String.valueOf(random.nextValueBoolean()),
                    () -> '"' + new AsciiStringGenerator().generate(random.getValueDelegate(), status) + '"',
                    () -> "undefined",
                    () -> "null",
                    () -> "this"
            )).get();
        }
    }

    private String generatePropertyNode(SplitRandom random) {
        return generateExpression(random) + "." + generateIdentNode(random);
    }

    private String generateReturnNode(SplitRandom random) {
        return random.nextStructureBoolean() ? "return" : "return " + generateExpression(random);
    }

    private String generateSwitchNode(SplitRandom random) {
        return "switch(" + generateExpression(random) + ") {"
                + String.join(" ", generateItems(this::generateCaseNode, random, 2)) + "}";
    }

    private String generateTernaryNode(SplitRandom random) {
        return generateExpression(random) + " ? " + generateExpression(random) +
                " : " + generateExpression(random);
    }

    private String generateThrowNode(SplitRandom random) {
        return "throw " + generateExpression(random);
    }

    private String generateTryNode(SplitRandom random) {
        return "try " + generateBlock(random) + generateCatchNode(random);
    }

    private String generateUnaryNode(SplitRandom random) {
        String token = random.chooseValue(UNARY_TOKENS);
        return token + " " + generateExpression(random);
    }

    private String generateVarNode(SplitRandom random) {
        return "var " + generateIdentNode(random);
    }

    private String generateWhileNode(SplitRandom random) {
        return "while (" + generateExpression(random) + ")" + generateBlock(random);
    }
}
