package de.embl.cba.mobie.image;

import de.embl.cba.mobie.projects.ProjectsCreator;
import de.embl.cba.mobie.ui.viewer.MoBIEViewer;
import de.embl.cba.tables.FileAndUrlUtils;
import de.embl.cba.tables.SwingUtils;
import de.embl.cba.tables.color.ColorUtils;
import de.embl.cba.tables.color.ColoringLuts;
import net.imglib2.type.numeric.ARGBType;
import org.apache.commons.compress.utils.FileNameUtils;
import org.apache.commons.lang.WordUtils;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FilenameFilter;
import java.text.Format;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.stream.Collectors;

// TODO - add some of this to swing utils?
public class ImagePropertiesEditor {
    public static final int TEXT_FIELD_HEIGHT = 20;
    public static final int COMBOBOX_WIDTH = 270;
    public static final Dimension BUTTON_DIMENSION = new Dimension( 80, TEXT_FIELD_HEIGHT );
    private String datasetName;
    private String imageName;
    private ProjectsCreator projectsCreator;
    private ImageProperties imageProperties;

    JComboBox<String> colorCombo;
    JCheckBox transparencyCheckBox;
    JTextField colorByColumnField;
    JFormattedTextField contrastLimitMinField;
    JFormattedTextField contrastLimitMaxField;
    JFormattedTextField valueLimitMinField;
    JFormattedTextField valueLimitMaxField;
    JFormattedTextField resolution3dViewField;
    JList tablesList;
    JTextField selectedLabelIdsField;
    JCheckBox showSelectedSegmentsIn3dCheckbox;
    JCheckBox showImageIn3dCheckbox;


    public ImagePropertiesEditor(String datasetName, String imageName, ProjectsCreator projectsCreator) {
        this.datasetName = datasetName;
        this.imageName = imageName;
        this.projectsCreator = projectsCreator;
        imageProperties = projectsCreator.getCurrentImages( datasetName ).get( imageName );
        editImagePropertiesDialog();
    }

    private JButton getButton(String buttonLabel )
    {
        return getButton( buttonLabel, BUTTON_DIMENSION );
    }

    private JButton getButton( String buttonLabel, Dimension dimension )
    {
        final JButton button = new JButton( buttonLabel );
        button.setPreferredSize( dimension ); // TODO
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
        return getJLabel( text, 170, 10);
    }

    private JLabel getJLabel( String text, int width, int height )
    {
        final JLabel comp = new JLabel( text );
        comp.setPreferredSize( new Dimension( width, height ) );
        comp.setHorizontalAlignment( SwingConstants.LEFT );
        comp.setHorizontalTextPosition( SwingConstants.LEFT );
        comp.setAlignmentX( Component.LEFT_ALIGNMENT );
        return comp;
    }

    private JPanel createLabelPanel ( String label ) {
        JPanel labelPanel = SwingUtils.horizontalLayoutPanel();
        JLabel jlabel = getJLabel( label, 170, 20 );
        jlabel.setBorder( BorderFactory.createEmptyBorder(10, 0, 10, 0) );
        labelPanel.add( jlabel );
        return labelPanel;
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
        return createTextPanel( label, null );
    }

    private JPanel createListPanel ( String label, JList list ) {
        final JPanel listPanel = SwingUtils.horizontalLayoutPanel();
        listPanel.add(getJLabel(label));
        JScrollPane scroller = new JScrollPane( list );
        scroller.setPreferredSize( new Dimension(250, 80));
        listPanel.add(scroller);
        return listPanel;
    }

    private JTextField createTextField ( String defaultValue ) {
        JTextField textField;
        if ( defaultValue == null ) {
            textField = new JTextField(20);
        } else {
            textField = new JTextField( defaultValue, 20);
        }

        return textField;
    }

    private JPanel createTextPanel ( String label, JTextField textField ) {
        final JPanel textPanel = SwingUtils.horizontalLayoutPanel();
        textPanel.add(getJLabel(label));
        textPanel.add(textField);

        return textPanel;
    }

    // private JPanel createNumberPanel (String label, Format format) {
    //     return createNumberPanel( label, format, "");
    // }

    private JFormattedTextField createFormattedTextField ( Format format, Object defaultValue ) {
        JFormattedTextField textField = new JFormattedTextField(format);
        if ( defaultValue != null ) {
            textField.setValue( defaultValue );
        }
        textField.setFocusLostBehavior( JFormattedTextField.COMMIT_OR_REVERT );

        return textField;
    }

    private JPanel createNumberPanel (String label, JFormattedTextField textField) {
        final JPanel textPanel = SwingUtils.horizontalLayoutPanel();
        textPanel.add(getJLabel(label));
        textPanel.add(textField);

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

    private JPanel createAcceptCancelPanel( JFrame frame ) {
        final JPanel acceptCancelPanel = SwingUtils.horizontalLayoutPanel();
        JButton acceptButton = getButton( "Update properties", new Dimension(160, 20));
        JButton cancelButton = getButton( "Cancel");

        acceptButton.addActionListener( e -> {
            // update properties
            // Make an imageproperties instance from current settings
            // Overwrite in currentImages
            // write to json
        } );

        cancelButton.addActionListener( e -> {
            frame.dispose();
        } );

        acceptCancelPanel.add(acceptButton);
        acceptCancelPanel.add(cancelButton);

        return acceptCancelPanel;
    }

    private JPanel createColorPanel () {
        JPanel colorPanel;

        if ( imageProperties.type.equals("segmentation") ) {
            String[] colorOptions = new String[] { ColoringLuts.GLASBEY, ColoringLuts.BLUE_WHITE_RED, ColoringLuts.VIRIDIS,
                    ColoringLuts.ARGB_COLUMN };
            colorCombo = new JComboBox<>( colorOptions );
            colorCombo.setSelectedItem(WordUtils.capitalize( imageProperties.color ) );
            colorPanel = createComboPanel( colorCombo, "color");
            // TODO - deal with zero transparency
        } else {
            ArrayList<String> colorOptions = new ArrayList<> ();
            colorOptions.add("white");
            colorOptions.add("randomFromGlasbey");
            String currentColor = imageProperties.color;

            if ( !colorOptions.contains( currentColor ) ) {
                colorOptions.add(currentColor);
            }

            String[] colorArray = new String[ colorOptions.size() ];
            colorOptions.toArray( colorArray );

            colorCombo = new JComboBox<>( colorArray );
            colorCombo.setSelectedItem( currentColor );
            JButton otherColorButton = createColorButton( colorCombo );

            colorPanel = createComboPanel( colorCombo, "color", otherColorButton );
        }

        return colorPanel;
    }

    private JPanel createTablesPanel() {
        JPanel tables = null;
        //TODO - remove default table, prehaps only show if there is more than one table here
        if ( imageProperties.type.equals("segmentation") ) {
            File tableFolder = new File(FileAndUrlUtils.combinePath(projectsCreator.getDatasetPath(datasetName), imageProperties.tableFolder) );
            File[] tableFiles = tableFolder.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(".csv") || name.toLowerCase().endsWith(".tsv");
                }
            });

            String[] tableNames = new String[tableFiles.length];
            for (int i = 0; i< tableFiles.length; i++) {
                tableNames[i] = FileNameUtils.getBaseName( tableFiles[i].getAbsolutePath() );
            }
            tablesList = new JList( tableNames );
            tablesList.setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
            tablesList.setLayoutOrientation( JList.VERTICAL );
            if ( tableNames.length < 3) {
                tablesList.setVisibleRowCount( tableNames.length );
            } else {
                tablesList.setVisibleRowCount(3);
            }

            ArrayList<String> selectedTables = imageProperties.tables;
            if ( selectedTables != null ) {
                if (selectedTables.size() > 0) {
                    ArrayList<Integer> selectedIndices = new ArrayList<>();
                    for (int i = 0; i < tablesList.getModel().getSize(); i++) {
                        if (selectedTables.contains(tablesList.getModel().getElementAt(i))) {
                            selectedIndices.add(i);
                        }
                    }
                    int[] selectedIndicesArray = selectedIndices.stream().mapToInt(i -> i).toArray();
                    tablesList.setSelectedIndices(selectedIndicesArray);
                }
            }

            tables = createListPanel( "tables", tablesList);
        }

        return tables;
    }




    public void editImagePropertiesDialog() {
        // TODO - only show ones relevant for that image type
        JFrame editImageFrame = new JFrame("Edit image properties...");
        editImageFrame.getContentPane().setLayout( new BoxLayout(editImageFrame.getContentPane(), BoxLayout.Y_AXIS ) );
        editImageFrame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );

        JPanel colorComboPanel = createColorPanel();

        transparencyCheckBox = new JCheckBox("Paint Zero Transparent");
        transparencyCheckBox.setSelected( false );

        colorByColumnField = createTextField( imageProperties.colorByColumn );
        final JPanel colorByColumnPanel = createTextPanel( "colorByColumn", colorByColumnField );

        // Format to only accept numbers
        NumberFormat amountFormat = NumberFormat.getNumberInstance();
        amountFormat.setMaximumFractionDigits(5);
        amountFormat.setGroupingUsed( false );

        double[] currentContrastLimits = imageProperties.contrastLimits;
        if ( currentContrastLimits != null ) {
            contrastLimitMinField = createFormattedTextField( amountFormat, currentContrastLimits[0]);
            contrastLimitMaxField = createFormattedTextField( amountFormat, currentContrastLimits[1] );
        } else {
            contrastLimitMinField = createFormattedTextField( amountFormat, null);
            contrastLimitMaxField = createFormattedTextField( amountFormat, null);
        }
        JPanel contrastLimitMinPanel = createNumberPanel( "contrast limit min", contrastLimitMinField );
        JPanel contrastLimitMaxPanel = createNumberPanel( "contrast limit max", contrastLimitMaxField );

        double[] currentValueLimits = imageProperties.valueLimits;
        if ( currentValueLimits != null ) {
            valueLimitMinField = createFormattedTextField( amountFormat, currentValueLimits[0] );
            valueLimitMaxField = createFormattedTextField( amountFormat, currentValueLimits[1] );
        } else {
            valueLimitMinField = createFormattedTextField( amountFormat, null );
            valueLimitMaxField = createFormattedTextField( amountFormat, null );
        }
        JPanel valueLimitMinPanel = createNumberPanel( "value limit min", valueLimitMinField );
        JPanel valueLimitMaxPanel = createNumberPanel( "value limit max", valueLimitMaxField );

        resolution3dViewField = createFormattedTextField( amountFormat, imageProperties.resolution3dView );
        final JPanel resolution3dViewPanel = createNumberPanel( "resolution 3d view", resolution3dViewField );

        JPanel tablesPanel = createTablesPanel();

        // TODO - how to restrict to a list of comma separated values??????
        // FOr now, strip any spaces, check on other chracters apart form numbers and commas. If there are - error.
        // Otherwise, parse
        ArrayList<Double> currentSelectedLabelIds = imageProperties.selectedLabelIds;
        String currentIdsString = currentSelectedLabelIds.stream().map(Object::toString).collect(Collectors.joining(","));
        selectedLabelIdsField = createTextField( currentIdsString );
        JPanel selectedLabelIDsPanel = createTextPanel( "selected label ids", selectedLabelIdsField);


        showSelectedSegmentsIn3dCheckbox = new JCheckBox("Show selected segments in 3d");
        showSelectedSegmentsIn3dCheckbox.setSelected( imageProperties.showSelectedSegmentsIn3d );

        showImageIn3dCheckbox = new JCheckBox("Show image in 3d");
        showImageIn3dCheckbox.setSelected( imageProperties.showImageIn3d );

        JPanel editPropertiesPanel = new JPanel();
        editPropertiesPanel.setLayout( new BoxLayout(editPropertiesPanel, BoxLayout.PAGE_AXIS) );
        editPropertiesPanel.add(colorComboPanel);
        if ( imageProperties.type.equals("segmentation")) {
            editPropertiesPanel.add(transparencyCheckBox);
            editPropertiesPanel.add(colorByColumnPanel);
        }
        editPropertiesPanel.add(contrastLimitMinPanel);
        editPropertiesPanel.add(contrastLimitMaxPanel);
        if ( imageProperties.type.equals("segmentation")) {
            editPropertiesPanel.add(valueLimitMinPanel);
            editPropertiesPanel.add(valueLimitMaxPanel);
            editPropertiesPanel.add(tablesPanel);
            editPropertiesPanel.add(createLabelPanel( "List label ids e.g. 12,35,95,100"));
            editPropertiesPanel.add(selectedLabelIDsPanel);
        }
        editPropertiesPanel.add(resolution3dViewPanel);
        if ( imageProperties.type.equals("segmentation")) {
            editPropertiesPanel.add(showSelectedSegmentsIn3dCheckbox);
        }
        editPropertiesPanel.add(showImageIn3dCheckbox);
        editPropertiesPanel.add( createAcceptCancelPanel( editImageFrame ));
        editImageFrame.add(editPropertiesPanel);

        editImageFrame.pack();
        editImageFrame.setVisible( true );

        // TODO - checkbox to make default bookmark

    }
}
