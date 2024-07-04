package develop;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class SimpleDialog  {
    private JTextField textField;
    private boolean isOkPressed;

    public SimpleDialog() {
        JDialog dialog = new JDialog((Frame) null, "Simple Dialog", true);
        textField = new JTextField(20);
        JPanel panel = new JPanel();
        panel.add(new JLabel("Enter something:"));
        panel.add(textField);
        dialog.add(panel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                isOkPressed = true;
                dialog.setVisible(false);
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                isOkPressed = false;
                dialog.setVisible(false);
            }
        });

        dialog.pack();
        dialog.setVisible( true );
    }

    public boolean isOkPressed() {
        return isOkPressed;
    }

    public String getInput() {
        return textField.getText();
    }

    public static void main(String[] args) {

        SimpleDialog dialog = new SimpleDialog();

        if (dialog.isOkPressed()) {
            System.out.println("User input: " + dialog.getInput());
        } else {
            System.out.println("User cancelled the input.");
        }
    }
}