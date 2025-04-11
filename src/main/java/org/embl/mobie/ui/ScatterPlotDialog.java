package org.embl.mobie.ui;

import org.embl.mobie.lib.plot.ScatterPlotSettings;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class ScatterPlotDialog
{
    private final List< String > columnNames;
    private final ScatterPlotSettings settings;
    private JComboBox< String > xColumnComboBox;
    private JComboBox< String > yColumnComboBox;

    private JTextField aspectRatio;
    private JTextField dotSize;
    private JCheckBox allTimePoints;
    private boolean isOkPressed;

    public ScatterPlotDialog( List< String > columnNames, ScatterPlotSettings settings )
    {
        this.columnNames = columnNames;
        this.settings = settings;
    }

    public boolean show()
    {
        // Show dialog
        //
        JDialog dialog = new JDialog( ( Frame ) null, "Color by Column", true );
        dialog.setLayout( new BoxLayout( dialog.getContentPane(), BoxLayout.Y_AXIS ) );

        addAllTimePointsCheckbox( dialog, settings.showAllTimepoints );

        addXColumnSelection( dialog, settings.selectedColumns[ 0 ] );

        addYColumnSelection( dialog, settings.selectedColumns[ 1 ] );

        addAspectRatioField( dialog, settings.aspectRatio );

        addDotSizeField( dialog, settings.dotSize );

        addOKCancelButton( dialog );

        //dialog.setPreferredSize( new Dimension( 250, 200 ) );
        dialog.setLocation(
                Toolkit.getDefaultToolkit().getScreenSize().width / 2 - 200,
                Toolkit.getDefaultToolkit().getScreenSize().height / 2 - 200
        );
        dialog.pack();
        dialog.setVisible( true );

        if ( isOkPressed )
        {
            settings.aspectRatio = Double.parseDouble( aspectRatio.getText() );
            settings.dotSize = Double.parseDouble( dotSize.getText() );
            settings.selectedColumns[ 0 ] = (String) xColumnComboBox.getSelectedItem();
            settings.selectedColumns[ 1 ] = (String) yColumnComboBox.getSelectedItem();
            settings.showAllTimepoints = allTimePoints.isSelected();
        }

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

    private void addAllTimePointsCheckbox( JDialog dialog, boolean showAllTimepoints )
    {
        JPanel panel = SwingHelper.horizontalFlowLayoutPanel();
        allTimePoints = new JCheckBox();
        allTimePoints.setSelected( showAllTimepoints );
        panel.add( new JLabel("Paint zero transparent:  ") );
        panel.add( allTimePoints );
        dialog.add( panel );
    }

    private void addXColumnSelection( JDialog dialog, String column )
    {
        JPanel panel = SwingHelper.horizontalFlowLayoutPanel();
        xColumnComboBox = new JComboBox<>( columnNames.toArray( new String[ 0 ] ) );
        xColumnComboBox.setSelectedItem( column );
        panel.add( new JLabel("Column:  ") );
        panel.add( xColumnComboBox );
        dialog.add( panel );
    }

    private void addYColumnSelection( JDialog dialog, String column )
    {
        JPanel panel = SwingHelper.horizontalFlowLayoutPanel();
        yColumnComboBox = new JComboBox<>( columnNames.toArray( new String[ 0 ] ) );
        yColumnComboBox.setSelectedItem( column );
        panel.add( new JLabel("Column:  ") );
        panel.add( yColumnComboBox );
        dialog.add( panel );
    }

    private void addAspectRatioField( JDialog dialog, double aspectRatio )
    {
        JPanel panel = SwingHelper.horizontalFlowLayoutPanel();
        this.aspectRatio = new JTextField( "    " + aspectRatio );
        panel.add( new JLabel("Aspect Ratio  (0 = Auto):  ") );
        panel.add( this.aspectRatio );
        dialog.add( panel );
    }

    private void addDotSizeField( JDialog dialog, double dotSize )
    {
        JPanel panel = SwingHelper.horizontalFlowLayoutPanel();
        this.dotSize = new JTextField( "     " + dotSize );
        panel.add( new JLabel("Dot Size:  ") );
        panel.add( this.dotSize );
        dialog.add( panel );
    }

    public ScatterPlotSettings getSettings()
    {
        return settings;
    }
}

