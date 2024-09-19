package org.embl.mobie.ui;

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
import java.util.Arrays;

import static org.embl.mobie.lib.view.save.ViewSaver.CREATE_SELECTION_GROUP;

public class StringArraySelectorDialog
{
    private final String title;
    private final String[] array;

    private JComboBox< String > arrayComboBox;

    private boolean isOkPressed;

    private JDialog dialog;

    public StringArraySelectorDialog( String title, String[] array )
    {
        this.title = title;
        this.array = array;
    }

    public boolean show()
    {
        dialog = new JDialog( ( Frame ) null, title, true );
        dialog.setLayout( new BoxLayout( dialog.getContentPane(), BoxLayout.Y_AXIS ) );

        // Array item selection
        //
        JPanel selectionPanel = SwingHelper.horizontalLayoutPanel();
        arrayComboBox = new JComboBox<>( array );
        Dimension maximumSize = new Dimension( 300, 20 );
        arrayComboBox.setMaximumSize( maximumSize );
        selectionPanel.add( new JLabel("Select:  ") );
        selectionPanel.add( arrayComboBox );

        dialog.add( selectionPanel );

        // OK and Cancel button
        //
        JPanel buttonPanel = new JPanel();
        JButton okButton = new JButton( "OK" );
        JButton cancelButton = new JButton( "Cancel" );
        buttonPanel.add( okButton );
        buttonPanel.add( cancelButton );
        dialog.add( buttonPanel );

        okButton.addActionListener( e ->
        {
            isOkPressed = true;
            dialog.setVisible( false );
        } );

        cancelButton.addActionListener( e ->
        {
            isOkPressed = false;
            dialog.setVisible( false );
        } );

        dialog.setPreferredSize( new Dimension( 250, 120 ) );
        dialog.setLocation(
                Toolkit.getDefaultToolkit().getScreenSize().width / 2 - 200,
                Toolkit.getDefaultToolkit().getScreenSize().height / 2 - 200
        );
        dialog.pack();
        dialog.setVisible( true );

        return isOkPressed;
    }

    public String getSelectedItem()
    {
        return (String) arrayComboBox.getSelectedItem();
    }
}

