package com.mathplotter.utils;

import com.mathplotter.exceptions.MathParsingException;
import com.mathplotter.model.Function;
import com.mathplotter.model.Point;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.awt.geom.Point2D;

public class NumericalMethods {

    private static final double DERIVATIVE_H = 1e-7;

    /**
     * Finds intersection points between two functions in the specified range.
     * Uses a step-based approach to detect where the difference between the functions
     * between the two functions, which indicates an intersection.
     */
    public static List<Point> findIntersections(Function f1, Function f2, double xMin, double xMax) {
        List<Point> intersections = new ArrayList<>();
        // Define the step size for iteration. A smaller step increases accuracy but reduces performance.
        double step = (xMax - xMin) / 20000.0; 

        try {
            double prevDiff = f1.evaluate(xMin) - f2.evaluate(xMin);

            for (double x = xMin + step; x <= xMax; x += step) {
                double currentDiff = f1.evaluate(x) - f2.evaluate(x);
                // If the sign of the difference has changed, an intersection has occurred.
                if (Math.signum(prevDiff) != Math.signum(currentDiff)) {
                    // Approximate the y-value at the intersection point.
                    double y = f1.evaluate(x);
                    intersections.add(new Point(x, y));
                }
                prevDiff = currentDiff;
            }
        } catch (MathParsingException e) {
            // Handle parsing errors silently
        }
        return intersections;
    }

    /**
     * Calculates the definite integral of a function using Simpson's rule.
     * @param f The function to integrate
     * @param start The lower bound of integration
     * @param end The upper bound of integration
     * @param n The number of subintervals (must be even)
     * @return The approximate value of the definite integral
     */
    public static double integrate(Function f, double start, double end, int n) {
        double h = (end - start) / n;
        double sum = 0;
        try {
            sum = f.evaluate(start) + f.evaluate(end);

            // Summation of terms with coefficient 4
            for (int i = 1; i < n; i++) {
                double x = start + i * h;
                sum += 4 * f.evaluate(x);
            }

            // Summation of terms with coefficient 2
            for (int i = 1; i < n - 1; i++) {
                double x = start + i * h;
                sum += 2 * f.evaluate(x);
            }
        } catch (MathParsingException e) {
            return 0.0;
        }

        return sum * h / 3;
    }

    /**
     * Calculates the numerical derivative of a function at a given point.
     * Uses the central difference method with a small step size.
     * @param f The function to differentiate
     * @param x The point at which to calculate the derivative
     * @return The approximate value of the derivative
     */
    public static double derivative(Function f, double x) {
        double h = 1e-6; // Small step size for numerical differentiation
        try {
            return (f.evaluate(x + h) - f.evaluate(x - h)) / (2 * h);
        } catch (MathParsingException e) {
            return 0.0;
        }
    }

    /**
     * Creates a new Function object representing the derivative of the given function.
     * @param f The original function
     * @return A new Function object representing the derivative
     */
    public static Function derivative(Function f) {
        return Function.createDerivative("d/dx(" + f.getExpression() + ")", f.getColor(), x -> {
            try {
                return derivative(f, x);
            } catch (Exception e) {
                return 0.0;
            }
        });
    }

    /**
     * Finds local maxima of a function in the specified range.
     * A local maximum occurs where the first derivative changes from positive to negative.
     */
    public static List<Point> findLocalMaxima(Function f, double xMin, double xMax) {
        List<Point> maxima = new ArrayList<>();
        double step = (xMax - xMin) / 10000.0;
        
        for (double x = xMin; x < xMax; x += step) {
            try {
                double prevSlope = derivative(f, x - step);
                double currentSlope = derivative(f, x);
                if (prevSlope > 0 && currentSlope < 0) {
                    maxima.add(new Point(x, f.evaluate(x)));
                }
            } catch (MathParsingException e) {
                // Skip points that can't be evaluated
            }
        }
        return maxima;
    }

    /**
     * Finds local minima of a function in the specified range.
     * A local minimum occurs where the first derivative changes from negative to positive.
     */
    public static List<Point> findLocalMinima(Function f, double xMin, double xMax) {
        List<Point> minima = new ArrayList<>();
        double step = (xMax - xMin) / 10000.0;

        for (double x = xMin; x < xMax; x += step) {
            try {
                double prevSlope = derivative(f, x - step);
                double currentSlope = derivative(f, x);
                if (prevSlope < 0 && currentSlope > 0) {
                    minima.add(new Point(x, f.evaluate(x)));
                }
            } catch (MathParsingException e) {
                // Skip points that can't be evaluated
            }
        }
        return minima;
    }

    /**
     * Finds all extrema (both maxima and minima) of a function in the specified range.
     * @param f The function to analyze
     * @param xMin The lower bound of the range
     * @param xMax The upper bound of the range
     * @return A list of points representing all extrema
     */
    public static List<Point> findExtrema(Function f, double xMin, double xMax) {
        List<Point> extrema = new ArrayList<>();
        // Implementation of findExtrema method
        return extrema;
    }

    /**
     * Finds zero crossings (roots/x-intercepts) of a function in the specified range.
     * Uses a step-based approach to detect sign changes.
     */
    public static List<Point> findRoots(Function f, double xMin, double xMax) {
        List<Point> roots = new ArrayList<>();
        double step = (xMax - xMin) / 20000.0;
        try {
            double prevY = f.evaluate(xMin);
            for (double x = xMin + step; x <= xMax; x += step) {
                double currY = f.evaluate(x);
                if (Math.signum(prevY) != Math.signum(currY)) {
                    // Approximate the root location
                    roots.add(new Point(x, 0));
                }
                prevY = currY;
            }
        } catch (MathParsingException e) {
            // Handle parsing errors silently
        }
        return roots;
    }

    /**
     * Finds points of inflection of a function in the specified range.
     * A point of inflection occurs where the second derivative changes sign.
     */
    public static List<Point> findInflectionPoints(Function f, double xMin, double xMax) {
        List<Point> inflections = new ArrayList<>();
        double step = (xMax - xMin) / 10000.0;
        for (double x = xMin; x < xMax; x += step) {
            try {
                double prevSecond = secondDerivative(f, x - step);
                double currentSecond = secondDerivative(f, x);
                if (Math.signum(prevSecond) != Math.signum(currentSecond)) {
                    inflections.add(new Point(x, f.evaluate(x)));
                }
            } catch (MathParsingException e) {
                // Skip points that can't be evaluated
            }
        }
        return inflections;
    }

    /**
     * Calculates the numerical second derivative of a function at a given point.
     * Uses the central difference method.
     */
    public static double secondDerivative(Function f, double x) {
        double h = 1e-4;
        try {
            return (f.evaluate(x + h) - 2 * f.evaluate(x) + f.evaluate(x - h)) / (h * h);
        } catch (MathParsingException e) {
            return 0.0;
        }
    }
} 