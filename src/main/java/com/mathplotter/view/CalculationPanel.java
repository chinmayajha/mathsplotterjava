package com.mathplotter.view;

import com.mathplotter.model.Function;
import com.mathplotter.model.GraphSettings;
import com.mathplotter.model.Point;
import com.mathplotter.utils.NumericalMethods;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.Vector;

public class CalculationPanel extends JPanel {

    private final GraphPanel graphPanel;
    private final ControlPanel controlPanel;
    private JComboBox<Function> intersectionFunc1;
    private JComboBox<Function> intersectionFunc2;
    private JComboBox<Function> integralFunc;
    private JSpinner integralStart, integralEnd, integralSteps;
    private JTextArea resultsArea;
    private JComboBox<Function> derivativeFunc;

    // --- Analysis State ---
    private enum AnalysisType { NONE, MAXIMA, MINIMA, INTERSECTIONS, ZEROS, INFLECTION }
    private AnalysisType currentAnalysis = AnalysisType.NONE;
    private JButton maximaBtn, minimaBtn, intersectionsBtn, zerosBtn, inflectionBtn;
    private Timer analysisDebounceTimer;
    private static final int ANALYSIS_DEBOUNCE_MS = 150;

    public CalculationPanel(GraphPanel graphPanel, ControlPanel controlPanel) {
        this.graphPanel = graphPanel;
        this.controlPanel = controlPanel;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createTitledBorder("Analysis"));

        setupAnalysisToggleUI();
        setupIntersectionUI();
        setupIntegrationUI();
        setupDerivativeUI();
        setupResultsUI();

        // Listen for viewport changes to update analysis dynamically
        graphPanel.addViewChangeListener(this::scheduleAnalysisUpdate);

        // Listen for function selection changes to update analysis
        // (for maxima, minima, zeros, intersections)
        if (derivativeFunc != null) {
            derivativeFunc.addActionListener(e -> {
                if (currentAnalysis == AnalysisType.MAXIMA || currentAnalysis == AnalysisType.MINIMA || currentAnalysis == AnalysisType.ZEROS) {
                    scheduleAnalysisUpdate();
                }
            });
        }
        if (intersectionFunc1 != null) {
            intersectionFunc1.addActionListener(e -> {
                if (currentAnalysis == AnalysisType.INTERSECTIONS) {
                    scheduleAnalysisUpdate();
                }
            });
        }
        if (intersectionFunc2 != null) {
            intersectionFunc2.addActionListener(e -> {
                if (currentAnalysis == AnalysisType.INTERSECTIONS) {
                    scheduleAnalysisUpdate();
                }
            });
        }
    }

    private void setupAnalysisToggleUI() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        panel.setBorder(BorderFactory.createTitledBorder("Quick Analysis"));
        Dimension btnSize = new Dimension(77, 24);
        maximaBtn = new JButton("Maxima");
        maximaBtn.setPreferredSize(btnSize);
        minimaBtn = new JButton("Minima");
        minimaBtn.setPreferredSize(btnSize);
        zerosBtn = new JButton("Zeros");
        zerosBtn.setPreferredSize(btnSize);
        inflectionBtn = new JButton("Inflection");
        inflectionBtn.setPreferredSize(btnSize);

        maximaBtn.addActionListener(e -> toggleAnalysis(AnalysisType.MAXIMA));
        minimaBtn.addActionListener(e -> toggleAnalysis(AnalysisType.MINIMA));
        zerosBtn.addActionListener(e -> toggleAnalysis(AnalysisType.ZEROS));
        inflectionBtn.addActionListener(e -> toggleAnalysis(AnalysisType.INFLECTION));

        panel.add(maximaBtn);
        panel.add(minimaBtn);
        panel.add(zerosBtn);
        panel.add(inflectionBtn);
        add(panel);
    }

    private void toggleAnalysis(AnalysisType type) {
        if (currentAnalysis == type) {
            currentAnalysis = AnalysisType.NONE;
        } else {
            currentAnalysis = type;
        }
        updateButtonStates();
        scheduleAnalysisUpdate();
        // Update graph analysis type immediately
        if (currentAnalysis == AnalysisType.NONE) {
            graphPanel.setAnalysisType(GraphPanel.AnalysisType.NONE, null, null);
            graphPanel.clearSpecialPoints();
        } else if (currentAnalysis == AnalysisType.INTERSECTIONS) {
            Function f1 = (Function) intersectionFunc1.getSelectedItem();
            Function f2 = (Function) intersectionFunc2.getSelectedItem();
            graphPanel.setAnalysisType(GraphPanel.AnalysisType.INTERSECTIONS, f1, f2);
        } else {
            Function f = (Function) derivativeFunc.getSelectedItem();
            graphPanel.setAnalysisType(GraphPanel.AnalysisType.valueOf(currentAnalysis.name()), f, null);
        }
    }

    private void updateButtonStates() {
        maximaBtn.setBackground(currentAnalysis == AnalysisType.MAXIMA ? Color.CYAN : null);
        minimaBtn.setBackground(currentAnalysis == AnalysisType.MINIMA ? Color.CYAN : null);
        zerosBtn.setBackground(currentAnalysis == AnalysisType.ZEROS ? Color.CYAN : null);
        inflectionBtn.setBackground(currentAnalysis == AnalysisType.INFLECTION ? Color.CYAN : null);
    }

    private void scheduleAnalysisUpdate() {
        if (analysisDebounceTimer != null && analysisDebounceTimer.isRunning()) {
            analysisDebounceTimer.restart();
        } else {
            analysisDebounceTimer = new Timer(ANALYSIS_DEBOUNCE_MS, e -> {
                analysisDebounceTimer.stop();
                updateAnalysis();
            });
            analysisDebounceTimer.setRepeats(false);
            analysisDebounceTimer.start();
        }
    }

    private void updateAnalysis() {
        if (currentAnalysis == AnalysisType.NONE) {
            graphPanel.setAnalysisType(GraphPanel.AnalysisType.NONE, null, null);
            graphPanel.clearSpecialPoints();
            resultsArea.setText("");
            return;
        }
        // Run in background for UI responsiveness
        new SwingWorker<List<Point>, Void>() {
            StringBuilder sb = new StringBuilder();
            @Override
            protected List<Point> doInBackground() {
                List<Point> points = new java.util.ArrayList<>();
                double xMin = graphPanel.getGraphSettings().getxMin();
                double xMax = graphPanel.getGraphSettings().getxMax();
                switch (currentAnalysis) {
                    case MAXIMA: {
                        Function f = (Function) derivativeFunc.getSelectedItem();
                        if (f != null) {
                            points = com.mathplotter.utils.NumericalMethods.findLocalMaxima(f, xMin, xMax);
                            sb.append(points.size()).append(" maxima found in current view.\n");
                            for (Point p : points) sb.append("Max: ").append(p).append("\n");
                        } else {
                            sb.append("Select a function for maxima analysis.\n");
                        }
                        break;
                    }
                    case MINIMA: {
                        Function f = (Function) derivativeFunc.getSelectedItem();
                        if (f != null) {
                            points = com.mathplotter.utils.NumericalMethods.findLocalMinima(f, xMin, xMax);
                            sb.append(points.size()).append(" minima found in current view.\n");
                            for (Point p : points) sb.append("Min: ").append(p).append("\n");
                        } else {
                            sb.append("Select a function for minima analysis.\n");
                        }
                        break;
                    }
                    case INFLECTION: {
                        Function f = (Function) derivativeFunc.getSelectedItem();
                        if (f != null) {
                            points = com.mathplotter.utils.NumericalMethods.findInflectionPoints(f, xMin, xMax);
                            sb.append(points.size()).append(" inflection points found in current view.\n");
                            for (Point p : points) sb.append("Inflection: ").append(p).append("\n");
                        } else {
                            sb.append("Select a function for inflection analysis.\n");
                        }
                        break;
                    }
                    case INTERSECTIONS: {
                        Function f1 = (Function) intersectionFunc1.getSelectedItem();
                        Function f2 = (Function) intersectionFunc2.getSelectedItem();
                        if (f1 != null && f2 != null && !f1.equals(f2)) {
                            points = com.mathplotter.utils.NumericalMethods.findIntersections(f1, f2, xMin, xMax);
                            sb.append(points.size()).append(" intersections found in current view.\n");
                            for (Point p : points) sb.append("Intersection: ").append(p).append("\n");
                        } else {
                            sb.append("Select two different functions for intersection analysis.\n");
                        }
                        break;
                    }
                    case ZEROS: {
                        Function f = (Function) derivativeFunc.getSelectedItem();
                        if (f != null) {
                            points = com.mathplotter.utils.NumericalMethods.findRoots(f, xMin, xMax);
                            sb.append(points.size()).append(" zeros found in current view.\n");
                            for (Point p : points) sb.append("Zero: ").append(p).append("\n");
                        } else {
                            sb.append("Select a function for zero analysis.\n");
                        }
                        break;
                    }
                }
                return points;
            }
            @Override
            protected void done() {
                try {
                    List<Point> points = get();
                    graphPanel.setSpecialPoints(points);
                    // Update graph analysis type again in case functions changed
                    if (currentAnalysis == AnalysisType.INTERSECTIONS) {
                        Function f1 = (Function) intersectionFunc1.getSelectedItem();
                        Function f2 = (Function) intersectionFunc2.getSelectedItem();
                        graphPanel.setAnalysisType(GraphPanel.AnalysisType.INTERSECTIONS, f1, f2);
                    } else {
                        Function f = (Function) derivativeFunc.getSelectedItem();
                        graphPanel.setAnalysisType(GraphPanel.AnalysisType.valueOf(currentAnalysis.name()), f, null);
                    }
                    resultsArea.setText(sb.toString());
                } catch (Exception ex) {
                    resultsArea.setText("Error updating analysis: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void setupIntersectionUI() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Intersections"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Function 1:"), gbc);
        gbc.gridx = 1;
        intersectionFunc1 = new JComboBox<>();
        intersectionFunc1.setRenderer(new FunctionListCellRenderer());
        intersectionFunc1.setPreferredSize(new Dimension(100, 22));
        panel.add(intersectionFunc1, gbc);
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Function 2:"), gbc);
        gbc.gridx = 1;
        intersectionFunc2 = new JComboBox<>();
        intersectionFunc2.setRenderer(new FunctionListCellRenderer());
        intersectionFunc2.setPreferredSize(new Dimension(100, 22));
        panel.add(intersectionFunc2, gbc);
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        JButton findButton = new JButton("Find Intersections");
        findButton.setPreferredSize(new Dimension(160, 24));
        findButton.addActionListener(e -> {
            if (currentAnalysis == AnalysisType.INTERSECTIONS) {
                // If already showing intersections, clear them
                currentAnalysis = AnalysisType.NONE;
                updateButtonStates();
                graphPanel.setAnalysisType(GraphPanel.AnalysisType.NONE, null, null);
                graphPanel.clearSpecialPoints();
                resultsArea.setText("");
            } else {
                currentAnalysis = AnalysisType.INTERSECTIONS;
                updateButtonStates();
                scheduleAnalysisUpdate();
                // Update graph analysis type immediately
                Function f1 = (Function) intersectionFunc1.getSelectedItem();
                Function f2 = (Function) intersectionFunc2.getSelectedItem();
                graphPanel.setAnalysisType(GraphPanel.AnalysisType.INTERSECTIONS, f1, f2);
            }
        });
        panel.add(findButton, gbc);
        add(panel);
    }
    
    private void setupIntegrationUI() {
        JPanel panel = new JPanel(new GridLayout(0, 2));
        panel.setBorder(BorderFactory.createTitledBorder("Definite Integral"));
        
        integralFunc = new JComboBox<>();
        integralFunc.setRenderer(new FunctionListCellRenderer());
        integralStart = new JSpinner(new SpinnerNumberModel(0.0, -1000.0, 1000.0, 0.1));
        integralEnd = new JSpinner(new SpinnerNumberModel(1.0, -1000.0, 1000.0, 0.1));
        integralSteps = new JSpinner(new SpinnerNumberModel(1000, 10, 10000, 10));
        JButton calcButton = new JButton("Calculate");
        
        panel.add(new JLabel("Function:"));
        panel.add(integralFunc);
        panel.add(new JLabel("From (a):"));
        panel.add(integralStart);
        panel.add(new JLabel("To (b):"));
        panel.add(integralEnd);
        panel.add(new JLabel("Subdivisions:"));
        panel.add(integralSteps);
        panel.add(new JLabel("")); // Spacer
        panel.add(calcButton);
        
        JButton clearButton = new JButton("Clear Shading");
        panel.add(clearButton);

        calcButton.addActionListener(e -> calculateIntegral());
        clearButton.addActionListener(e -> clearShading());
        add(panel);
    }
    
    private void setupDerivativeUI() {
        JPanel panel = new JPanel(new GridLayout(0, 2));
        panel.setBorder(BorderFactory.createTitledBorder("Derivatives & Extrema"));
        
        derivativeFunc = new JComboBox<>();
        derivativeFunc.setRenderer(new FunctionListCellRenderer());
        JButton plotDerivativeBtn = new JButton("Plot Derivative");

        panel.add(new JLabel("Function:"));
        panel.add(derivativeFunc);
        panel.add(plotDerivativeBtn);
        panel.add(new JLabel("")); // Spacer
        
        plotDerivativeBtn.addActionListener(e -> plotDerivative());
        add(panel);
    }
    
    private void setupResultsUI() {
        resultsArea = new JTextArea(5, 20);
        resultsArea.setEditable(false);
        resultsArea.setBorder(BorderFactory.createTitledBorder("Results"));
        add(new JScrollPane(resultsArea));
    }

    public void updateFunctionLists(List<Function> functions) {
        Vector<Function> functionVector = new Vector<>(functions);
        intersectionFunc1.setModel(new DefaultComboBoxModel<>(functionVector));
        intersectionFunc2.setModel(new DefaultComboBoxModel<>(functionVector));
        integralFunc.setModel(new DefaultComboBoxModel<>(functionVector));
        derivativeFunc.setModel(new DefaultComboBoxModel<>(functionVector));
        
        // Avoid selecting the same function by default
        if (functionVector.size() > 1) {
            intersectionFunc2.setSelectedIndex(1);
        }
    }

    private void findIntersections() {
        Function f1 = (Function) intersectionFunc1.getSelectedItem();
        Function f2 = (Function) intersectionFunc2.getSelectedItem();

        if (f1 == null || f2 == null || f1.equals(f2)) {
            JOptionPane.showMessageDialog(this, "Please select two different functions.", "Selection Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            GraphSettings settings = graphPanel.getGraphSettings();
            List<Point> points = NumericalMethods.findIntersections(f1, f2, settings.getxMin(), settings.getxMax());
            graphPanel.setSpecialPoints(points);

            StringBuilder sb = new StringBuilder("Intersections found:\n");
            if (points.isEmpty()) {
                sb.append("None in the current view.");
            } else {
                points.forEach(p -> sb.append(p.toString()).append("\n"));
            }
            resultsArea.setText(sb.toString());
        } catch (Exception e) {
            resultsArea.setText("Error calculating intersections: " + e.getMessage());
        }
    }
    
    private void calculateIntegral() {
        Function f = (Function) integralFunc.getSelectedItem();
        if (f == null) {
             JOptionPane.showMessageDialog(this, "Please select a function.", "Selection Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            double a = ((Number) integralStart.getValue()).doubleValue();
            double b = ((Number) integralEnd.getValue()).doubleValue();
            int n = ((Number) integralSteps.getValue()).intValue();

            double result = NumericalMethods.integrate(f, a, b, 1000);
            graphPanel.setShadedArea(f, a, b);

            resultsArea.setText(String.format("Integral from %.2f to %.2f: %.6f", a, b, result));

        } catch (Exception ex) {
            resultsArea.setText("Error calculating integral: " + ex.getMessage());
        }
    }

    private void clearShading() {
        graphPanel.clearShadedArea();
        resultsArea.setText("Shaded area cleared.");
    }

    private void plotDerivative() {
        Function f = (Function) derivativeFunc.getSelectedItem();
        if (f == null) return;

        try {
            com.mathplotter.model.Function derivativeFunction = NumericalMethods.derivative(f);
            graphPanel.addFunction(derivativeFunction);
            resultsArea.setText("Derivative function added to the graph.");
            controlPanel.updateFunctionLists(graphPanel.getFunctions());
        } catch (Exception e) {
            resultsArea.setText("Error calculating derivative: " + e.getMessage());
        }
    }
} 