package org.embl.mobie.lib.create.ui;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class FileChooserWithBrowse extends JDialog  {

    JTextField pathTextField;
    boolean isOkPressed;

    public FileChooserWithBrowse(String dialogTitle, String label) {

        // Set as modal dialog, so MoBIE will wait for it to close
        super((Frame) null, dialogTitle, true);

        Container contentPane = this.getContentPane();
        contentPane.setLayout( new BoxLayout(contentPane, BoxLayout.PAGE_AXIS ) );
        this.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );

        JPanel pathPanel = getPathPanel( label );
        contentPane.add(pathPanel);

        JPanel buttonPanel = getButtonPanel();
        contentPane.add(buttonPanel);

        this.pack();
        this.setLocationRelativeTo(null);
        this.setVisible( true );
    }

    public String getPath() {
        if ( isOkPressed ) {
            return pathTextField.getText();
        } else {
            return null;
        }
    }

    private JPanel getPathPanel( String label ) {
        JPanel pathPanel = new JPanel();
        pathPanel.setLayout( new BoxLayout(pathPanel, BoxLayout.LINE_AXIS ) );
        pathPanel.setBorder( BorderFactory.createEmptyBorder(10, 10, 10, 10) );

        JLabel pathLabel = new JLabel( label );
        pathTextField = new JTextField(50);
        JButton browseButton = getBrowseButton();

        pathPanel.add(pathLabel);
        pathPanel.add(Box.createRigidArea(new Dimension(5,0)));
        pathPanel.add(pathTextField);
        pathPanel.add(Box.createRigidArea(new Dimension(5,0)));
        pathPanel.add(browseButton);

        return pathPanel;
    }

    private JPanel getButtonPanel() {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout( new FlowLayout(FlowLayout.RIGHT) );
        buttonPanel.setBorder( BorderFactory.createEmptyBorder(5, 10, 10, 10) );

        JButton okButton = new JButton( "OK" );
        JButton cancelButton = new JButton( "Cancel" );
        buttonPanel.add( okButton );
        buttonPanel.add( cancelButton );

        okButton.addActionListener( new ActionListener()
        {
            public void actionPerformed( ActionEvent e )
            {
                isOkPressed = true;
                dispose();
            }
        } );

        cancelButton.addActionListener( new ActionListener()
        {
            public void actionPerformed( ActionEvent e )
            {
                isOkPressed = false;
                dispose();
            }
        } );

        return buttonPanel;
    }

    private JButton getBrowseButton()
    {
        JButton browseButton = new JButton( "Browse" );

        browseButton.addActionListener( new ActionListener()
        {
            @Override
            public void actionPerformed( ActionEvent e )
            {
                // Create a JFileChooser
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

                // Show the file chooser dialog
                int result = fileChooser.showOpenDialog( null );

                // If a file is selected, set the text field with the file path
                if ( result == JFileChooser.APPROVE_OPTION )
                {
                    File selectedFile = fileChooser.getSelectedFile();
                    pathTextField.setText( selectedFile.getAbsolutePath() );
                }
            }
        } );
        return browseButton;
    }
}