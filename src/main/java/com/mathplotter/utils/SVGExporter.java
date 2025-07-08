package com.mathplotter.utils;

import com.mathplotter.model.Function;
import com.mathplotter.model.Point;
import com.mathplotter.view.GraphPanel;

import java.awt.Color;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class SVGExporter {
    private GraphPanel graphPanel;
    private double xMin, xMax, yMin, yMax;
    private int width, height;
    
    public SVGExporter(GraphPanel panel) {
        this.graphPanel = panel;
        this.xMin = panel.getGraphSettings().getxMin();
        this.xMax = panel.getGraphSettings().getxMax();
        this.yMin = panel.getGraphSettings().getyMin();
        this.yMax = panel.getGraphSettings().getyMax();
        this.width = panel.getWidth();
        this.height = panel.getHeight();
    }
    
    public void exportToSVG(File outputFile) throws IOException {
        StringBuilder svg = new StringBuilder();
        
        // SVG header
        svg.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" ");
        svg.append("width=\"").append(width).append("\" ");
        svg.append("height=\"").append(height).append("\" ");
        svg.append("viewBox=\"0 0 ").append(width).append(" ").append(height).append("\">\n");
        
        // Add background
        svg.append("<rect width=\"").append(width).append("\" height=\"").append(height)
           .append("\" fill=\"white\" stroke=\"none\"/>\n");
        
        // Add grid
        addGridToSVG(svg);
        
        // Add axes
        addAxesToSVG(svg);
        
        // Add axis labels and tick marks
        addAxisLabelsToSVG(svg);
        
        // Add functions
        addFunctionsToSVG(svg);
        
        // Add shaded area (integral region)
        addShadedAreaToSVG(svg);
        
        // Add special points (intersections, extrema)
        addSpecialPointsToSVG(svg);
        
        // SVG footer
        svg.append("</svg>");
        
        // Write to file
        try (FileWriter writer = new FileWriter(outputFile)) {
            writer.write(svg.toString());
        }
    }
    
    private void addGridToSVG(StringBuilder svg) {
        svg.append("<g id=\"grid\" stroke=\"#e0e0e0\" stroke-width=\"0.5\">\n");
        
        // Vertical grid lines
        double gridSpacingX = calculateGridSpacing(xMax - xMin);
        for (double x = Math.ceil(xMin / gridSpacingX) * gridSpacingX; x <= xMax; x += gridSpacingX) {
            double screenX = graphToScreenX(x);
            svg.append("<line x1=\"").append(screenX).append("\" y1=\"0\" x2=\"")
               .append(screenX).append("\" y2=\"").append(height).append("\"/>\n");
        }
        
        // Horizontal grid lines
        double gridSpacingY = calculateGridSpacing(yMax - yMin);
        for (double y = Math.ceil(yMin / gridSpacingY) * gridSpacingY; y <= yMax; y += gridSpacingY) {
            double screenY = graphToScreenY(y);
            svg.append("<line x1=\"0\" y1=\"").append(screenY).append("\" x2=\"")
               .append(width).append("\" y2=\"").append(screenY).append("\"/>\n");
        }
        
        svg.append("</g>\n");
    }
    
    private void addAxesToSVG(StringBuilder svg) {
        svg.append("<g id=\"axes\" stroke=\"black\" stroke-width=\"2\">\n");
        
        // X-axis (y = 0)
        if (yMin <= 0 && yMax >= 0) {
            double screenY = graphToScreenY(0);
            svg.append("<line x1=\"0\" y1=\"").append(screenY).append("\" x2=\"")
               .append(width).append("\" y2=\"").append(screenY).append("\"/>\n");
        }
        
        // Y-axis (x = 0)
        if (xMin <= 0 && xMax >= 0) {
            double screenX = graphToScreenX(0);
            svg.append("<line x1=\"").append(screenX).append("\" y1=\"0\" x2=\"")
               .append(screenX).append("\" y2=\"").append(height).append("\"/>\n");
        }
        
        svg.append("</g>\n");
    }
    
    private void addAxisLabelsToSVG(StringBuilder svg) {
        svg.append("<g id=\"axis-labels\" font-family=\"Arial\" font-size=\"12\" fill=\"black\">\n");
        
        // X-axis labels
        double labelSpacingX = calculateGridSpacing(xMax - xMin);
        for (double x = Math.ceil(xMin / labelSpacingX) * labelSpacingX; x <= xMax; x += labelSpacingX) {
            if (Math.abs(x) < 1e-10) continue; // Skip zero
            double screenX = graphToScreenX(x);
            double screenY = (yMin <= 0 && yMax >= 0) ? graphToScreenY(0) + 15 : height - 5;
            svg.append("<text x=\"").append(screenX).append("\" y=\"").append(screenY)
               .append("\" text-anchor=\"middle\">").append(formatNumber(x)).append("</text>\n");
        }
        
        // Y-axis labels
        double labelSpacingY = calculateGridSpacing(yMax - yMin);
        for (double y = Math.ceil(yMin / labelSpacingY) * labelSpacingY; y <= yMax; y += labelSpacingY) {
            if (Math.abs(y) < 1e-10) continue; // Skip zero
            double screenX = (xMin <= 0 && xMax >= 0) ? graphToScreenX(0) - 5 : 5;
            double screenY = graphToScreenY(y) + 4;
            svg.append("<text x=\"").append(screenX).append("\" y=\"").append(screenY)
               .append("\" text-anchor=\"end\">").append(formatNumber(y)).append("</text>\n");
        }
        
        svg.append("</g>\n");
    }
    
    private void addFunctionsToSVG(StringBuilder svg) {
        svg.append("<g id=\"functions\">\n");
        
        for (Function function : graphPanel.getFunctions()) {
            if (!function.isVisible()) continue;
            
            svg.append("<path d=\"");
            boolean firstPoint = true;
            
            // Generate path data
            for (int screenX = 0; screenX <= width; screenX++) {
                double graphX = screenToGraphX(screenX);
                try {
                    double graphY = function.evaluate(graphX);
                    if (Double.isFinite(graphY)) {
                        double screenY = graphToScreenY(graphY);
                        
                        if (firstPoint) {
                            svg.append("M ").append(screenX).append(" ").append(screenY);
                            firstPoint = false;
                        } else {
                            svg.append(" L ").append(screenX).append(" ").append(screenY);
                        }
                    } else {
                        firstPoint = true; // Start new path segment
                    }
                } catch (Exception e) {
                    firstPoint = true; // Start new path segment
                }
            }
            
            svg.append("\" fill=\"none\" stroke=\"").append(colorToHex(function.getColor()))
               .append("\" stroke-width=\"2\"/>\n");
        }
        
        svg.append("</g>\n");
    }
    
    private void addShadedAreaToSVG(StringBuilder svg) {
        // Check if there's a shaded area to export
        if (graphPanel.getShadedFunction() == null) return;
        
        svg.append("<g id=\"shaded-area\" fill=\"#c8c8ff\" fill-opacity=\"0.4\">\n");
        
        // Create the path for the shaded area
        svg.append("<path d=\"");
        
        // Start from the left boundary at the x-axis (y=0)
        double startX = graphToScreenX(graphPanel.getShadeStart());
        double axisY = graphToScreenY(0);
        svg.append("M ").append(startX).append(" ").append(axisY);
        
        // Draw the function curve from start to end
        double xRange = xMax - xMin;
        for (int screenX = (int) startX; screenX <= graphToScreenX(graphPanel.getShadeEnd()); screenX++) {
            double graphX = screenToGraphX(screenX);
            if (graphX >= graphPanel.getShadeStart() && graphX <= graphPanel.getShadeEnd()) {
                try {
                    double graphY = graphPanel.getShadedFunction().evaluate(graphX);
                    if (Double.isFinite(graphY)) {
                        double screenY = graphToScreenY(graphY);
                        svg.append(" L ").append(screenX).append(" ").append(screenY);
                    }
                } catch (Exception e) {
                    // Skip points where function evaluation fails
                }
            }
        }
        
        // Close the path by going back to the x-axis
        double endX = graphToScreenX(graphPanel.getShadeEnd());
        svg.append(" L ").append(endX).append(" ").append(axisY);
        svg.append(" Z");
        
        svg.append("\"/>\n");
        svg.append("</g>\n");
    }
    
    private void addSpecialPointsToSVG(StringBuilder svg) {
        svg.append("<g id=\"special-points\">\n");
        
        // Add special points (intersections, extrema)
        List<Point> specialPoints = graphPanel.getSpecialPoints();
        if (specialPoints != null) {
            for (Point point : specialPoints) {
                double screenX = graphToScreenX(point.getX());
                double screenY = graphToScreenY(point.getY());
                svg.append("<circle cx=\"").append(screenX).append("\" cy=\"").append(screenY)
                   .append("\" r=\"4\" fill=\"red\" stroke=\"white\" stroke-width=\"1\"/>\n");
            }
        }
        
        svg.append("</g>\n");
    }
    
    // Helper methods
    private double graphToScreenX(double graphX) {
        return (graphX - xMin) / (xMax - xMin) * width;
    }
    
    private double graphToScreenY(double graphY) {
        return (yMax - graphY) / (yMax - yMin) * height;
    }
    
    private double screenToGraphX(double screenX) {
        return xMin + (screenX / width) * (xMax - xMin);
    }
    
    private String colorToHex(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }
    
    private String formatNumber(double value) {
        if (Math.abs(value) < 1e-10) return "0";
        if (Math.abs(value - Math.round(value)) < 1e-10) {
            return String.valueOf((int) Math.round(value));
        }
        return String.format("%.2f", value);
    }
    
    private double calculateGridSpacing(double range) {
        double magnitude = Math.pow(10, Math.floor(Math.log10(range)));
        double normalized = range / magnitude;
        
        if (normalized <= 2) return magnitude / 2;
        if (normalized <= 5) return magnitude;
        return magnitude * 2;
    }
} 