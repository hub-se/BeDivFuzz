package de.hub.se.jqf.bedivfuzz.examples.json;


import java.util.Arrays;
import java.util.function.Function;
import com.pholser.junit.quickcheck.generator.GenerationStatus;
import de.hub.se.jqf.bedivfuzz.junit.quickcheck.SplitGenerator;
import de.hub.se.jqf.bedivfuzz.junit.quickcheck.SplitRandom;

public class SplitJsonGenerator extends SplitGenerator<String> {

    public SplitJsonGenerator() {
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
            "\"", "\\", "/", "b", "f", "n", "r", "t"
    };

    @Override
    public String generate(SplitRandom random, GenerationStatus status) {
        this.status = status;
        this.currentDepth = 0;
        this.currentWhitespaceDepth = 0;
        return generateElement(random);
    }

    private String generateElement(SplitRandom random) {
        String element = generateValue(random);
        String ws1 = generateWhitespace(random);
        String ws2 = generateWhitespace(random);
        return ws1 + element + ws2;

    }

    private String generateValue(SplitRandom random) {
        return random.chooseStructure(Arrays.<Function<SplitRandom, String>>asList(
                this::generateObject,
                this::generateArray,
                this::generateString,
                this::generateNumber,
                (rdm -> "true"),
                (rdm -> "false"),
                (rdm -> "null"))).apply(random);
    }

    private String generateObject(SplitRandom random) {
        String object = random.chooseStructure(Arrays.<Function<SplitRandom, String>>asList(
                this::generateWhitespace,
                this::generateMembers)).apply(random);

        return "{" + object + "}";
    }

    private String generateArray(SplitRandom random) {
        String array = random.chooseStructure(Arrays.<Function<SplitRandom, String>>asList(
                this::generateWhitespace,
                this::generateElements)).apply(random);

        return "[" + array + "]";
    }

    private String generateString(SplitRandom random) {
        String result = generateCharacters(random);
        return '"' + result + '"';
    }

    private String generateNumber(SplitRandom random) {
        String number = generateInteger(random);
        String fraction = generateFraction(random);
        String exponent = generateExponent(random);
        return number + fraction + exponent;

    }

    private String generateWhitespace(SplitRandom random) {
        String whitespace;
        if (currentWhitespaceDepth >= MAX_WS_DEPTH || random.nextStructureBoolean()) {
            whitespace = "";
        }
        else {
            currentWhitespaceDepth++;
            whitespace = random.chooseValue(whitespaceVariants) + generateWhitespace(random);
            currentWhitespaceDepth--;
        }
        return whitespace;
    }

    private String generateMembers(SplitRandom random) {
        String member;
        if ((currentDepth >= MAX_RECURSION_DEPTH || random.nextStructureBoolean()) && currentDepth >= MIN_MEMBERS_DEPTH) {
            member = generateMember(random);
        }
        else {
            currentDepth++;
            member = generateMember(random) + "," + generateMembers(random);
            currentDepth--;
        }
        return member;
    }

    private String generateMember(SplitRandom random) {
        String ws1 = generateWhitespace(random);
        String string = generateString(random);
        String ws2 = generateWhitespace(random);
        String element = generateElement(random);
        return ws1 + string + ws2 + ":" + element;
    }

    private String generateElements(SplitRandom random) {
        String elements;
        if ((currentDepth >= MAX_RECURSION_DEPTH || random.nextStructureBoolean()) && currentDepth >= MIN_ELEMENTS_DEPTH) {
            elements = generateElement(random);
        }
        else {
            currentDepth++;
            elements = generateElement(random) + "," + generateElements(random);
            currentDepth--;
        }
        return elements;
    }

    private String generateCharacters(SplitRandom random) {
        String character;
        if ((currentDepth >= MAX_RECURSION_DEPTH || random.nextStructureBoolean()) && currentDepth >= MIN_CHAR_DEPTH) {
            character = "";
        }
        else {
            currentDepth++;
            character = generateCharacter(random) + generateCharacters(random);
            currentDepth--;
        }
        return character;
    }

    private String generateCharacter(SplitRandom random) {
        return random.chooseStructure(Arrays.<Function<SplitRandom, String>>asList(
                this::generateUnicode,
                this::generateBackslashEscape)).apply(random);
    }

    private String generateUnicode(SplitRandom random) {
        char randomChar;
        do {
            randomChar = (char) (random.nextValueInt(126 - 32) + 32);
        } while (randomChar == '"' || randomChar == '\\');
        return String.valueOf(randomChar);
    }

    private String generateBackslashEscape(SplitRandom random) {
        return "\\" + generateEscape(random);
    }

    private String generateEscape(SplitRandom random) {
        if (random.nextStructureInt(escapeVariants.length + 1) == 0) {
            String hex1 = generateHex(random);
            String hex2 = generateHex(random);
            String hex3 = generateHex(random);
            String hex4 = generateHex(random);
            return "u" + hex1 + hex2 + hex3 + hex4;
        } else {
            return random.chooseValue(escapeVariants);
        }

    }

    private String generateHex(SplitRandom random) {
        return random.chooseStructure(Arrays.<Function<SplitRandom, String>>asList(
                this::generateDigit,
                this::generateUpperCaseHex,
                this::generateLowerCaseHex)).apply(random);
    }

    private String generateDigit(SplitRandom random) {
        return String.valueOf(random.nextValueInt(10));
    }

    private String generateUpperCaseHex(SplitRandom random) {
        char randomChar = (char) ('A' + random.nextValueInt(6));
        return String.valueOf(randomChar);
    }

    private String generateLowerCaseHex(SplitRandom random) {
        char randomChar = (char) ('a' + random.nextValueInt(6));
        return String.valueOf(randomChar);
    }

    private String generateOneNine(SplitRandom random) {
        return String.valueOf(1 + random.nextValueInt(9));
    }

    private String generateInteger(SplitRandom random) {
        switch (random.nextStructureInt(4)) {
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

    private String generateDigits(SplitRandom random) {
        if (currentDepth >= MAX_RECURSION_DEPTH) {
            return generateDigit(random);
        }
        String digits;
        if (random.nextStructureBoolean()) {
            digits = generateDigit(random);
        } else {
            currentDepth++;
            digits = generateDigit(random) + generateDigits(random);
            currentDepth--;
        }
        return digits;
    }

    private String generateFraction(SplitRandom random) {
        String fraction;
        if (random.nextStructureBoolean()) {
            fraction = "";
        } else {
            fraction = "." + generateDigits(random);
        }
        return fraction;
    }

    private String generateExponent(SplitRandom random) {
        switch (random.nextValueInt(3)) {
            case 0:
                return "E" + generateSign(random) + generateDigits(random);
            case 1:
                return "e" + generateSign(random) + generateDigits(random);
            default:
                return "";
        }
    }

    private String generateSign(SplitRandom random) {
        switch (random.nextValueInt(3)) {
            case 0:
                return "+";
            case 1:
                return "-";
            default:
                return "";
        }
    }
}
