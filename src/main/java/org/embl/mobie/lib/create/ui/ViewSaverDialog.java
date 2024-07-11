package org.embl.mobie.lib.create.ui;

import org.embl.mobie.MoBIE;
import org.embl.mobie.lib.io.FileLocation;
import org.embl.mobie.lib.serialize.View;
import org.embl.mobie.ui.SwingHelper;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.Arrays;

import static org.embl.mobie.lib.view.save.ViewSaver.CREATE_SELECTION_GROUP;

public class ViewSaverDialog
{

    private static final JComboBox< FileLocation > fileLocation = new JComboBox<>( FileLocation.values() );
    private static final JTextField viewJsonPath = new JTextField( "", 20 );
    private static final JCheckBox makeViewExclusive = new JCheckBox( "Make view exclusive" );
    private static String selectedViewGroup;

    private JComboBox< String > viewGroup;

    private final View view;
    private boolean isOkPressed;

    private JDialog dialog;
    private JTextField newGroup;
    private JTextField viewName;

    public ViewSaverDialog( View view )
    {
        this.view = view;
    }

    public boolean show()
    {
        String dialogTitle = "Save view" + ( view.getName() == null ? "" : ": " + view.getName() );
        dialog = new JDialog( ( Frame ) null, dialogTitle, true );
        dialog.setLayout( new BoxLayout( dialog.getContentPane(), BoxLayout.Y_AXIS ) );

        Dimension maximumSize = new Dimension( 300, 20 );

        // View name
        //
        if ( view.getName() == null )
        {
            JPanel viewNamePanel = SwingHelper.horizontalLayoutPanel();
            viewName = new JTextField( "", 15 );
            viewName.setMaximumSize( maximumSize );
            viewNamePanel.add( new JLabel("View name:  ") );
            viewNamePanel.add( viewName );
            //viewNamePanel.add( Box.createHorizontalGlue() );
            dialog.add( viewNamePanel );
        }

        // View description
        // TODO: add dialog field and consume it here

        // Save location
        //
        JPanel locationPanel = SwingHelper.horizontalLayoutPanel();
        fileLocation.setMaximumSize( maximumSize );
        locationPanel.add( new JLabel("Save location:  ") );
        locationPanel.add( fileLocation );
        dialog.add( locationPanel );

        JPanel jsonPathPanel = SwingHelper.horizontalLayoutPanel();
        viewJsonPath.setMaximumSize( maximumSize );
        JButton browseButton = getBrowseButton();
        jsonPathPanel.add( new JLabel("File path:  ") );
        jsonPathPanel.add( viewJsonPath );
        jsonPathPanel.add( browseButton );
        dialog.add( jsonPathPanel );

        // Add item listener to the combo box
        fileLocation.addItemListener( new ItemListener()
        {
            @Override
            public void itemStateChanged( ItemEvent e )
            {
                if ( e.getStateChange() == ItemEvent.SELECTED )
                {
                    FileLocation selectedItem = ( FileLocation ) fileLocation.getSelectedItem();
                    jsonPathPanel.setVisible( FileLocation.ExternalFile.equals( selectedItem ) );
                    dialog.revalidate();
                    dialog.repaint();
                }
            }
        } );

        // Selection group for UI
        //
        JPanel viewGroupPanel = SwingHelper.horizontalLayoutPanel();
        String[] viewGroupChoices = getViewGroupChoices();
        viewGroup = new JComboBox<>( viewGroupChoices );
        if ( selectedViewGroup != null &&
             Arrays.asList( viewGroupChoices ).contains( selectedViewGroup ) )
        {
            viewGroup.setSelectedItem( selectedViewGroup );
        }
        viewGroup.setMaximumSize( maximumSize );
        viewGroupPanel.add( new JLabel("View group:  ") );
        viewGroupPanel.add( viewGroup );
        dialog.add( viewGroupPanel );

        JPanel newGroupPanel = SwingHelper.horizontalLayoutPanel();
        newGroup = new JTextField( "", 30 );
        newGroup.setMaximumSize( maximumSize );
        newGroupPanel.add( new JLabel("Group name:   ") );
        newGroupPanel.add( newGroup );
        dialog.add( newGroupPanel );

        viewGroup.addItemListener( new ItemListener()
        {
            @Override
            public void itemStateChanged( ItemEvent e )
            {
                if ( e.getStateChange() == ItemEvent.SELECTED )
                {
                    String selectedItem = ( String ) viewGroup.getSelectedItem();
                    newGroupPanel.setVisible( CREATE_SELECTION_GROUP.equals( selectedItem ) );
                    dialog.revalidate();
                    dialog.repaint();
                }
            }
        } );

        // Exclusive
        //
        JPanel checkBoxPanel = SwingHelper.horizontalLayoutPanel();
        checkBoxPanel.add( makeViewExclusive );
        dialog.add( checkBoxPanel );

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
            }
        } );

        dialog.setPreferredSize( new Dimension( 400, 300 ) );
        dialog.setLocation(
                Toolkit.getDefaultToolkit().getScreenSize().width / 2 - 200,
                Toolkit.getDefaultToolkit().getScreenSize().height / 2 - 200
        );
        dialog.pack();
        jsonPathPanel.setVisible( false );
        newGroupPanel.setVisible( false );
        dialog.setVisible( true );

        return isOkPressed;
    }

    private static String[] getViewGroupChoices()
    {
        String[] groupNames = MoBIE.getInstance().getUserInterface().getUISelectionGroupNames();
        String[] choices = new String[ groupNames.length + 1 ];
        choices[ groupNames.length ] = CREATE_SELECTION_GROUP;
        System.arraycopy( groupNames, 0, choices, 0, groupNames.length );
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

    public String getViewName()
    {
        return viewName.getText();
    }

    public String getViewJsonPath()
    {
        return viewJsonPath.getText();
    }

    public String getViewGroup()
    {
        selectedViewGroup = ( String ) viewGroup.getSelectedItem();
        return selectedViewGroup;
    }

    public String getNewGroup()
    {
        selectedViewGroup = newGroup.getText();
        return selectedViewGroup;
    }
}

