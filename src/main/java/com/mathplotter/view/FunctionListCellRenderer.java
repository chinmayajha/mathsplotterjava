package com.mathplotter.view;

import com.mathplotter.model.Function;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class FunctionListCellRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(
            JList<?> list, Object value, int index,
            boolean isSelected, boolean cellHasFocus) {
        
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        
        if (value instanceof Function) {
            Function function = (Function) value;
            
            setText(function.getExpression());
            setIcon(createColorIcon(function.getColor()));
            
            if (!function.isVisible()) {
                setForeground(Color.GRAY);
                setFont(getFont().deriveFont(Font.ITALIC));
            }
            
            setToolTipText(String.format("Expression: %s | Visible: %s",
                function.getExpression(), function.isVisible() ? "Yes" : "No"));
        }
        
        return this;
    }
    
    private Icon createColorIcon(Color color) {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = img.createGraphics();
        g2d.setColor(color);
        g2d.fillRect(0, 0, 16, 16);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(0, 0, 15, 15);
        g2d.dispose();
        return new ImageIcon(img);
    }
} 