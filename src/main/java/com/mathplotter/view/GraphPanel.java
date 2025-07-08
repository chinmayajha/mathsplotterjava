package com.mathplotter.view;

import com.mathplotter.model.Function;
import com.mathplotter.model.GraphSettings;
import com.mathplotter.model.Point;
import com.mathplotter.utils.NumericalMethods;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.util.stream.Collectors;

import javax.swing.SwingWorker;
import java.util.HashSet;
import java.util.Set;
import javax.swing.Timer;

public class GraphPanel extends JPanel {

    private List<com.mathplotter.model.Function> functions = new ArrayList<>();
    private GraphSettings settings;
    private java.awt.Point lastMousePosition;

    // For visualization
    private List<Point> specialPoints = new ArrayList<>();
    private com.mathplotter.model.Function shadedFunction;
    private double shadeStart, shadeEnd;

    // For interactive cursor
    private java.awt.Point mousePosition;
    private com.mathplotter.model.Function nearestFunction;
    private Point2D.Double curvePoint;
    private boolean showCursor = false;
    private static final int SNAP_DISTANCE = 10; // pixels

    // View change listeners
    public interface ViewChangeListener {
        void onViewChanged();
    }
    private final Set<ViewChangeListener> viewChangeListeners = new HashSet<>();
    public void addViewChangeListener(ViewChangeListener l) { viewChangeListeners.add(l); }
    public void removeViewChangeListener(ViewChangeListener l) { viewChangeListeners.remove(l); }
    private void fireViewChanged() {
        for (ViewChangeListener l : viewChangeListeners) l.onViewChanged();
    }

    // Debounce timer for analysis
    private Timer analysisDebounceTimer;
    private static final int ANALYSIS_DEBOUNCE_MS = 200;

    public GraphPanel() {
        this.settings = new GraphSettings();
        addMouseListeners();
    }

    private void addMouseListeners() {
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                lastMousePosition = e.getPoint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                showCursor = false;
                nearestFunction = null;
                repaint();
            }
        });

        this.addMouseWheelListener(new MouseAdapter() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                double zoomFactor = e.getWheelRotation() < 0 ? 1.1 : 0.9;
                
                // Get mouse position in graph coordinates
                double mouseX = settings.getxMin() + (e.getX() / (double) getWidth()) * (settings.getxMax() - settings.getxMin());
                double mouseY = settings.getyMax() - (e.getY() / (double) getHeight()) * (settings.getyMax() - settings.getyMin());

                double xRange = settings.getxMax() - settings.getxMin();
                double yRange = settings.getyMax() - settings.getyMin();

                settings.setxMin(mouseX - (mouseX - settings.getxMin()) / zoomFactor);
                settings.setxMax(mouseX + (settings.getxMax() - mouseX) / zoomFactor);
                settings.setyMin(mouseY - (mouseY - settings.getyMin()) / zoomFactor);
                settings.setyMax(mouseY + (settings.getyMax() - mouseY) / zoomFactor);

                repaint();
                fireViewChanged();
            }
        });

        this.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (lastMousePosition != null) {
                    int dx = e.getX() - lastMousePosition.x;
                    int dy = e.getY() - lastMousePosition.y;

                    double xRange = settings.getxMax() - settings.getxMin();
                    double yRange = settings.getyMax() - settings.getyMin();

                    double xPan = dx * (xRange / getWidth());
                    double yPan = dy * (yRange / getHeight());

                    settings.setxMin(settings.getxMin() - xPan);
                    settings.setxMax(settings.getxMax() - xPan);
                    settings.setyMin(settings.getyMin() + yPan);
                    settings.setyMax(settings.getyMax() + yPan);

                    lastMousePosition = e.getPoint();
                    updateStatusBar(e.getPoint());
                    repaint();
                    fireViewChanged();
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                mousePosition = e.getPoint();
                updateCursorPosition();
                repaint();
            }
        });
    }

    private void updateCursorPosition() {
        if (mousePosition == null) return;

        Point2D.Double graphMouse = screenToGraph(mousePosition);
        com.mathplotter.model.Function nearest = findNearestFunction(graphMouse);

        if (nearest != null) {
            double x = graphMouse.getX();
            try {
                double y = nearest.evaluate(x);
                this.curvePoint = new Point2D.Double(x, y);
                this.nearestFunction = nearest;
                this.showCursor = true;
            } catch (Exception ex) {
                this.showCursor = false;
                this.nearestFunction = null;
            }
        } else {
            this.showCursor = false;
            this.nearestFunction = null;
        }
        updateStatusBar(mousePosition); // keep the status bar updating
    }

    private com.mathplotter.model.Function findNearestFunction(Point2D.Double graphPoint) {
        com.mathplotter.model.Function nearestFunc = null;
        double minDistance = Double.MAX_VALUE;

        for (com.mathplotter.model.Function func : functions) {
            if (!func.isVisible()) continue;
            try {
                double y = func.evaluate(graphPoint.getX());
                Point2D.Double screenFuncPoint = graphToScreen(new Point2D.Double(graphPoint.getX(), y));
                double distance = mousePosition.distance(screenFuncPoint);
                if (distance < SNAP_DISTANCE && distance < minDistance) {
                    minDistance = distance;
                    nearestFunc = func;
                }
            } catch (Exception e) {
                continue;
            }
        }
        return nearestFunc;
    }

    private Point2D.Double screenToGraph(java.awt.Point screenPoint) {
        double xRange = settings.getxMax() - settings.getxMin();
        double yRange = settings.getyMax() - settings.getyMin();
        double graphX = settings.getxMin() + (screenPoint.x / (double) getWidth()) * xRange;
        double graphY = settings.getyMax() - (screenPoint.y / (double) getHeight()) * yRange;
        return new Point2D.Double(graphX, graphY);
    }

    private Point2D.Double graphToScreen(Point2D.Double graphPoint) {
        double xRange = settings.getxMax() - settings.getxMin();
        double yRange = settings.getyMax() - settings.getyMin();
        double screenX = (graphPoint.getX() - settings.getxMin()) / xRange * getWidth();
        double screenY = (settings.getyMax() - graphPoint.getY()) / yRange * getHeight();
        return new Point2D.Double(screenX, screenY);
    }

    private void updateStatusBar(java.awt.Point screenPos) {
        if (showCursor && nearestFunction != null) {
            String message = String.format("  f(x)=%s | (%.4f, %.4f)", nearestFunction.getExpression(), curvePoint.getX(), curvePoint.getY());
            firePropertyChange("coordinates", null, message);
        } else {
            double mouseX = settings.getxMin() + (screenPos.x / (double) getWidth()) * (settings.getxMax() - settings.getxMin());
            double mouseY = settings.getyMax() - (screenPos.y / (double) getHeight()) * (settings.getyMax() - settings.getyMin());
            String message = String.format("  x: %.4f, y: %.4f", mouseX, mouseY);
            firePropertyChange("coordinates", null, message);
        }
    }

    public void addFunction(com.mathplotter.model.Function function) {
        functions.add(function);
        repaint();
    }

    public void removeFunction(com.mathplotter.model.Function function) {
        functions.remove(function);
        repaint();
    }

    public void removeAllFunctions() {
        functions.clear();
        this.specialPoints.clear();
        this.shadedFunction = null;
        repaint();
    }

    public void addIntersectionPoints(List<Point2D> points) {
        // This method is not used in the original implementation
    }

    public void addExtremaPoints(List<Point2D> points) {
        // This method is not used in the original implementation
    }

    public void clearSpecialPoints() {
        this.specialPoints.clear();
        repaint();
    }

    public List<com.mathplotter.model.Function> getFunctions() {
        return functions;
    }

    public void setFunctionsVisibility(List<com.mathplotter.model.Function> funcs, boolean visible) {
        for (com.mathplotter.model.Function f : funcs) {
            f.setVisible(visible);
        }
        repaint();
    }

    public void zoom(double factor) {
        // Zoom centered on the view's center
        double centerX = (settings.getxMin() + settings.getxMax()) / 2;
        double centerY = (settings.getyMin() + settings.getyMax()) / 2;

        settings.setxMin(centerX - (centerX - settings.getxMin()) / factor);
        settings.setxMax(centerX + (settings.getxMax() - centerX) / factor);
        settings.setyMin(centerY - (centerY - settings.getyMin()) / factor);
        settings.setyMax(centerY + (settings.getyMax() - centerY) / factor);

        repaint();
        fireViewChanged();
    }

    public void resetView() {
        this.settings = new GraphSettings();
        repaint();
        fireViewChanged();
    }

    public void setAxisRanges(double xMin, double xMax, double yMin, double yMax) {
        settings.setxMin(xMin);
        settings.setxMax(xMax);
        settings.setyMin(yMin);
        settings.setyMax(yMax);

        repaint();
        fireViewChanged();
    }

    // Add analysis type state
    public enum AnalysisType { NONE, MAXIMA, MINIMA, INTERSECTIONS, ZEROS, INFLECTION }
    private AnalysisType currentAnalysis = AnalysisType.NONE;
    private Function analysisFunc1 = null;
    private Function analysisFunc2 = null;

    public void setAnalysisType(AnalysisType type, Function f1, Function f2) {
        this.currentAnalysis = type;
        this.analysisFunc1 = f1;
        this.analysisFunc2 = f2;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawGrid(g2d);
        drawAxes(g2d);
        drawFunctions(g2d);
        drawShadedArea(g2d);
        // Only draw special points for the current analysis
        if (currentAnalysis != AnalysisType.NONE) {
            drawSpecialPoints(g2d);
        }
        if (showCursor && curvePoint != null && nearestFunction != null) {
            drawInteractiveCursor((Graphics2D) g);
        }
    }

    private void drawGrid(Graphics2D g2d) {
        g2d.setColor(new Color(200, 200, 200));
        g2d.setStroke(new BasicStroke(1));

        double xRange = settings.getxMax() - settings.getxMin();
        double yRange = settings.getyMax() - settings.getyMin();

        // Dynamic grid spacing
        double xGridSpacing = Math.pow(10, Math.floor(Math.log10(xRange)) - 1);
        double yGridSpacing = Math.pow(10, Math.floor(Math.log10(yRange)) - 1);
        
        // Adjust spacing for better visuals
        if (xRange / xGridSpacing > 20) xGridSpacing *= 5;
        else if (xRange / xGridSpacing < 4) xGridSpacing /= 2;
        if (yRange / yGridSpacing > 20) yGridSpacing *= 5;
        else if (yRange / yGridSpacing < 4) yGridSpacing /= 2;


        // Draw vertical grid lines
        for (double x = Math.floor(settings.getxMin() / xGridSpacing) * xGridSpacing; x <= settings.getxMax(); x += xGridSpacing) {
            int screenX = (int) (((x - settings.getxMin()) / xRange) * getWidth());
            g2d.drawLine(screenX, 0, screenX, getHeight());
        }

        // Draw horizontal grid lines
        for (double y = Math.floor(settings.getyMin() / yGridSpacing) * yGridSpacing; y <= settings.getyMax(); y += yGridSpacing) {
            int screenY = getHeight() - (int) (((y - settings.getyMin()) / yRange) * getHeight());
            g2d.drawLine(0, screenY, getWidth(), screenY);
        }
    }

    private void drawAxes(Graphics2D g2d) {
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(2.0f));

        // Draw main axes
        if (settings.getyMin() <= 0 && settings.getyMax() >= 0) {
            // X-axis
            int y = (int) graphToScreenY(0);
            g2d.drawLine(0, y, getWidth(), y);
            drawXAxisLabels(g2d, y);
        }

        if (settings.getxMin() <= 0 && settings.getxMax() >= 0) {
            // Y-axis
            int x = (int) graphToScreenX(0);
            g2d.drawLine(x, 0, x, getHeight());
            drawYAxisLabels(g2d, x);
        }
    }

    private void drawXAxisLabels(Graphics2D g2d, int axisY) {
        g2d.setFont(new Font("Arial", Font.PLAIN, 10));
        FontMetrics fm = g2d.getFontMetrics();

        double range = settings.getxMax() - settings.getxMin();
        double spacing = calculateOptimalSpacing(range, getWidth());

        // Draw tick marks and labels
        for (double x = Math.ceil(settings.getxMin() / spacing) * spacing; x <= settings.getxMax(); x += spacing) {
            int screenX = (int) graphToScreenX(x);

            // Draw tick mark
            g2d.setColor(Color.BLACK);
            g2d.drawLine(screenX, axisY - 5, screenX, axisY + 5);

            // Draw label
            if (Math.abs(x) > 1e-10) { // Don't label zero
                String label = formatAxisLabel(x);
                int labelWidth = fm.stringWidth(label);
                g2d.drawString(label, screenX - labelWidth/2, axisY + 18);
            }

            // Draw minor tick marks
            drawMinorTicks(g2d, x, spacing / 5, axisY, true);
        }
    }

    private void drawYAxisLabels(Graphics2D g2d, int axisX) {
        g2d.setFont(new Font("Arial", Font.PLAIN, 10));
        FontMetrics fm = g2d.getFontMetrics();

        double range = settings.getyMax() - settings.getyMin();
        double spacing = calculateOptimalSpacing(range, getHeight());

        for (double y = Math.ceil(settings.getyMin() / spacing) * spacing; y <= settings.getyMax(); y += spacing) {
            int screenY = (int) graphToScreenY(y);

            // Draw tick mark
            g2d.setColor(Color.BLACK);
            g2d.drawLine(axisX - 5, screenY, axisX + 5, screenY);

            // Draw label
            if (Math.abs(y) > 1e-10) { // Don't label zero
                String label = formatAxisLabel(y);
                g2d.drawString(label, axisX - fm.stringWidth(label) - 8, screenY + 4);
            }

            // Draw minor tick marks
            drawMinorTicks(g2d, y, spacing / 5, axisX, false);
        }
    }

    private void drawMinorTicks(Graphics2D g2d, double major, double minorSpacing, int axisPos, boolean isXAxis) {
        g2d.setColor(Color.GRAY);

        for (int i = 1; i < 5; i++) {
            double minorValue = major + i * minorSpacing;

            if (isXAxis) {
                if (minorValue >= settings.getxMin() && minorValue <= settings.getxMax()) {
                    int screenX = (int) graphToScreenX(minorValue);
                    g2d.drawLine(screenX, axisPos - 2, screenX, axisPos + 2);
                }
            } else {
                if (minorValue >= settings.getyMin() && minorValue <= settings.getyMax()) {
                    int screenY = (int) graphToScreenY(minorValue);
                    g2d.drawLine(axisPos - 2, screenY, axisPos + 2, screenY);
                }
            }
        }
    }

    private double calculateOptimalSpacing(double range, int pixels) {
        if (pixels <= 0) return range;
        double targetSpacing = range / (pixels / 80.0); // Aim for labels every 80 pixels
        if (targetSpacing <= 0) return 1.0;
        double magnitude = Math.pow(10, Math.floor(Math.log10(targetSpacing)));
        double normalized = targetSpacing / magnitude;

        if (normalized <= 1) return magnitude;
        if (normalized <= 2) return 2 * magnitude;
        if (normalized <= 5) return 5 * magnitude;
        return 10 * magnitude;
    }

    private String formatAxisLabel(double value) {
        if (Math.abs(value) < 1e-10) return "0";

        if (Math.abs(value) >= 1000 || (Math.abs(value) < 0.01 && Math.abs(value) > 0) ) {
            return String.format("%.1e", value);
        }

        if (Math.abs(value - Math.round(value)) < 1e-10) {
            return String.valueOf((int) Math.round(value));
        }

        return String.format("%.2f", value);
    }

    private void drawFunctions(Graphics2D g2d) {
        for (com.mathplotter.model.Function function : functions) {
            if (function.isVisible()) {
                plotFunction(g2d, function);
            }
        }
    }

    private void plotFunction(Graphics2D g2d, com.mathplotter.model.Function f) {
        g2d.setColor(f.getColor());
        g2d.setStroke(new BasicStroke(2));

        GeneralPath path = new GeneralPath();
        
        double xRange = settings.getxMax() - settings.getxMin();
        double yRange = settings.getyMax() - settings.getyMin();
        double pixelToX = xRange / getWidth();

        boolean firstPoint = true;

        for (int screenX = 0; screenX < getWidth(); screenX++) {
            double x = settings.getxMin() + screenX * pixelToX;
            try {
                double y = f.evaluate(x);

                if (Double.isNaN(y) || Double.isInfinite(y)) {
                    firstPoint = true; // Discontinuity
                    continue;
                }

                double screenY = getHeight() - ((y - settings.getyMin()) / yRange) * getHeight();

                if (firstPoint) {
                    path.moveTo(screenX, screenY);
                    firstPoint = false;
                } else {
                    path.lineTo(screenX, screenY);
                }
            } catch (Exception e) {
                // In case of math errors for specific points (e.g., log(-1))
                firstPoint = true;
            }
        }
        g2d.draw(path);
    }

    private void drawSpecialPoints(Graphics2D g2d) {
        g2d.setColor(Color.RED);
        for (Point p : specialPoints) {
            double xRange = settings.getxMax() - settings.getxMin();
            double yRange = settings.getyMax() - settings.getyMin();
            int screenX = (int) (((p.getX() - settings.getxMin()) / xRange) * getWidth());
            int screenY = getHeight() - (int) (((p.getY() - settings.getyMin()) / yRange) * getHeight());
            
            // Draw a small circle for each point
            g2d.fillOval(screenX - 4, screenY - 4, 8, 8);
        }
    }

    private void drawShadedArea(Graphics2D g2d) {
        if (shadedFunction == null) return;
        
        // Set up the shading color with transparency
        g2d.setColor(new Color(200, 200, 255, 100));
        
        double xRange = settings.getxMax() - settings.getxMin();
        double yRange = settings.getyMax() - settings.getyMin();
        double pixelToX = xRange / getWidth();

        GeneralPath path = new GeneralPath();
        
        try {
            // Start from the left boundary at the x-axis (y=0)
            path.moveTo(graphToScreenX(shadeStart), graphToScreenY(0));
            
            // Draw the function curve from start to end
            for (int screenX = graphToScreenX(shadeStart); screenX <= graphToScreenX(shadeEnd); screenX++) {
                double x = settings.getxMin() + (screenX / (double) getWidth()) * xRange;
                if (x >= shadeStart && x <= shadeEnd) {
                    try {
                        double y = shadedFunction.evaluate(x);
                        if (Double.isFinite(y)) {
                            double screenY = graphToScreenY(y);
                            path.lineTo(screenX, screenY);
                        }
                    } catch (Exception e) {
                        // Skip points where function evaluation fails
                    }
                }
            }
            
            // Close the path by going back to the x-axis
            path.lineTo(graphToScreenX(shadeEnd), graphToScreenY(0));
            path.closePath();
            
            // Fill the shaded area
            g2d.fill(path);
            
        } catch (Exception e) {
            // Skip shading if function evaluation fails
        }
    }

    private void drawInteractiveCursor(Graphics2D g2d) {
        Point2D.Double screenPoint = graphToScreen(curvePoint);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        int x = (int) screenPoint.getX();
        int y = (int) screenPoint.getY();

        // Draw crosshair
        g2d.setColor(new Color(128, 128, 128, 150));
        g2d.drawLine(x, 0, x, getHeight());
        g2d.drawLine(0, y, getWidth(), y);

        // Draw point on curve
        g2d.setColor(nearestFunction.getColor());
        g2d.fillOval(x - 5, y - 5, 10, 10);
        g2d.setColor(Color.WHITE);
        g2d.fillOval(x - 2, y - 2, 4, 4);

        drawCoordinateTooltip(g2d, screenPoint);
    }

    private void drawCoordinateTooltip(Graphics2D g2d, Point2D.Double screenPoint) {
        String coordText = String.format("(%.3f, %.3f)", curvePoint.getX(), curvePoint.getY());
        
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(coordText);
        int textHeight = fm.getHeight();
        
        int tooltipX = (int) screenPoint.getX() + 15;
        int tooltipY = (int) screenPoint.getY() - 15;

        // Adjust if tooltip goes off screen
        if (tooltipX + textWidth + 10 > getWidth()) {
            tooltipX = (int) screenPoint.getX() - textWidth - 20;
        }
        if (tooltipY - textHeight - 5 < 0) {
            tooltipY = (int) screenPoint.getY() + textHeight + 20;
        }
        
        // Draw tooltip
        g2d.setColor(new Color(255, 255, 224, 220));
        g2d.fillRect(tooltipX, tooltipY - textHeight, textWidth + 10, textHeight + 5);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(tooltipX, tooltipY - textHeight, textWidth + 10, textHeight + 5);
        g2d.drawString(coordText, tooltipX + 5, tooltipY);
    }

    private int graphToScreenX(double x) {
        double xRange = settings.getxMax() - settings.getxMin();
        return (int) ((x - settings.getxMin()) / xRange * getWidth());
    }

    private int graphToScreenY(double y) {
        double yRange = settings.getyMax() - settings.getyMin();
        return (int) ((settings.getyMax() - y) / yRange * getHeight());
    }

    public GraphSettings getGraphSettings() {
        return settings;
    }

    public void setSpecialPoints(List<Point> specialPoints) {
        this.specialPoints = specialPoints;
        repaint();
    }

    public List<Point> getSpecialPoints() {
        return new ArrayList<>(specialPoints);
    }

    public void setShadedArea(com.mathplotter.model.Function function, double start, double end) {
        this.shadedFunction = function;
        this.shadeStart = start;
        this.shadeEnd = end;
        repaint();
    }

    public com.mathplotter.model.Function getShadedFunction() {
        return shadedFunction;
    }

    public double getShadeStart() {
        return shadeStart;
    }

    public double getShadeEnd() {
        return shadeEnd;
    }

    public void clearShadedArea() {
        this.shadedFunction = null;
        this.shadeStart = 0;
        this.shadeEnd = 0;
        repaint();
    }
}