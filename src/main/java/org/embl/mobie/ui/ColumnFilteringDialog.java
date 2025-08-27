package org.embl.mobie.ui;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class ColumnFilteringDialog
{
    private final List< String > columnNames;
    private JComboBox< String > columnComboBox;
    private JTextField valueField;
    private JCheckBox keepSelected;
    private boolean isOkPressed;

    public ColumnFilteringDialog( List< String > columnNames )
    {
        this.columnNames = columnNames;
    }

    public boolean show()
    {
        // Show dialog
        //
        JDialog dialog = new JDialog( ( Frame ) null, "Color by Column", true );
        dialog.setLayout( new BoxLayout( dialog.getContentPane(), BoxLayout.Y_AXIS ) );

        addColumnSelection( dialog );

        addValueField( dialog );

        addPaintZeroTransparentCheckbox( dialog );

        addOKCancelButton( dialog );

        //dialog.setPreferredSize( new Dimension( 250, 200 ) );
        dialog.setLocation(
                Toolkit.getDefaultToolkit().getScreenSize().width / 2 - 200,
                Toolkit.getDefaultToolkit().getScreenSize().height / 2 - 200
        );
        dialog.pack();
        dialog.setVisible( true );

        return isOkPressed;
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

    private void addPaintZeroTransparentCheckbox( JDialog dialog )
    {
        JPanel panel = SwingHelper.horizontalFlowLayoutPanel();
        keepSelected = new JCheckBox( );
        panel.add( new JLabel("Paint zero transparent:  ") );
        panel.add( keepSelected );
        dialog.add( panel );
    }

    private void addColumnSelection( JDialog dialog )
    {
        JPanel panel = SwingHelper.horizontalFlowLayoutPanel();
        columnComboBox = new JComboBox<>( columnNames.toArray( new String[ 0 ] ) );
        //Dimension maximumSize = new Dimension( 300, 20 );
        //columnComboBox.setMaximumSize( maximumSize );
        panel.add( new JLabel("Column:  ") );
        panel.add( columnComboBox );
        dialog.add( panel );
    }

    private void addValueField( JDialog dialog )
    {
        JPanel panel = SwingHelper.horizontalFlowLayoutPanel();
        valueField = new JTextField( "                           0" );
        //Dimension maximumSize = new Dimension( 300, 20 );
        //valueField.setMaximumSize( maximumSize );
        panel.add( new JLabel("Value:  ") );
        panel.add( valueField );
        dialog.add( panel );
    }

    public String getColumnName()
    {
        return (String) columnComboBox.getSelectedItem();
    }

    public String getValue()
    {
        return valueField.getText().trim();
    }

    public boolean getKeepSelected()
    {
        return keepSelected.isSelected();
    }

}

