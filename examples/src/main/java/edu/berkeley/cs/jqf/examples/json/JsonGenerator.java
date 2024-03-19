package edu.berkeley.cs.jqf.examples.json;


import java.util.Arrays;
import java.util.function.Function;
import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

public class JsonGenerator extends Generator<String> {

    public JsonGenerator() {
        super(String.class);
    }

    private GenerationStatus status;

    private static final int MAX_WS_DEPTH = 3;
    private static final int MAX_RECURSION_DEPTH = 20;
    private static final int MIN_MEMBERS_DEPTH = 10;
    private static final int MIN_CHAR_DEPTH = 10;
    private static final int MIN_ELEMENTS_DEPTH = 10;


    private int currentDepth;
    private int currentWhitespaceDepth;


    private static final String[] whitespaceVariants = {
            " ", "\n", "\r", "\t"
    };

    private static final String[] escapeVariants = {
            "\"", "\\", "/", "b", "f", "n", "r", "t", "u"
    };

    @Override
    public String generate(SourceOfRandomness random, GenerationStatus status) {
        this.status = status;
        this.currentDepth = 0;
        this.currentWhitespaceDepth = 0;
        return generateElement(random);
    }

    private String generateElement(SourceOfRandomness random) {
        String element = generateValue(random);
        String ws1 = generateWhitespace(random);
        String ws2 = generateWhitespace(random);
        return ws1 + element + ws2;

    }

    private String generateValue(SourceOfRandomness random) {
        return random.choose(Arrays.<Function<SourceOfRandomness, String>>asList(
                this::generateObject,
                this::generateArray,
                this::generateString,
                this::generateNumber,
                (rdm -> "true"),
                (rdm -> "false"),
                (rdm -> "null"))).apply(random);
    }

    private String generateObject(SourceOfRandomness random) {
        String object = random.choose(Arrays.<Function<SourceOfRandomness, String>>asList(
                this::generateWhitespace,
                this::generateMembers)).apply(random);

        return "{" + object + "}";
    }

    private String generateArray(SourceOfRandomness random) {
        String array = random.choose(Arrays.<Function<SourceOfRandomness, String>>asList(
                this::generateWhitespace,
                this::generateElements)).apply(random);

        return "[" + array + "]";
    }

    private String generateString(SourceOfRandomness random) {
        String result = generateCharacters(random);
        return '"' + result + '"';
    }

    private String generateNumber(SourceOfRandomness random) {
        String number = generateInteger(random);
        String fraction = generateFraction(random);
        String exponent = generateExponent(random);
        return number + fraction + exponent;

    }

    private String generateWhitespace(SourceOfRandomness random) {
        String whitespace;
        if (currentWhitespaceDepth >= MAX_WS_DEPTH || random.nextBoolean()) {
            whitespace = "";
        }
        else {
            currentWhitespaceDepth++;
            whitespace = random.choose(whitespaceVariants) + generateWhitespace(random);
            currentWhitespaceDepth--;
        }
        return whitespace;
    }

    private String generateMembers(SourceOfRandomness random) {
        String member;
        if ((currentDepth >= MAX_RECURSION_DEPTH || random.nextBoolean()) && currentDepth >= MIN_MEMBERS_DEPTH) {
            member = generateMember(random);
        }
        else {
            currentDepth++;
            member = generateMember(random) + "," + generateMembers(random);
            currentDepth--;
        }
        return member;
    }

    private String generateMember(SourceOfRandomness random) {
        String ws1 = generateWhitespace(random);
        String string = generateString(random);
        String ws2 = generateWhitespace(random);
        String element = generateElement(random);
        return ws1 + string + ws2 + ":" + element;
    }

    private String generateElements(SourceOfRandomness random) {
        String elements;
        if ((currentDepth >= MAX_RECURSION_DEPTH || random.nextBoolean()) && currentDepth >= MIN_ELEMENTS_DEPTH) {
            elements = generateElement(random);
        }
        else {
            currentDepth++;
            elements = generateElement(random) + "," + generateElements(random);
            currentDepth--;
        }
        return elements;
    }

    private String generateCharacters(SourceOfRandomness random) {
        String character;
        if ((currentDepth >= MAX_RECURSION_DEPTH || random.nextBoolean()) && currentDepth >= MIN_CHAR_DEPTH) {
            character = "";
        }
        else {
            currentDepth++;
            character = generateCharacter(random) + generateCharacters(random);
            currentDepth--;
        }
        return character;
    }

    private String generateCharacter(SourceOfRandomness random) {
        return random.choose(Arrays.<Function<SourceOfRandomness, String>>asList(
                this::generateUnicode,
                this::generateBackslashEscape)).apply(random);
    }

    private String generateUnicode(SourceOfRandomness random) {
        char randomChar;
        do {
            randomChar = (char) (random.nextInt(126 - 32) + 32);
        } while (randomChar == '"' || randomChar == '\\');
        return String.valueOf(randomChar);
    }

    private String generateBackslashEscape(SourceOfRandomness random) {
        return "\\" + generateEscape(random);
    }

    private String generateEscape(SourceOfRandomness random) {
        String escape = random.choose(escapeVariants);
        if ("u".equals(escape)) {
            String hex1 = generateHex(random);
            String hex2 = generateHex(random);
            String hex3 = generateHex(random);
            String hex4 = generateHex(random);
            return escape + hex1 + hex2 + hex3 + hex4;
        }
        return escape;

    }

    private String generateHex(SourceOfRandomness random) {
        return random.choose(Arrays.<Function<SourceOfRandomness, String>>asList(
                this::generateDigit,
                this::generateUpperCaseHex,
                this::generateLowerCaseHex)).apply(random);
    }

    private String generateDigit(SourceOfRandomness random) {
        return String.valueOf(random.nextInt(10));
    }

    private String generateUpperCaseHex(SourceOfRandomness random) {
        char randomChar = (char) ('A' + random.nextInt(6));
        return String.valueOf(randomChar);
    }

    private String generateLowerCaseHex(SourceOfRandomness random) {
        char randomChar = (char) ('a' + random.nextInt(6));
        return String.valueOf(randomChar);
    }

    private String generateOneNine(SourceOfRandomness random) {
        return String.valueOf(1 + random.nextInt(9));
    }

    private String generateInteger(SourceOfRandomness random) {
        switch (random.nextInt(4)) {
            case 0:
                return generateDigit(random);
            case 1:
                return generateOneNine(random) + generateDigits(random);
            case 2:
                return "-" + generateDigit(random);
            case 3:
                return "-" + generateOneNine(random) + generateDigits(random);
            default:
                return "";
        }
    }

    private String generateDigits(SourceOfRandomness random) {
        if (currentDepth >= MAX_RECURSION_DEPTH) {
            return generateDigit(random);
        }
        String digits;
        if (random.nextBoolean()) {
            digits = generateDigit(random);
        } else {
            currentDepth++;
            digits = generateDigit(random) + generateDigits(random);
            currentDepth--;
        }
        return digits;
    }

    private String generateFraction(SourceOfRandomness random) {
        String fraction;
        if (random.nextBoolean()) {
            fraction = "";
        } else {
            fraction = "." + generateDigits(random);
        }
        return fraction;
    }

    private String generateExponent(SourceOfRandomness random) {
        switch (random.nextInt(3)) {
            case 0:
                return "E" + generateSign(random) + generateDigits(random);
            case 1:
                return "e" + generateSign(random) + generateDigits(random);
            default:
                return "";
        }
    }

    private String generateSign(SourceOfRandomness random) {
        switch (random.nextInt(3)) {
            case 0:
                return "+";
            case 1:
                return "-";
            default:
                return "";
        }
    }
}
