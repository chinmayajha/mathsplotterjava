package com.mathplotter.utils;

import com.mathplotter.exceptions.MathParsingException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class MathParser {

    private final String expression;
    private int pos = -1, ch;

    private final Map<String, Double> variables = new HashMap<>();
    private final Map<String, Function<Double, Double>> functions = new HashMap<>();

    public MathParser(String expression) {
        this.expression = expression.replaceAll("\\s+", "");
        addDefaultFunctions();
        variables.put("pi", Math.PI);
        variables.put("e", Math.E);
    }

    private void addDefaultFunctions() {
        functions.put("sin", Math::sin);
        functions.put("cos", Math::cos);
        functions.put("tan", Math::tan);
        functions.put("sqrt", Math::sqrt);
        functions.put("abs", Math::abs);
        functions.put("log", Math::log);
        functions.put("log10", Math::log10);
        functions.put("exp", Math::exp);
        functions.put("asin", Math::asin);
        functions.put("acos", Math::acos);
        functions.put("atan", Math::atan);
        functions.put("ceil", Math::ceil);
        functions.put("floor", Math::floor);
    }

    void nextChar() {
        ch = (++pos < expression.length()) ? expression.charAt(pos) : -1;
    }

    boolean eat(int charToEat) {
        // No need to skip spaces here anymore
        if (ch == charToEat) {
            nextChar();
            return true;
        }
        return false;
    }

    public double evaluate(double xValue) throws MathParsingException {
        variables.put("x", xValue);
        pos = -1;
        nextChar();
        double result = parseExpression();
        if (pos < expression.length()) {
            throw new MathParsingException("Unexpected: " + (char) ch);
        }
        return result;
    }

    // Grammar:
    // expression = term | expression `+` term | expression `-` term
    // term = factor | term `*` factor | term `/` factor
    // factor = `+` factor | `-` factor | `(` expression `)` | number
    //        | functionName factor | factor `^` factor

    double parseExpression() throws MathParsingException {
        double x = parseTerm();
        for (;;) {
            if (eat('+')) x += parseTerm(); // addition
            else if (eat('-')) x -= parseTerm(); // subtraction
            else return x;
        }
    }

    double parseTerm() throws MathParsingException {
        double x = parseFactor();
        for (;;) {
            if (eat('*')) x *= parseFactor(); // multiplication
            else if (eat('/')) x /= parseFactor(); // division
            else return x;
        }
    }

    double parseFactor() throws MathParsingException {
        if (eat('+')) return parseFactor(); // unary plus
        if (eat('-')) return -parseFactor(); // unary minus

        double x;
        int startPos = this.pos;
        if (eat('(')) { // parentheses
            x = parseExpression();
            if (!eat(')')) throw new MathParsingException("Missing ')'");
        } else if ((ch >= '0' && ch <= '9') || ch == '.') { // numbers
            while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
            x = Double.parseDouble(expression.substring(startPos, this.pos));
        } else if (ch >= 'a' && ch <= 'z') { // functions & variables
            while (ch >= 'a' && ch <= 'z') nextChar();
            String funcOrVar = expression.substring(startPos, this.pos);
            if (variables.containsKey(funcOrVar)) {
                x = variables.get(funcOrVar);
            } else if (functions.containsKey(funcOrVar)) {
                // Check for function arguments in parentheses
                if (eat('(')) {
                    double arg1 = parseExpression();
                    if (eat(',')) {
                        // Function with two arguments
                        double arg2 = parseExpression();
                        if (!eat(')')) throw new MathParsingException("Missing ')' after function arguments");
                        if (funcOrVar.equals("log")) {
                            // log(base, x) = Math.log(x) / Math.log(base)
                            x = Math.log(arg2) / Math.log(arg1);
                        } else {
                            throw new MathParsingException("Function '" + funcOrVar + "' does not support two arguments");
                        }
                    } else {
                        if (!eat(')')) throw new MathParsingException("Missing ')' after function argument");
                        // Single-argument function
                        x = functions.get(funcOrVar).apply(arg1);
                    }
                } else {
                    // Function without parentheses, e.g., sinx
                    x = functions.get(funcOrVar).apply(parseFactor());
                }
            } else {
                throw new MathParsingException("Unknown function or variable: " + funcOrVar);
            }
        } else {
            throw new MathParsingException("Unexpected: " + (char)ch);
        }

        if (eat('^')) x = Math.pow(x, parseFactor()); // exponentiation

        return x;
    }
} 