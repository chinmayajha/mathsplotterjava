package com.mathplotter.view;

import com.mathplotter.model.Function;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Random;

public class FunctionInputPanel extends JPanel {

    private final JTextField functionInputField;
    private final JButton addFunctionButton;
    private final JButton deleteButton;
    private final JButton deleteAllButton;
    private final JList<Function> functionList;
    private final DefaultListModel<Function> listModel;
    private final GraphPanel graphPanel;
    private final ControlPanel controlPanel;

    public FunctionInputPanel(GraphPanel graphPanel, ControlPanel controlPanel) {
        this.graphPanel = graphPanel;
        this.controlPanel = controlPanel;

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Functions"));

        // Input components
        JPanel inputPanel = new JPanel(new BorderLayout());
        functionInputField = new JTextField("sin(x)");
        addFunctionButton = new JButton("Add");
        inputPanel.add(functionInputField, BorderLayout.CENTER);
        inputPanel.add(addFunctionButton, BorderLayout.EAST);

        // List of functions
        listModel = new DefaultListModel<>();
        functionList = new JList<>(listModel);
        functionList.setCellRenderer(new FunctionListCellRenderer());
        functionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(functionList);

        // Deletion buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        deleteButton = new JButton("Delete");
        deleteAllButton = new JButton("Delete All");
        deleteButton.setEnabled(false);
        buttonPanel.add(deleteButton);
        buttonPanel.add(deleteAllButton);

        add(inputPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        // Action listeners
        addFunctionButton.addActionListener(e -> addFunction());
        deleteButton.addActionListener(e -> deleteSelectedFunction());
        deleteAllButton.addActionListener(e -> deleteAllFunctions());

        functionList.addListSelectionListener(e -> deleteButton.setEnabled(!functionList.isSelectionEmpty()));
        
        // Add context menu for right-click options
        setupContextMenu();

        // Double-click to delete
        functionList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    deleteSelectedFunction();
                }
            }
        });
        
        functionList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                    deleteSelectedFunction();
                }
            }
        });
    }

    private void deleteSelectedFunction() {
        Function selectedFunction = functionList.getSelectedValue();
        if (selectedFunction != null) {
            int result = JOptionPane.showConfirmDialog(
                this,
                "Delete function: " + selectedFunction.getExpression() + "?",
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION);

            if (result == JOptionPane.YES_OPTION) {
                graphPanel.removeFunction(selectedFunction);
                controlPanel.updateFunctionLists(graphPanel.getFunctions());
            }
        }
    }

    private void deleteAllFunctions() {
        if (listModel.getSize() > 0) {
            int result = JOptionPane.showConfirmDialog(
                this,
                "Delete all " + listModel.getSize() + " functions?",
                "Confirm Delete All",
                JOptionPane.YES_NO_OPTION);
            
            if (result == JOptionPane.YES_OPTION) {
                graphPanel.removeAllFunctions();
                controlPanel.updateFunctionLists(graphPanel.getFunctions());
            }
        }
    }

    private void addFunction() {
        String expression = functionInputField.getText();
        if (expression.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Function expression cannot be empty.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            // Create a random color for the new function
            Random rand = new Random();
            Color color = new Color(rand.nextInt(200), rand.nextInt(200), rand.nextInt(200));

            // Don't add a function that already exists.
            if (graphPanel.getFunctions().stream().anyMatch(f -> f.getExpression().equals(expression))) {
                JOptionPane.showMessageDialog(this, "Function already exists.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Function newFunction = new Function(expression, color);
            graphPanel.addFunction(newFunction);
            
            // This now becomes the single source of truth for updating all lists.
            controlPanel.updateFunctionLists(graphPanel.getFunctions());

            functionInputField.setText("");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error parsing function: " + ex.getMessage(), "Parsing Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void setupContextMenu() {
        JPopupMenu contextMenu = new JPopupMenu();
        JMenuItem deleteItem = new JMenuItem("Delete");
        deleteItem.addActionListener(e -> deleteSelectedFunction());
        contextMenu.add(deleteItem);
        functionList.setComponentPopupMenu(contextMenu);
    }

    public void updateFunctionList(List<Function> functions) {
        listModel.clear();
        for (Function func : functions) {
            listModel.addElement(func);
        }
    }
} 