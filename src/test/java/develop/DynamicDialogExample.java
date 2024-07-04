package develop;

import bdv.tools.boundingbox.TransformedBoxEditor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

public class DynamicDialogExample {
    public static void main(String[] args) {
        // Create the frame
        JFrame frame = new JFrame("Conditional TextField Example");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(300, 200);
        frame.setLayout(new FlowLayout());

        // Create the combo box (choice)
        String[] choices = {"Select an option", "Show TextField", "Other"};
        JComboBox<String> comboBox = new JComboBox<>(choices);
        frame.add(comboBox);

        // Create the text field
        JTextField textField = new JTextField(15);
        textField.setVisible(false); // Initially hidden
        frame.add(textField);

        // Add item listener to the combo box
        comboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    String selectedItem = (String) comboBox.getSelectedItem();
                    if ("Show TextField".equals(selectedItem)) {
                        textField.setVisible(true);
                    } else {
                        textField.setVisible(false);
                    }
                    frame.revalidate();
                    frame.repaint();
                }
            }
        });

        // Display the frame
        frame.setVisible(true);
    }
}