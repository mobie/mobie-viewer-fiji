package de.embl.cba.mobie.projects;

import bdv.tools.brightness.ConverterSetup;
import bdv.tools.brightness.SliderPanelDouble;
import bdv.util.BoundedValueDouble;
import de.embl.cba.bdv.utils.BrightnessUpdateListener;
import de.embl.cba.mobie.dataset.DatasetsParser;
import de.embl.cba.mobie.ui.viewer.MoBIEViewer;
import de.embl.cba.tables.FileAndUrlUtils;
import de.embl.cba.tables.FileUtils;
import de.embl.cba.tables.SwingUtils;
import ij.Prefs;
import ij.gui.GenericDialog;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.util.Arrays;

public class ProjectsCreatorPanel extends JFrame {
    public static final int TEXT_FIELD_HEIGHT = 20;
    public static final int COMBOBOX_WIDTH = 270;
    public static final Dimension BUTTON_DIMENSION = new Dimension( 80, TEXT_FIELD_HEIGHT );
    private ProjectsCreator projectsCreator;
    private JComboBox<String> datasetComboBox;
    private JComboBox<String> imagesComboBox;

    public ProjectsCreatorPanel ( File projectLocation ) {
        this.projectsCreator = new ProjectsCreator( projectLocation );
        addDatasetPanel();
        addImagesPanel();
        this.getContentPane().setLayout( new BoxLayout(this.getContentPane(), BoxLayout.Y_AXIS ) );
        this.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
    }

    public void showProjectsCreatorPanel() {
        this.pack();
        this.setVisible( true );
    }

    public ProjectsCreator getProjectsCreator() {
        return projectsCreator;
    }

    private void addDatasetPanel() {
        final JPanel horizontalLayoutPanel = SwingUtils.horizontalLayoutPanel();

        final JButton addButton = getButton("Add");

        createDatasetComboBox();
        addButton.addActionListener(e -> addDatasetDialog());

        horizontalLayoutPanel.add(getJLabel("dataset"));
        horizontalLayoutPanel.add(datasetComboBox);
        horizontalLayoutPanel.add(addButton);

        this.getContentPane().add(horizontalLayoutPanel);
    }

    private void createDatasetComboBox() {
        datasetComboBox = new JComboBox<>( projectsCreator.getCurrentDatasets() );
        setComboBoxDimensions(datasetComboBox);
        datasetComboBox.setPrototypeDisplayValue(MoBIEViewer.PROTOTYPE_DISPLAY_VALUE);
    }

    private void addImagesPanel() {
        final JPanel horizontalLayoutPanel = SwingUtils.horizontalLayoutPanel();

        final JButton addButton = getButton("Add");

        imagesComboBox = new JComboBox<>( projectsCreator.getCurrentDatasets() );
        setComboBoxDimensions(imagesComboBox);
        addButton.addActionListener(e -> addImageDialog());
        imagesComboBox.setPrototypeDisplayValue(MoBIEViewer.PROTOTYPE_DISPLAY_VALUE);

        horizontalLayoutPanel.add(getJLabel("image"));
        horizontalLayoutPanel.add(imagesComboBox);
        horizontalLayoutPanel.add(addButton);

        this.getContentPane().add(horizontalLayoutPanel);
    }

    public String chooseDatasetDialog() {
        final GenericDialog gd = new GenericDialog( "Choose a dataset" );
        String[] currentDatasets = projectsCreator.getCurrentDatasets();
        gd.addChoice("Dataset", currentDatasets, currentDatasets[0]);
        gd.showDialog();

        if ( !gd.wasCanceled() ) {
            return gd.getNextChoice();

        } else {
            return null;
        }


    }

    public String addDatasetDialog () {
        final GenericDialog gd = new GenericDialog( "Create a new dataset" );

        gd.addStringField( "Name of dataset", "");
        gd.showDialog();

        if ( !gd.wasCanceled() ) {
            String datasetName = gd.getNextString();
            // check not already in datasets
            boolean contains = Arrays.stream( projectsCreator.getCurrentDatasets() ).anyMatch(datasetName::equals);
            if ( !contains ) {
                projectsCreator.addDataset( datasetName );
                updateDatasetsComboBox();
            }

            return datasetName;

        } else {
            return null;
        }
    }

    private void addImageDialog () {

        final JFileChooser jFileChooser = new JFileChooser();
        if (jFileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            String imagePath = jFileChooser.getSelectedFile().getAbsolutePath();

            final GenericDialog gd = new GenericDialog("Add a new image");
            gd.addMessage("Adding image to dataset: " + datasetComboBox.getSelectedItem());
            gd.addStringField("Name of image", "");
            gd.addChoice("Bdv format", new String[]{"n5", "h5"}, "n5");
            gd.addStringField("Pixel size unit", "micrometer");
            gd.addNumericField("x pixel size", 1);
            gd.addNumericField("y pixel size", 1);
            gd.addNumericField("z pixel size", 1);
            gd.showDialog();

            if (!gd.wasCanceled()) {
                String imageName = gd.getNextString();
                String datasetName = (String) datasetComboBox.getSelectedItem();
                String bdvFormat = gd.getNextChoice();
                String pixelSizeUnit = gd.getNextString();
                double xPixelSize = gd.getNextNumber();
                double yPixelSize = gd.getNextNumber();
                double zPixelSize = gd.getNextNumber();
                projectsCreator.addImage( imagePath, imageName, datasetName, bdvFormat, pixelSizeUnit,
                        xPixelSize, yPixelSize, zPixelSize);

            }
        }
    }

    private void updateDatasetsComboBox () {
        if ( datasetComboBox != null ) {
            datasetComboBox.removeAllItems();
            for (String datasetName : projectsCreator.getCurrentDatasets()) {
                datasetComboBox.addItem(datasetName);
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
