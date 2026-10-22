package dev.kotan.aeronautics_scripting.script;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ScriptEngine {
    public static final int MAX_SOURCE_LENGTH = 8_192;
    public static final int MAX_STATEMENTS = 64;
    public static final int MAX_DISPLAY_LINES = 8;
    public static final int MAX_EXPRESSION_DEPTH = 32;

    private static final Pattern VARIABLE_NAME = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");
    private static final Pattern SCREEN_STATEMENT =
        Pattern.compile("screen\\s+\"([^\"]{1,40})\"\\s*=\\s*(.+)");

    public ScriptExecutionResult execute(String source, Map<String, Double> inputs) {
        if (source == null) {
            throw new IllegalArgumentException("Script source must not be null");
        }
        if (source.length() > MAX_SOURCE_LENGTH) {
            throw new IllegalArgumentException("Script source exceeds " + MAX_SOURCE_LENGTH + " characters");
        }

        Map<String, Double> variables = new HashMap<>();
        inputs.forEach((name, value) -> {
            if (name == null || !VARIABLE_NAME.matcher(name).matches()) {
                throw new IllegalArgumentException("Invalid input variable name: " + name);
            }
            if (value == null || !Double.isFinite(value)) {
                throw new IllegalArgumentException("Input variable must be a finite number: " + name);
            }
            variables.put(name, value);
        });

        List<String> displayLines = new ArrayList<>();
        OptionalInt redstonePower = OptionalInt.empty();
        String[] lines = source.split("\\R", -1);
        int statementCount = 0;

        for (int index = 0; index < lines.length; index++) {
            String statement = lines[index].strip();
            if (statement.isEmpty() || statement.startsWith("#")) {
                continue;
            }
            if (++statementCount > MAX_STATEMENTS) {
                throw new ScriptException(index + 1, "Script exceeds " + MAX_STATEMENTS + " statements");
            }

            if (statement.startsWith("let ")) {
                int assignment = statement.indexOf('=');
                if (assignment < 0) {
                    throw new ScriptException(index + 1, "Expected 'let name = expression'");
                }
                String name = statement.substring(4, assignment).strip();
                if (!VARIABLE_NAME.matcher(name).matches()) {
                    throw new ScriptException(index + 1, "Invalid variable name: " + name);
                }
                variables.put(name, evaluate(statement.substring(assignment + 1), variables, index + 1));
                continue;
            }

            Matcher screenMatcher = SCREEN_STATEMENT.matcher(statement);
            if (screenMatcher.matches()) {
                if (displayLines.size() >= MAX_DISPLAY_LINES) {
                    throw new ScriptException(index + 1, "A screen can contain at most " + MAX_DISPLAY_LINES + " lines");
                }
                double value = evaluate(screenMatcher.group(2), variables, index + 1);
                displayLines.add(screenMatcher.group(1) + ": " + format(value));
                continue;
            }

            if (statement.startsWith("link")) {
                int assignment = statement.indexOf('=');
                if (assignment < 0 || !statement.substring(0, assignment).strip().equals("link")) {
                    throw new ScriptException(index + 1, "Expected 'link = expression'");
                }
                double value = evaluate(statement.substring(assignment + 1), variables, index + 1);
                redstonePower = OptionalInt.of((int) Math.round(Math.max(0, Math.min(value, 15))));
                continue;
            }

            throw new ScriptException(index + 1, "Unknown statement");
        }

        return new ScriptExecutionResult(displayLines, redstonePower, variables);
    }

    private static double evaluate(String expression, Map<String, Double> variables, int line) {
        try {
            double value = new ExpressionParser(expression, variables).parse();
            if (!Double.isFinite(value)) {
                throw new IllegalArgumentException("Expression result is not finite");
            }
            return value;
        } catch (IllegalArgumentException exception) {
            throw new ScriptException(line, exception.getMessage());
        }
    }

    private static String format(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    private static final class ExpressionParser {
        private final String source;
        private final Map<String, Double> variables;
        private int position;
        private int nestingDepth;

        private ExpressionParser(String source, Map<String, Double> variables) {
            this.source = source;
            this.variables = variables;
        }

        private double parse() {
            double value = parseComparison();
            skipWhitespace();
            if (position != source.length()) {
                throw error("Unexpected character '" + source.charAt(position) + "'");
            }
            return value;
        }

        private double parseComparison() {
            double left = parseAdditive();
            while (true) {
                skipWhitespace();
                if (consume(">=")) {
                    left = left >= parseAdditive() ? 1 : 0;
                } else if (consume("<=")) {
                    left = left <= parseAdditive() ? 1 : 0;
                } else if (consume("==")) {
                    left = left == parseAdditive() ? 1 : 0;
                } else if (consume("!=")) {
                    left = left != parseAdditive() ? 1 : 0;
                } else if (consume(">")) {
                    left = left > parseAdditive() ? 1 : 0;
                } else if (consume("<")) {
                    left = left < parseAdditive() ? 1 : 0;
                } else {
                    return left;
                }
            }
        }

        private double parseAdditive() {
            double value = parseMultiplicative();
            while (true) {
                skipWhitespace();
                if (consume("+")) {
                    value += parseMultiplicative();
                } else if (consume("-")) {
                    value -= parseMultiplicative();
                } else {
                    return value;
                }
            }
        }

        private double parseMultiplicative() {
            double value = parseUnary();
            while (true) {
                skipWhitespace();
                if (consume("*")) {
                    value *= parseUnary();
                } else if (consume("/")) {
                    double divisor = parseUnary();
                    if (divisor == 0) {
                        throw error("Division by zero");
                    }
                    value /= divisor;
                } else {
                    return value;
                }
                if (!Double.isFinite(value)) {
                    throw error("Expression result is not finite");
                }
            }
        }

        private double parseUnary() {
            skipWhitespace();
            boolean negative = false;
            while (consume("+") || consume("-")) {
                if (source.charAt(position - 1) == '-') {
                    negative = !negative;
                }
            }
            double value = parsePrimary();
            return negative ? -value : value;
        }

        private double parsePrimary() {
            skipWhitespace();
            if (consume("(")) {
                enterNesting();
                try {
                    double value = parseComparison();
                    skipWhitespace();
                    if (!consume(")")) {
                        throw error("Expected ')'");
                    }
                    return value;
                } finally {
                    nestingDepth--;
                }
            }

            if (position < source.length() && (Character.isDigit(source.charAt(position)) || source.charAt(position) == '.')) {
                return parseNumber();
            }

            String name = parseName();
            skipWhitespace();
            if (consume("(")) {
                enterNesting();
                try {
                    return parseFunction(name);
                } finally {
                    nestingDepth--;
                }
            }

            Double value = variables.get(name);
            if (value == null) {
                throw error("Unknown variable: " + name);
            }
            return value;
        }

        private double parseNumber() {
            int start = position;
            boolean decimalPoint = false;
            while (position < source.length()) {
                char character = source.charAt(position);
                if (Character.isDigit(character)) {
                    position++;
                } else if (character == '.' && !decimalPoint) {
                    decimalPoint = true;
                    position++;
                } else {
                    break;
                }
            }
            if (position < source.length() && (source.charAt(position) == 'e' || source.charAt(position) == 'E')) {
                position++;
                if (position < source.length() && (source.charAt(position) == '+' || source.charAt(position) == '-')) {
                    position++;
                }
                int exponentStart = position;
                while (position < source.length() && Character.isDigit(source.charAt(position))) {
                    position++;
                }
                if (position == exponentStart) {
                    throw error("Invalid number exponent");
                }
            }
            try {
                return Double.parseDouble(source.substring(start, position));
            } catch (NumberFormatException exception) {
                throw error("Invalid number");
            }
        }

        private String parseName() {
            skipWhitespace();
            int start = position;
            if (position >= source.length() || !isNameStart(source.charAt(position))) {
                throw error("Expected a number, variable, or function");
            }
            position++;
            while (position < source.length() && isNamePart(source.charAt(position))) {
                position++;
            }
            return source.substring(start, position);
        }

        private double parseFunction(String name) {
            List<Double> arguments = new ArrayList<>();
            skipWhitespace();
            if (!consume(")")) {
                do {
                    arguments.add(parseComparison());
                    skipWhitespace();
                } while (consume(","));
                if (!consume(")")) {
                    throw error("Expected ')' after function arguments");
                }
            }

            return switch (name) {
                case "abs" -> {
                    requireArgumentCount(name, arguments, 1);
                    yield Math.abs(arguments.get(0));
                }
                case "min" -> {
                    requireArgumentCount(name, arguments, 2);
                    yield Math.min(arguments.get(0), arguments.get(1));
                }
                case "max" -> {
                    requireArgumentCount(name, arguments, 2);
                    yield Math.max(arguments.get(0), arguments.get(1));
                }
                case "clamp" -> {
                    requireArgumentCount(name, arguments, 3);
                    if (arguments.get(1) > arguments.get(2)) {
                        throw error("clamp lower bound must not exceed upper bound");
                    }
                    yield Math.max(arguments.get(1), Math.min(arguments.get(0), arguments.get(2)));
                }
                default -> throw error("Unknown function: " + name);
            };
        }

        private void requireArgumentCount(String name, List<Double> arguments, int expected) {
            if (arguments.size() != expected) {
                throw error(name + " expects " + expected + " argument(s)");
            }
        }

        private boolean consume(String token) {
            if (source.startsWith(token, position)) {
                position += token.length();
                return true;
            }
            return false;
        }

        private void skipWhitespace() {
            while (position < source.length() && Character.isWhitespace(source.charAt(position))) {
                position++;
            }
        }

        private IllegalArgumentException error(String message) {
            return new IllegalArgumentException(message + " at column " + (position + 1));
        }

        private void enterNesting() {
            if (++nestingDepth > MAX_EXPRESSION_DEPTH) {
                nestingDepth--;
                throw error("Expression exceeds maximum nesting depth of " + MAX_EXPRESSION_DEPTH);
            }
        }

        private static boolean isNameStart(char character) {
            return Character.isLetter(character) || character == '_';
        }

        private static boolean isNamePart(char character) {
            return Character.isLetterOrDigit(character) || character == '_';
        }
    }
}
