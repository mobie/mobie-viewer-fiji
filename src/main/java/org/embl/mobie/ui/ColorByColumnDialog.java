package org.embl.mobie.ui;

import net.imglib2.util.Pair;
import org.embl.mobie.lib.annotation.Annotation;
import org.embl.mobie.lib.color.ColoringModel;
import org.embl.mobie.lib.color.ColoringModels;
import org.embl.mobie.lib.color.lut.LUTs;
import org.embl.mobie.lib.table.AnnotationTableModel;

import javax.swing.*;
import java.awt.*;

import static org.embl.mobie.lib.color.lut.LUTs.COLORING_LUTS;
import static org.embl.mobie.lib.color.lut.LUTs.TRANSPARENT;

public class ColorByColumnDialog< A extends Annotation >
{
    private static String lut;
    private static String columnName;
    private static boolean paintZeroTransparent;

    private final AnnotationTableModel< A > table;
    private JComboBox< String > columnComboBox;
    private JComboBox< String > lutComboBox;
    private JCheckBox paintZeroTransparentCheckBox;

    private boolean isOkPressed;

    public ColorByColumnDialog( AnnotationTableModel< A > table )
    {
        this.table = table;
    }

    public ColoringModel< A > show()
    {
        // Show dialog
        //
        JDialog dialog = new JDialog( ( Frame ) null, "Color by Column", true );
        dialog.setLayout( new BoxLayout( dialog.getContentPane(), BoxLayout.Y_AXIS ) );

        addColumnSelection( dialog );

        addLutSelection( dialog );

        addPaintZeroTransparentCheckbox( dialog );

        addOKCancelButton( dialog );

        dialog.setPreferredSize( new Dimension( 250, 200 ) );
        dialog.setLocation(
                Toolkit.getDefaultToolkit().getScreenSize().width / 2 - 200,
                Toolkit.getDefaultToolkit().getScreenSize().height / 2 - 200
        );
        dialog.pack();
        dialog.setVisible( true );

        // Consume dialog fields
        //
        if ( ! isOkPressed ) return null;

        columnName = getColumnName();
        lut = getLut();
        paintZeroTransparent = getPaintZeroTransparent();

        if ( paintZeroTransparent )
            lut += LUTs.ZERO_TRANSPARENT;

        if ( Number.class.isAssignableFrom( table.columnClass( columnName ) ) )
        {
            final Pair< Double, Double > minMax = table.getMinMax( columnName );
            return ColoringModels.createNumericModel( columnName, lut, minMax, true );
        }
        else
        {
            return ColoringModels.createCategoricalModel( columnName, lut, TRANSPARENT );
        }
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
        JPanel panel = SwingHelper.horizontalLayoutPanel();
        paintZeroTransparentCheckBox = new JCheckBox( );
        panel.add( new JLabel("Paint zero transparent:  ") );
        panel.add( paintZeroTransparentCheckBox );
        dialog.add( panel );
    }

    private void addColumnSelection( JDialog dialog )
    {
        JPanel panel = SwingHelper.horizontalLayoutPanel();
        columnComboBox = new JComboBox<>( table.columnNames().toArray( new String[ 0 ] ) );
        Dimension maximumSize = new Dimension( 300, 20 );
        columnComboBox.setMaximumSize( maximumSize );
        panel.add( new JLabel("Column:  ") );
        panel.add( columnComboBox );
        dialog.add( panel );
    }

    private void addLutSelection( JDialog dialog )
    {
        JPanel panel = SwingHelper.horizontalLayoutPanel();
        lutComboBox = new JComboBox<>( COLORING_LUTS );
        lutComboBox.setSelectedItem( lut == null ? COLORING_LUTS[ 0 ] : lut );
        Dimension maximumSize = new Dimension( 300, 20 );
        lutComboBox.setMaximumSize( maximumSize );
        panel.add( new JLabel("Color:  ") );
        panel.add( lutComboBox );
        dialog.add( panel );
    }

    private String getColumnName()
    {
        return (String) columnComboBox.getSelectedItem();
    }

    private String getLut()
    {
        return (String) lutComboBox.getSelectedItem();
    }

    private boolean getPaintZeroTransparent()
    {
        return paintZeroTransparentCheckBox.isSelected();
    }


}

