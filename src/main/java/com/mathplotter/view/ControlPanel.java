package com.mathplotter.view;

import com.mathplotter.model.Function;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

public class ControlPanel extends JPanel {

    private final FunctionInputPanel functionInputPanel;
    private final CalculationPanel calculationPanel;
    private final JPanel viewControlPanel;
    private final JButton zoomInButton, zoomOutButton, resetButton;

    public ControlPanel(GraphPanel graphPanel) {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(300, 800));
        setBorder(BorderFactory.createTitledBorder("Controls"));

        functionInputPanel = new FunctionInputPanel(graphPanel, this);
        calculationPanel = new CalculationPanel(graphPanel, this);
        
        // View controls
        viewControlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        viewControlPanel.setBorder(BorderFactory.createTitledBorder("View"));
        Dimension btnSize = new Dimension(90, 24);
        zoomInButton = new JButton("Zoom In");
        zoomInButton.setPreferredSize(btnSize);
        zoomOutButton = new JButton("Zoom Out");
        zoomOutButton.setPreferredSize(btnSize);
        resetButton = new JButton("Center");
        resetButton.setPreferredSize(btnSize);
        viewControlPanel.add(zoomInButton);
        viewControlPanel.add(zoomOutButton);
        viewControlPanel.add(resetButton);

        // Add action listeners
        zoomInButton.addActionListener(e -> graphPanel.zoom(1.2));
        zoomOutButton.addActionListener(e -> graphPanel.zoom(0.8));
        resetButton.addActionListener(e -> graphPanel.resetView());

        // Layout
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.add(functionInputPanel);
        container.add(viewControlPanel);
        container.add(calculationPanel);

        add(container, BorderLayout.NORTH);
    }

    public void updateFunctionLists(List<Function> functions) {
        calculationPanel.updateFunctionLists(functions);
        functionInputPanel.updateFunctionList(functions);
    }
} 