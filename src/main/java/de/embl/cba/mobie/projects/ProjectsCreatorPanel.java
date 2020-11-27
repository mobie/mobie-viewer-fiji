package de.embl.cba.mobie.projects;

import bdv.tools.brightness.ConverterSetup;
import bdv.tools.brightness.SliderPanelDouble;
import bdv.util.BoundedValueDouble;
import de.embl.cba.bdv.utils.BrightnessUpdateListener;
import de.embl.cba.mobie.dataset.DatasetsParser;
import de.embl.cba.mobie.image.ImageProperties;
import de.embl.cba.mobie.image.MutableImageProperties;
import de.embl.cba.mobie.ui.command.OpenMoBIEPublishedProjectCommand;
import de.embl.cba.mobie.ui.viewer.MoBIEViewer;
import de.embl.cba.tables.FileAndUrlUtils;
import de.embl.cba.tables.FileUtils;
import de.embl.cba.tables.SwingUtils;
import de.embl.cba.tables.color.ColorUtils;
import de.embl.cba.tables.color.ColoringLuts;
import de.embl.cba.tables.image.SourceAndMetadata;
import ij.Prefs;
import ij.gui.GenericDialog;
import net.imagej.ImageJ;
import net.imglib2.type.numeric.ARGBType;
import org.apache.commons.compress.utils.FileNameUtils;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.MaskFormatter;
import java.awt.*;
import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Field;
import java.text.Format;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;

// TODO - move some of this to swingutils

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

    private JPanel createComboPanel (JComboBox<String> combo, String label) {
        final JPanel comboPanel = SwingUtils.horizontalLayoutPanel();
        setComboBoxDimensions( combo );
        combo.setPrototypeDisplayValue(MoBIEViewer.PROTOTYPE_DISPLAY_VALUE);
        comboPanel.add(getJLabel(label));
        comboPanel.add(combo);

        return comboPanel;
    }

    private JPanel createComboPanel (JComboBox<String> combo, String label, JButton button) {
        JPanel comboPanel = createComboPanel( combo, label );
        comboPanel.add( button );

        return comboPanel;
    }

    private JPanel createTextPanel ( String label ) {
        return createTextPanel( label, null);
    }

    private JPanel createListPanel ( String label, JList list ) {
        final JPanel listPanel = SwingUtils.horizontalLayoutPanel();
        listPanel.add(getJLabel(label));
        JScrollPane scroller = new JScrollPane( list );
        scroller.setPreferredSize( new Dimension(250, 80));
        listPanel.add(scroller);
        return listPanel;
    }

    private JPanel createTextPanel ( String label, Format format) {
        final JPanel textPanel = SwingUtils.horizontalLayoutPanel();
        textPanel.add(getJLabel(label));

        if (format == null) {
            JTextField textField = new JTextField(20);
            textPanel.add(textField);
        } else {
            JFormattedTextField textField = new JFormattedTextField( format );
            textField.setFocusLostBehavior( JFormattedTextField.COMMIT_OR_REVERT );
            textPanel.add(textField);
        }

        return textPanel;
    }

    // TODO - there must be a way to collapse this with the one above
    private JPanel createTextPanel ( String label, MaskFormatter format, boolean aah) {
        final JPanel textPanel = SwingUtils.horizontalLayoutPanel();
        textPanel.add(getJLabel(label));

        if (format == null) {
            JTextField textField = new JTextField(20);
            textPanel.add(textField);
        } else {
            JFormattedTextField textField = new JFormattedTextField( format );
            textField.setFocusLostBehavior( JFormattedTextField.COMMIT_OR_REVERT );
            textPanel.add(textField);
        }

        return textPanel;
    }

    private JButton createColorButton( JComboBox<String> colorCombo )
    {
        JButton colorButton;
        colorButton = new JButton( "other color" );

        colorButton.addActionListener( e -> {
            Color color = JColorChooser.showDialog( null, "", null );

            if ( color == null ) return;

            ARGBType colorARGB = ColorUtils.getARGBType( color );
            colorCombo.addItem( colorARGB.toString() );
            colorCombo.setSelectedItem( colorARGB.toString() );

        } );

        return colorButton;
    }



    public void editImagePropertiesDialog( String datasetName, String imageName ) {
        // TODO - only show ones relevant for that image type
        ImageProperties imageProperties = projectsCreator.getCurrentImages( datasetName ).get( imageName );

        JFrame editImageFrame = new JFrame();
        editImageFrame.getContentPane().setLayout( new BoxLayout(editImageFrame.getContentPane(), BoxLayout.Y_AXIS ) );
        editImageFrame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );

        JPanel colorComboPanel;
        if ( imageProperties.type.equals("segmentation") ) {
            String[] colorOptions = new String[] { ColoringLuts.GLASBEY, ColoringLuts.BLUE_WHITE_RED, ColoringLuts.VIRIDIS,
                    ColoringLuts.ARGB_COLUMN };
            colorComboPanel = createComboPanel( new JComboBox<>( colorOptions ), "color");
            // TODO - deal with zero transparency
        } else {
            String[] colorOptions = new String[] {"white", "randomFromGlasbey"};
            JComboBox<String> colorCombo = new JComboBox<>( colorOptions );
            JButton otherColorButton = createColorButton( colorCombo );
            colorComboPanel = createComboPanel( colorCombo, "color", otherColorButton );
        }



        JCheckBox transparent = new JCheckBox("Paint Zero Transparent");
        transparent.setSelected( false );

        final JPanel colorByColumnPanel = createTextPanel( "colorByColumn" );

        NumberFormat amountFormat = NumberFormat.getNumberInstance();
        amountFormat.setMaximumFractionDigits(5);
        final JPanel contrastLimitMin = createTextPanel( "contrast limit min", amountFormat);
        final JPanel contrastLimitMax = createTextPanel( "contrast limit max", amountFormat);
        final JPanel valueLimitMin = createTextPanel( "value limit min", amountFormat);
        final JPanel valueLimitMax = createTextPanel( "value limit max", amountFormat);
        final JPanel resolution3dView = createTextPanel( "resolution 3d view", amountFormat);

        JPanel tables = null;
        //TODO - remove default table, prehaps only show if there is more than one table here
        if ( imageProperties.type.equals("segmentation") ) {
            File tableFolder = new File(FileAndUrlUtils.combinePath(projectsCreator.getDatasetPath(datasetName), imageProperties.tableFolder) );
            System.out.println(tableFolder);
            File[] tableFiles = tableFolder.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(".csv") || name.toLowerCase().endsWith(".tsv");
                }
            });

            String[] tableNames = new String[tableFiles.length];
            for (int i = 0; i< tableFiles.length; i++) {
                tableNames[i] = FileNameUtils.getBaseName( tableFiles[i].getAbsolutePath() );
            }
            JList tableList = new JList( tableNames );
            tableList.setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
            tableList.setLayoutOrientation( JList.VERTICAL );
            if ( tableNames.length < 3) {
                tableList.setVisibleRowCount( tableNames.length );
            } else {
                tableList.setVisibleRowCount(3);
            }

            tables = createListPanel( "tables", tableList);
        }

        // TODO - how to restrict to a list of comma separated values??????
        JPanel selectedLabelIDs = createTextPanel( "selected label ids", amountFormat);


        JCheckBox showSelectedSegmentsIn3d = new JCheckBox("Show selected segments in 3d");
        transparent.setSelected( false );

        JCheckBox showImageIn3d = new JCheckBox("Show image in 3d");
        transparent.setSelected( false );

        // need numeric entries for contrast limits and valuelimits
        // public String colorByColumn;
        // public double[] contrastLimits;
        // public double[] valueLimits;
        // public double resolution3dView;
        // public ArrayList< String > tables;
        // public ArrayList< Double > selectedLabelIds;
        // public boolean showSelectedSegmentsIn3d;
        // public boolean showImageIn3d;

        JPanel editPropertiesPanel = new JPanel();
        editPropertiesPanel.setLayout( new BoxLayout(editPropertiesPanel, BoxLayout.Y_AXIS) );
        editPropertiesPanel.add(colorComboPanel);
        if ( imageProperties.type.equals("segmentation")) {
            editPropertiesPanel.add(transparent);
            editPropertiesPanel.add(colorByColumnPanel);
        }
        editPropertiesPanel.add(contrastLimitMin);
        editPropertiesPanel.add(contrastLimitMax);
        if ( imageProperties.type.equals("segmentation")) {
            editPropertiesPanel.add(valueLimitMin);
            editPropertiesPanel.add(valueLimitMax);
            editPropertiesPanel.add(tables);
            editPropertiesPanel.add(selectedLabelIDs);
        }
        editPropertiesPanel.add(resolution3dView);
        if ( imageProperties.type.equals("segmentation")) {
            editPropertiesPanel.add(showSelectedSegmentsIn3d);
        }
        editPropertiesPanel.add(showImageIn3d);
        editImageFrame.add(editPropertiesPanel);

        editImageFrame.pack();
        editImageFrame.setVisible( true );

        // TODO - checkbox to make default bookmark

    }
}
