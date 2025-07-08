package com.mathplotter.view;

import com.mathplotter.utils.SVGExporter;
import com.mathplotter.model.Function;
import com.mathplotter.utils.NumericalMethods;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.core.type.TypeReference;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

public class MainFrame extends JFrame {
    private GraphPanel graphPanel;
    private ControlPanel controlPanel;
    private JLabel statusBar;

    public MainFrame() {
        setTitle("Mathematical Function Plotter & Solver");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initComponents();
        layoutComponents();
        createMenuBar();
    }

    private void initComponents() {
        graphPanel = new GraphPanel();
        controlPanel = new ControlPanel(graphPanel);
        statusBar = new JLabel(" ");
        
        // Listen for coordinate updates from the graph panel
        graphPanel.addPropertyChangeListener("coordinates", e -> {
            statusBar.setText((String) e.getNewValue());
        });
    }

    private void layoutComponents() {
        setLayout(new BorderLayout());
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, controlPanel, graphPanel);
        splitPane.setDividerLocation(330); // Reduced width for the left pane (55% of previous 600)
        splitPane.setOneTouchExpandable(true);
        add(splitPane, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem exportSvgItem = new JMenuItem("Export to SVG...");
        JMenuItem exportJsonItem = new JMenuItem("Export Functions to JSON...");
        JMenuItem importJsonItem = new JMenuItem("Import Functions from JSON...");

        exportSvgItem.addActionListener(e -> exportSVG());
        exportJsonItem.addActionListener(e -> exportFunctionsToJson());
        importJsonItem.addActionListener(e -> importFunctionsFromJson());
        
        fileMenu.add(exportSvgItem);
        fileMenu.add(exportJsonItem);
        fileMenu.add(importJsonItem);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);
    }

    private void exportSVG() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export as SVG");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Scalable Vector Graphics (*.svg)", "svg"));
        fileChooser.setSelectedFile(new File("graph.svg"));

        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            // Ensure the file has a .svg extension
            if (!fileToSave.getName().toLowerCase().endsWith(".svg")) {
                fileToSave = new File(fileToSave.getParentFile(), fileToSave.getName() + ".svg");
            }

            try {
                SVGExporter exporter = new SVGExporter(graphPanel);
                exporter.exportToSVG(fileToSave);
                JOptionPane.showMessageDialog(this, "Graph exported successfully to " + fileToSave.getAbsolutePath(), "Export Successful", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error exporting SVG: " + ex.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }

    private void exportFunctionsToJson() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Functions as JSON");
        fileChooser.setFileFilter(new FileNameExtensionFilter("JSON Files (*.json)", "json"));
        fileChooser.setSelectedFile(new File("functions.json"));
        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            if (!fileToSave.getName().toLowerCase().endsWith(".json")) {
                fileToSave = new File(fileToSave.getParentFile(), fileToSave.getName() + ".json");
            }
            try {
                ObjectMapper mapper = new ObjectMapper();
                mapper.enable(SerializationFeature.INDENT_OUTPUT);
                List<FunctionDTO> functionDTOs = new ArrayList<>();
                for (Function f : graphPanel.getFunctions()) {
                    functionDTOs.add(new FunctionDTO(f));
                }
                mapper.writeValue(fileToSave, functionDTOs);
                JOptionPane.showMessageDialog(this, "Functions exported successfully to " + fileToSave.getAbsolutePath(), "Export Successful", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error exporting functions: " + ex.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void importFunctionsFromJson() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Import Functions from JSON");
        fileChooser.setFileFilter(new FileNameExtensionFilter("JSON Files (*.json)", "json"));
        int userSelection = fileChooser.showOpenDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToOpen = fileChooser.getSelectedFile();
            try {
                ObjectMapper mapper = new ObjectMapper();
                List<FunctionDTO> functionDTOs = mapper.readValue(fileToOpen, new TypeReference<List<FunctionDTO>>(){});
                graphPanel.removeAllFunctions();
                List<Function> importedFunctions = new ArrayList<>();
                // First pass: create all normal functions
                for (FunctionDTO dto : functionDTOs) {
                    if (!isDerivativeExpression(dto.expression)) {
                        Function f = new Function(dto.expression, new Color(dto.colorRGB));
                        f.setVisible(dto.visible);
                        importedFunctions.add(f);
                        graphPanel.addFunction(f);
                    }
                }
                // Second pass: create all derivatives (including nested)
                for (FunctionDTO dto : functionDTOs) {
                    if (isDerivativeExpression(dto.expression)) {
                        Function derivative = resolveDerivative(dto.expression, importedFunctions, dto.colorRGB, dto.visible);
                        if (derivative != null) {
                            graphPanel.addFunction(derivative);
                            importedFunctions.add(derivative);
                        }
                    }
                }
                controlPanel.updateFunctionLists(graphPanel.getFunctions());
                JOptionPane.showMessageDialog(this, "Functions imported successfully from " + fileToOpen.getAbsolutePath(), "Import Successful", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error importing functions: " + ex.getMessage(), "Import Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // Recursively resolve nested derivatives
    private Function resolveDerivative(String expr, List<Function> importedFunctions, int colorRGB, boolean visible) {
        if (isDerivativeExpression(expr)) {
            String innerExpr = expr.substring(5, expr.length() - 1);
            Function base;
            if (isDerivativeExpression(innerExpr)) {
                base = resolveDerivative(innerExpr, importedFunctions, colorRGB, visible);
            } else {
                base = importedFunctions.stream().filter(fn -> fn.getExpression().equals(innerExpr)).findFirst().orElse(null);
                if (base == null) {
                    try {
                        base = new Function(innerExpr, new Color(colorRGB));
                        importedFunctions.add(base);
                    } catch (Exception e) {
                        return null;
                    }
                }
            }
            Function derivative = NumericalMethods.derivative(base);
            derivative.setColor(new Color(colorRGB));
            derivative.setVisible(visible);
            return derivative;
        }
        return null;
    }

    private boolean isDerivativeExpression(String expr) {
        return expr.startsWith("d/dx(") && expr.endsWith(")");
    }

    // Helper DTO for JSON serialization
    private static class FunctionDTO {
        public String expression;
        public int colorRGB;
        public boolean visible;
        public FunctionDTO() {}
        public FunctionDTO(Function f) {
            this.expression = f.getExpression();
            this.colorRGB = f.getColor().getRGB();
            this.visible = f.isVisible();
        }
    }
} 