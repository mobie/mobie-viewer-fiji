package org.embl.mobie.ui;

import ij.IJ;
import net.imglib2.util.Pair;
import org.embl.mobie.lib.annotation.Annotation;
import org.embl.mobie.lib.color.AdditiveColoringModel;
import org.embl.mobie.lib.color.ColoringModel;
import org.embl.mobie.lib.color.ColoringModels;
import org.embl.mobie.lib.color.lut.LUTs;
import org.embl.mobie.lib.table.AnnotationTableModel;

import javax.swing.*;
import java.awt.*;
import org.embl.mobie.lib.color.NumericAnnotationColoringModel;

import static org.embl.mobie.lib.color.lut.LUTs.COLORING_LUTS;
import static org.embl.mobie.lib.color.lut.LUTs.TRANSPARENT;

public class ColorByColumnDialog< A extends Annotation >
{
    // The name of the dialog
    public static final String COLOR_BY_COLUMN = "Color by Column";

    private static String lutName;
    private static String columnName;
    private static boolean paintZeroTransparent;
    private final AnnotationTableModel< A > table;
    private final ColoringModel< A > currentColoringModel;
    private JComboBox< String > columnComboBox;
    private JComboBox< String > lutComboBox;
    private JCheckBox paintZeroTransparentCheckBox;
    private JCheckBox highValuesTransparentCheckBox;
    private JRadioButton replaceColoringRadioButton;
    private JRadioButton addToExistingColoringRadioButton;
    private static boolean highValuesTransparent;
    private boolean isOkPressed;

    public ColorByColumnDialog( AnnotationTableModel< A > table )
    {
        this( table, null );
    }

    public ColorByColumnDialog( AnnotationTableModel< A > table, ColoringModel< A > currentColoringModel )
    {
        this.table = table;
        this.currentColoringModel = currentColoringModel;
    }

    public ColoringModel< A > show()
    {
        // Show dialog
        //
        // Use centralized UI constant for dialog title
        JDialog dialog = new JDialog( ( Frame ) null, COLOR_BY_COLUMN, true );
        dialog.setLayout( new BoxLayout( dialog.getContentPane(), BoxLayout.Y_AXIS ) );

        addColumnSelection( dialog );

        addLutSelection( dialog );

        addPaintZeroTransparentCheckbox( dialog );
        addHighValuesTransparentCheckbox( dialog );

        addColoringModeSelection( dialog );

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
        if ( ! isOkPressed ) return null;

        columnName = getColumnName();
        lutName = getLutName();
        paintZeroTransparent = getPaintZeroTransparent();

        if ( paintZeroTransparent )
            lutName += LUTs.ZERO_TRANSPARENT;

        highValuesTransparent = getHighValuesTransparent();
        final ColoringModel< A > coloringModel = createColoringModel();

        // If the created coloring model is numeric, apply the high-values-transparent setting
        if ( coloringModel instanceof NumericAnnotationColoringModel )
        {
            @SuppressWarnings("unchecked")
            final NumericAnnotationColoringModel< A > numericModel = ( NumericAnnotationColoringModel< A > ) coloringModel;
            numericModel.setHighValuesTransparent( highValuesTransparent );
        }

        if ( addToExistingColoring() && currentColoringModel instanceof AdditiveColoringModel )
            return addToExistingAdditiveColoringModel( coloringModel );

        return new AdditiveColoringModel<>( coloringModel );
    }

    private ColoringModel< A > addToExistingAdditiveColoringModel( ColoringModel< A > coloringModel )
    {
        final String name = ColoringModels.getName( coloringModel );
        final AdditiveColoringModel< A > additiveColoringModel = ( AdditiveColoringModel< A > ) currentColoringModel;
        if ( additiveColoringModel.containsColoringModel( name ) )
            return showDuplicateColoringWarning( name );

        additiveColoringModel.addColoringModel( name, coloringModel );
        return additiveColoringModel;
    }

    private ColoringModel< A > showDuplicateColoringWarning( String name )
    {
        IJ.showMessage( "Coloring " + name + " is already active.\nPlease choose a different column or LUT." );
        return null;
    }

    private ColoringModel< A > createColoringModel()
    {
        if ( lutName.contains( LUTs.GLASBEY ) )
        {
            return ColoringModels.createCategoricalModel( columnName, lutName, TRANSPARENT );
        }
        else if ( Number.class.isAssignableFrom( table.columnClass( columnName ) ) )
        {
            final Pair< Double, Double > minMax = table.getMinMax( columnName );
            return ColoringModels.createNumericModel( columnName, lutName, minMax );
        }
        else
        {
            return ColoringModels.createCategoricalModel( columnName, lutName, TRANSPARENT );
        }
    }

    private void addColoringModeSelection( JDialog dialog )
    {
        JPanel panel = SwingHelper.horizontalFlowLayoutPanel();

        replaceColoringRadioButton = new JRadioButton( "Replace current coloring" );
        addToExistingColoringRadioButton = new JRadioButton( "Add to current coloring" );
        addToExistingColoringRadioButton.setEnabled( currentColoringModel != null );

        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add( replaceColoringRadioButton );
        buttonGroup.add( addToExistingColoringRadioButton );
        replaceColoringRadioButton.setSelected( true );

        panel.add( replaceColoringRadioButton );
        panel.add( addToExistingColoringRadioButton );
        dialog.add( panel );
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
        paintZeroTransparentCheckBox = new JCheckBox( );
        paintZeroTransparentCheckBox.setSelected( paintZeroTransparent || ( lutName != null && lutName.contains( LUTs.ZERO_TRANSPARENT ) ) );
        panel.add( new JLabel("Paint zero transparent:  ") );
        panel.add( paintZeroTransparentCheckBox );
        dialog.add( panel );
    }

    private void addHighValuesTransparentCheckbox( JDialog dialog )
    {
        JPanel panel = SwingHelper.horizontalFlowLayoutPanel();
        highValuesTransparentCheckBox = new JCheckBox();
        boolean init = highValuesTransparent || ( currentColoringModel instanceof NumericAnnotationColoringModel && ((NumericAnnotationColoringModel) currentColoringModel).isHighValuesTransparent() );
        highValuesTransparentCheckBox.setSelected( init );
        panel.add( new JLabel("Make values > max transparent:  ") );
        panel.add( highValuesTransparentCheckBox );
        dialog.add( panel );
    }

    private void addColumnSelection( JDialog dialog )
    {
        JPanel panel = SwingHelper.horizontalFlowLayoutPanel();
        columnComboBox = new JComboBox<>( table.columnNames().toArray( new String[ 0 ] ) );
        Dimension maximumSize = new Dimension( 300, 20 );
        columnComboBox.setMaximumSize( maximumSize );
        panel.add( new JLabel("Column:  ") );
        panel.add( columnComboBox );
        dialog.add( panel );
    }

    private void addLutSelection( JDialog dialog )
    {
        JPanel panel = SwingHelper.horizontalFlowLayoutPanel();
        final String[] lutNames = getLutNames( getColumnName() );
        lutComboBox = new JComboBox<>( lutNames );
        final String selectedLutName = getSelectedLutName();
        lutComboBox.setSelectedItem( contains( lutNames, selectedLutName ) ? selectedLutName : lutNames[ 0 ] );
        Dimension maximumSize = new Dimension( 300, 20 );
        lutComboBox.setMaximumSize( maximumSize );
        columnComboBox.addActionListener( e -> updateLutChoices() );
        panel.add( new JLabel("Color:  ") );
        panel.add( lutComboBox );
        dialog.add( panel );
    }

    private String getColumnName()
    {
        return (String) columnComboBox.getSelectedItem();
    }

    private String getLutName()
    {
        return (String) lutComboBox.getSelectedItem();
    }

    private String getSelectedLutName()
    {
        if ( lutName == null )
            return COLORING_LUTS[ 0 ];

        if ( lutName.contains( LUTs.SINGLE_COLOR ) )
            return LUTs.SINGLE_COLOR;

        return lutName.replace( LUTs.ZERO_TRANSPARENT, "" );
    }

    private void updateLutChoices()
    {
        final String selectedLutName = getLutName();
        final String[] lutNames = getLutNames( getColumnName() );
        lutComboBox.setModel( new DefaultComboBoxModel<>( lutNames ) );
        lutComboBox.setSelectedItem( contains( lutNames, selectedLutName ) ? selectedLutName : lutNames[ 0 ] );
    }

    private String[] getLutNames( String columnName )
    {
        if ( Number.class.isAssignableFrom( table.columnClass( columnName ) ) )
            return COLORING_LUTS;

        final String[] lutNames = new String[ COLORING_LUTS.length - 1 ];
        int index = 0;
        for ( String lutName : COLORING_LUTS )
        {
            if ( lutName.equals( LUTs.SINGLE_COLOR ) )
                continue;

            lutNames[ index++ ] = lutName;
        }

        return lutNames;
    }

    private boolean contains( String[] values, String value )
    {
        for ( String currentValue : values )
            if ( currentValue.equals( value ) )
                return true;

        return false;
    }

    private boolean getPaintZeroTransparent()
    {
        return paintZeroTransparentCheckBox.isSelected();
    }

    private boolean getHighValuesTransparent()
    {
        return highValuesTransparentCheckBox.isSelected();
    }

    private boolean addToExistingColoring()
    {
        return addToExistingColoringRadioButton.isSelected();
    }

}
