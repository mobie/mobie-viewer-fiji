package org.embl.mobie.lib.create.ui;

import org.embl.mobie.MoBIE;
import org.embl.mobie.lib.io.FileLocation;
import org.embl.mobie.lib.serialize.View;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;

import static org.embl.mobie.lib.view.save.ViewSaver.CREATE_SELECTION_GROUP;

public class ViewSaverDialog
{
    private JComboBox< FileLocation > fileLocation;
    private final View view;
    private JTextField textField;
    private boolean isOkPressed;
    private JTextField viewJsonPath;
    private JPanel viewJsonPathPanel;
    private JDialog dialog;
    private JCheckBox makeViewExclusive;
    private JComboBox< String > selectionGroup;
    private JTextField newSelectionGroup;

    public ViewSaverDialog( View view )
    {
        this.view = view;
    }

    public boolean show()
    {
        String dialogTitle = "Save view" + ( view.getName() == null ? "" : ": " + view.getName() );
        dialog = new JDialog( ( Frame ) null, dialogTitle, true );
        dialog.setLayout( new BoxLayout( dialog.getContentPane(), BoxLayout.Y_AXIS ) );

        // Save location
        //
        fileLocation = new JComboBox<>( FileLocation.values() );
        dialog.add( fileLocation );

        viewJsonPath = new JTextField( "JSON Path", 30 );
        JButton browseButton = getBrowseButton();
        viewJsonPathPanel = new JPanel();
        viewJsonPathPanel.add( viewJsonPath );
        viewJsonPathPanel.add( browseButton );
        dialog.add( viewJsonPathPanel );
        viewJsonPathPanel.setVisible( false );

        // Add item listener to the combo box
        fileLocation.addItemListener( new ItemListener()
        {
            @Override
            public void itemStateChanged( ItemEvent e )
            {
                if ( e.getStateChange() == ItemEvent.SELECTED )
                {
                    FileLocation selectedItem = ( FileLocation ) fileLocation.getSelectedItem();
                    if ( FileLocation.ExternalJSONFile.equals( selectedItem ) )
                        viewJsonPathPanel.setVisible( true );
                    else
                        viewJsonPathPanel.setVisible( false );
                    dialog.revalidate();
                    dialog.repaint();
                }
            }
        } );

        // Selection group for UI
        //
        selectionGroup = new JComboBox<>( getSelectionGroupChoices() );
        dialog.add( selectionGroup );
        newSelectionGroup = new JTextField( "Selection group", 30 );
        dialog.add( newSelectionGroup );
        newSelectionGroup.setVisible( false );

        selectionGroup.addItemListener( new ItemListener()
        {
            @Override
            public void itemStateChanged( ItemEvent e )
            {
                if ( e.getStateChange() == ItemEvent.SELECTED )
                {
                    String selectedItem = ( String ) selectionGroup.getSelectedItem();
                    if ( CREATE_SELECTION_GROUP.equals( selectedItem ) )
                    {
                        newSelectionGroup.setVisible( true );
                    } else
                    {
                        newSelectionGroup.setVisible( false );
                    }
                    dialog.revalidate();
                    dialog.repaint();
                }
            }
        } );

        // Exclusive
        //
        makeViewExclusive = new JCheckBox( "Make view exclusive" );
        dialog.add( makeViewExclusive );


        JPanel buttonPanel = new JPanel();
        JButton okButton = new JButton( "OK" );
        JButton cancelButton = new JButton( "Cancel" );
        buttonPanel.add( okButton );
        buttonPanel.add( cancelButton );
        dialog.add( buttonPanel );

        okButton.addActionListener( new ActionListener()
        {
            public void actionPerformed( ActionEvent e )
            {
                isOkPressed = true;
                dialog.setVisible( false );
            }
        } );

        cancelButton.addActionListener( new ActionListener()
        {
            public void actionPerformed( ActionEvent e )
            {
                isOkPressed = false;
                dialog.setVisible( false );
                // how to return false from the containing method
            }
        } );

        dialog.pack();
        dialog.setVisible( true );
        return isOkPressed;
    }

    private String[] getSelectionGroupChoices()
    {
        String[] currentUiSelectionGroups = MoBIE.getInstance().getUserInterface().getUISelectionGroupNames();
        String[] choices = new String[ currentUiSelectionGroups.length + 1 ];
        choices[ 0 ] = CREATE_SELECTION_GROUP;
        System.arraycopy( currentUiSelectionGroups, 0, choices, 1, currentUiSelectionGroups.length );
        return choices;
    }

    @NotNull
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

                // Show the file chooser dialog
                int result = fileChooser.showOpenDialog( null );

                // If a file is selected, set the text field with the file path
                if ( result == JFileChooser.APPROVE_OPTION )
                {
                    File selectedFile = fileChooser.getSelectedFile();
                    viewJsonPath.setText( selectedFile.getAbsolutePath() );
                }
            }
        } );
        return browseButton;
    }

    public FileLocation getFileLocation()
    {
        return ( FileLocation ) fileLocation.getSelectedItem();
    }

    public boolean getMakeViewExclusive()
    {
        return makeViewExclusive.isSelected();
    }
}

