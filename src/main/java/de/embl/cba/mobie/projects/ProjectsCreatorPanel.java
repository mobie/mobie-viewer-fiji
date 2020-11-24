package de.embl.cba.mobie.projects;

import bdv.tools.brightness.ConverterSetup;
import bdv.tools.brightness.SliderPanelDouble;
import bdv.util.BoundedValueDouble;
import de.embl.cba.bdv.utils.BrightnessUpdateListener;
import de.embl.cba.mobie.dataset.DatasetsParser;
import de.embl.cba.mobie.ui.viewer.MoBIEViewer;
import de.embl.cba.tables.SwingUtils;
import ij.Prefs;
import ij.gui.GenericDialog;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

public class ProjectsCreatorPanel extends JFrame {
    public static final int TEXT_FIELD_HEIGHT = 20;
    public static final int COMBOBOX_WIDTH = 270;
    public static final Dimension BUTTON_DIMENSION = new Dimension( 80, TEXT_FIELD_HEIGHT );
    private ProjectsCreator projectsCreator;

    public ProjectsCreatorPanel ( ProjectsCreator projectsCreator ) {
        this.projectsCreator = projectsCreator;
        addDatasetPanel();
        this.pack();
        this.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
        this.setVisible( true );
    }

    private void addDatasetPanel() {
        final JPanel horizontalLayoutPanel = SwingUtils.horizontalLayoutPanel();

        final JButton button = getButton("Add");

        final JComboBox<String> comboBox = new JComboBox<>( projectsCreator.getCurrentDatasets() );
        setComboBoxDimensions(comboBox);
        button.addActionListener(e -> addDatasetDialog());
        comboBox.setPrototypeDisplayValue(MoBIEViewer.PROTOTYPE_DISPLAY_VALUE);

        horizontalLayoutPanel.add(getJLabel("dataset"));
        horizontalLayoutPanel.add(comboBox);
        horizontalLayoutPanel.add(button);

        this.getContentPane().add(horizontalLayoutPanel);
    }

    private void addImagesPanel() {

    }

    private void addDatasetDialog () {
        final GenericDialog gd = new GenericDialog( "Create a new dataset" );

        gd.addStringField( "Name of dataset", "");
        gd.showDialog();

        if ( !gd.wasCanceled() ) {
            String datasetName = gd.getNextString();
            // check not already in datasets
            boolean contains = Arrays.stream( projectsCreator.getCurrentDatasets() ).anyMatch(datasetName::equals);
            if ( !contains ) {
                projectsCreator.addDataset( datasetName );
            }

        }
    }

    private JButton getButton( String buttonLabel )
    {
        final JButton button = new JButton( buttonLabel );
        button.setPreferredSize( BUTTON_DIMENSION ); // TODO
        return button;
    }

    private void setComboBoxDimensions( JComboBox< String > comboBox )
    {
        comboBox.setPrototypeDisplayValue( MoBIEViewer.PROTOTYPE_DISPLAY_VALUE );
        comboBox.setPreferredSize( new Dimension( COMBOBOX_WIDTH, 20 ) );
        comboBox.setMaximumSize( new Dimension( COMBOBOX_WIDTH, 20 ) );
    }

    private JLabel getJLabel( String text )
    {
        final JLabel comp = new JLabel( text );
        comp.setPreferredSize( new Dimension( 170,10 ) );
        comp.setHorizontalAlignment( SwingConstants.LEFT );
        comp.setHorizontalTextPosition( SwingConstants.LEFT );
        comp.setAlignmentX( Component.LEFT_ALIGNMENT );
        return comp;
    }
}
