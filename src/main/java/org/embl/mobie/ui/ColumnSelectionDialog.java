package org.embl.mobie.ui;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class ColumnSelectionDialog
{
    private static String lut;
    private static String columnName;
    private final List< String > columnNames;
    private JComboBox< String > columnComboBox;
    private boolean isOkPressed;

    public ColumnSelectionDialog( List< String > columnNames )
    {
        this.columnNames = columnNames;
    }

    public boolean show()
    {
        // Show dialog
        //
        JDialog dialog = new JDialog( ( Frame ) null, "Select Column", true );
        dialog.setLayout( new BoxLayout( dialog.getContentPane(), BoxLayout.Y_AXIS ) );

        addColumnSelection( dialog );

        addOKCancelButton( dialog );

        //dialog.setPreferredSize( new Dimension( 250, 200 ) );
        dialog.setLocation(
                Toolkit.getDefaultToolkit().getScreenSize().width / 2 - 200,
                Toolkit.getDefaultToolkit().getScreenSize().height / 2 - 200
        );
        dialog.pack();
        dialog.setVisible( true );

        // Consume dialog fields
        //
        if ( ! isOkPressed ) return false;

        // remember for next time building the UI
        columnName = getColumnName();

        return true;
    }

    private void addOKCancelButton( JDialog dialog )
    {
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
    }

    private void addColumnSelection( JDialog dialog )
    {
        JPanel panel = SwingHelper.horizontalFlowLayoutPanel();
        columnComboBox = new JComboBox<>( columnNames.toArray(new String[0]) );
        Dimension maximumSize = new Dimension( 300, 20 );
        columnComboBox.setMaximumSize( maximumSize );
        panel.add( new JLabel("Column:  ") );
        panel.add( columnComboBox );
        dialog.add( panel );
    }

    public String getColumnName()
    {
        return (String) columnComboBox.getSelectedItem();
    }

}

