package com.mathplotter.model;

import com.mathplotter.exceptions.MathParsingException;
import com.mathplotter.utils.MathParser;
import java.awt.Color;

public class Function {
    private final String expression;
    private transient MathParser parser;
    private transient java.util.function.Function<Double, Double> customEvaluator;
    private Color color;
    private boolean visible;

    public Function(String expression, Color color) throws MathParsingException {
        this.expression = expression;
        this.parser = new MathParser(expression);
        // We can do a test evaluation to catch syntax errors early
        this.parser.evaluate(1.0); 
        this.color = color;
        this.visible = true;
    }

    public static Function createDerivative(String expression, Color color, java.util.function.Function<Double, Double> customEvaluator) {
        return new Function(expression, color, customEvaluator);
    }

    // Private constructor for internal use cases like creating a derivative function
    private Function(String expression, Color color, java.util.function.Function<Double, Double> customEvaluator) {
        this.expression = expression;
        this.customEvaluator = customEvaluator;
        this.color = color;
        this.visible = true;
    }

    public double evaluate(double x) throws MathParsingException {
        if (customEvaluator != null) {
            return customEvaluator.apply(x);
        }
        return parser.evaluate(x);
    }

    @Override
    public String toString() {
        return this.expression;
    }

    // Getters and Setters
    public String getExpression() {
        return expression;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }
} 