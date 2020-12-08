package de.embl.cba.mobie.image;

import de.embl.cba.mobie.projects.ProjectsCreator;
import de.embl.cba.mobie.ui.viewer.MoBIEViewer;
import de.embl.cba.mobie.utils.Utils;
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
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static de.embl.cba.mobie.utils.ui.SwingUtils.*;

// TODO - add option to rename images? (more difficult as need to edit path inside the xml too)
public class ImagePropertiesEditor {
    private String datasetName;
    private String imageName;
    private ProjectsCreator projectsCreator;
    private ImageProperties imageProperties;
    private NumberFormat amountFormat;

    private JComboBox<String> colorCombo;
    private JCheckBox transparencyCheckBox;
    private JTextField colorByColumnField;
    private JFormattedTextField contrastLimitMinField;
    private JFormattedTextField contrastLimitMaxField;
    private JFormattedTextField valueLimitMinField;
    private JFormattedTextField valueLimitMaxField;
    private JFormattedTextField resolution3dViewField;
    private JList tablesList;
    private JTextField selectedLabelIdsField;
    private JCheckBox showSelectedSegmentsIn3dCheckbox;
    private JCheckBox showImageIn3dCheckbox;
    private JCheckBox showByDefaultCheckbox;
    private boolean zeroTransparent;


    public ImagePropertiesEditor(String datasetName, String imageName, ProjectsCreator projectsCreator) {
        this.datasetName = datasetName;
        this.imageName = imageName;
        this.projectsCreator = projectsCreator;
        imageProperties = projectsCreator.getImageProperties( datasetName, imageName );

        // Format to only accept numbers in text fields
        amountFormat = NumberFormat.getNumberInstance();
        amountFormat.setMaximumFractionDigits(5);
        amountFormat.setGroupingUsed( false );

        zeroTransparent = false;
        editImagePropertiesDialog();
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
            try {
                updateImageProperties();
                projectsCreator.writeImagesJson( this.datasetName );
                frame.dispose();
            } catch (ParseException parseException) {
                parseException.printStackTrace();
            }
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
            String capitalisedColor = WordUtils.capitalize( imageProperties.color );

            // deal with any 'zero transparent' options
            if ( capitalisedColor.contains( ColoringLuts.ZERO_TRANSPARENT ) ) {
                capitalisedColor = capitalisedColor.split( ColoringLuts.ZERO_TRANSPARENT )[0];
                zeroTransparent = true;
            }

            colorCombo.setSelectedItem( capitalisedColor );
            colorPanel = createComboPanel( colorCombo, "color");

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

    private String[] getTableNames() {
        File tableFolder = new File(FileAndUrlUtils.combinePath(projectsCreator.getDatasetPath(datasetName), imageProperties.tableFolder) );
        File[] tableFiles = tableFolder.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".csv") || name.toLowerCase().endsWith(".tsv");
            }
        });

        if ( !(tableFiles.length > 0) ) {
            return null;
        }

        // we don't include the default table here, as it is always shown
        ArrayList<String> tableNames = new ArrayList<>();
        for (int i = 0; i< tableFiles.length; i++) {
            String tableName = FileNameUtils.getBaseName( tableFiles[i].getAbsolutePath() );
            if (!tableName.equals("default")) {
                tableNames.add( tableName );
            }
        }

        if ( !(tableNames.size() > 0) ) {
            return null;
        }

        String[] tableNamesArray = new String[tableNames.size()];
        tableNamesArray = tableNames.toArray( tableNamesArray );

        return tableNamesArray;
    }

    private void makeTablesList ( String[] tableNamesArray ) {
        tablesList = new JList(tableNamesArray);
        tablesList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        tablesList.setLayoutOrientation(JList.VERTICAL);
        if ( tableNamesArray.length < 3) {
            tablesList.setVisibleRowCount(tableNamesArray.length);
        } else {
            tablesList.setVisibleRowCount(3);
        }

        ArrayList<String> selectedTables = imageProperties.tables;
        if (selectedTables != null) {
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
    }

    private JPanel createTablesPanel() {
        JPanel tables = null;
        if ( imageProperties.type.equals("segmentation") ) {

            String[] tableNamesArray = getTableNames();

            if ( tableNamesArray != null) {
                makeTablesList( tableNamesArray );
                tables = createListPanel("tables", tablesList);
            }
        }

        return tables;
    }

    public void editImagePropertiesDialog() {
        JFrame editImageFrame = new JFrame("Edit image properties...");
        editImageFrame.getContentPane().setLayout( new BoxLayout(editImageFrame.getContentPane(), BoxLayout.Y_AXIS ) );
        editImageFrame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );

        JPanel colorComboPanel = createColorPanel();

        transparencyCheckBox = new JCheckBox("Paint Zero Transparent");
        transparencyCheckBox.setSelected( zeroTransparent );

        colorByColumnField = createTextField( imageProperties.colorByColumn );
        final JPanel colorByColumnPanel = createTextPanel( "colorByColumn", colorByColumnField );

        double[] currentContrastLimits = imageProperties.contrastLimits;
        if ( currentContrastLimits != null ) {
            contrastLimitMinField = createFormattedTextField( amountFormat, currentContrastLimits[0]);
            contrastLimitMaxField = createFormattedTextField( amountFormat, currentContrastLimits[1] );
        } else {
            // if no contrast limits, suggest reasonable defaults
            contrastLimitMinField = createFormattedTextField( amountFormat, 0.0);
            contrastLimitMaxField = createFormattedTextField( amountFormat, 255.0);
        }
        JPanel contrastLimitMinPanel = createNumberPanel( "contrast limit min", contrastLimitMinField );
        JPanel contrastLimitMaxPanel = createNumberPanel( "contrast limit max", contrastLimitMaxField );

        double[] currentValueLimits = imageProperties.valueLimits;
        if ( currentValueLimits != null ) {
            valueLimitMinField = createFormattedTextField( amountFormat, currentValueLimits[0] );
            valueLimitMaxField = createFormattedTextField( amountFormat, currentValueLimits[1] );
        } else {
            valueLimitMinField = createFormattedTextField( amountFormat, 0.0 );
            valueLimitMaxField = createFormattedTextField( amountFormat, 0.0 );
        }
        JPanel valueLimitMinPanel = createNumberPanel( "value limit min", valueLimitMinField );
        JPanel valueLimitMaxPanel = createNumberPanel( "value limit max", valueLimitMaxField );

        resolution3dViewField = createFormattedTextField( amountFormat, imageProperties.resolution3dView );
        final JPanel resolution3dViewPanel = createNumberPanel( "resolution 3d view", resolution3dViewField );

        JPanel tablesPanel = createTablesPanel();

        if ( imageProperties.selectedLabelIds != null ) {
            ArrayList<Double> currentSelectedLabelIds = imageProperties.selectedLabelIds;
            String currentIdsString = currentSelectedLabelIds.stream().map(Object::toString).collect(Collectors.joining(","));
            selectedLabelIdsField = createTextField(currentIdsString);
        } else {
            selectedLabelIdsField = createTextField( "" );
        }
        JPanel selectedLabelIDsPanel = createTextPanel("selected label ids", selectedLabelIdsField);


        showSelectedSegmentsIn3dCheckbox = new JCheckBox("Show selected segments in 3d");
        showSelectedSegmentsIn3dCheckbox.setSelected( imageProperties.showSelectedSegmentsIn3d );

        showImageIn3dCheckbox = new JCheckBox("Show image in 3d");
        showImageIn3dCheckbox.setSelected( imageProperties.showImageIn3d );

        showByDefaultCheckbox = new JCheckBox( "Show by default" );
        showByDefaultCheckbox.setSelected( projectsCreator.isInDefaultBookmark( imageName, datasetName ) );

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
            if ( tablesPanel != null ) {
                editPropertiesPanel.add(tablesPanel);
            }
            editPropertiesPanel.add(createLabelPanel( "List label ids e.g. 12,35,95,100"));
            editPropertiesPanel.add(selectedLabelIDsPanel);
        }
        editPropertiesPanel.add(resolution3dViewPanel);
        if ( imageProperties.type.equals("segmentation")) {
            editPropertiesPanel.add(showSelectedSegmentsIn3dCheckbox);
        }
        editPropertiesPanel.add(showImageIn3dCheckbox);
        editPropertiesPanel.add(showByDefaultCheckbox);
        editPropertiesPanel.add( createAcceptCancelPanel( editImageFrame ));
        editImageFrame.add(editPropertiesPanel);

        editImageFrame.pack();
        editImageFrame.setLocationRelativeTo(null);
        editImageFrame.setVisible( true );

    }

    private void commitAllEdits() {
        // ensure all edited values are commited in text fields, otherwise can have unexpected behaviour if
        // user types and immediately clicks the accept button
        try {
            contrastLimitMinField.commitEdit();
            contrastLimitMaxField.commitEdit();
            valueLimitMinField.commitEdit();
            valueLimitMaxField.commitEdit();
            resolution3dViewField.commitEdit();
        } catch (ParseException e) {
            e.printStackTrace();
        }

    }

    private boolean updateDefaultBookmarkSettingsDialog() {
        int result = JOptionPane.showConfirmDialog(null,
                "Update the default bookmark settings for this image?", "Update default bookmark too?",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (result == JOptionPane.YES_OPTION) {
            return true;
        } else {
            return false;
        }
    }

    private void updateSelectecLabelIds () {
        if ( !selectedLabelIdsField.getText().equals("") ) {
            String selectedLabelIdsText = selectedLabelIdsField.getText().trim();
            String selectedLabelIdsTextNoSpaces = selectedLabelIdsText.replaceAll("\\s+","");

            // check contains nothing but numbers or . or ,
            if ( Pattern.matches( "[0-9 | , | .]+", selectedLabelIdsTextNoSpaces )) {
                String[] ids = selectedLabelIdsTextNoSpaces.split(",");
                ArrayList<Double> selectedIds = new ArrayList<>();
                for ( String id : ids ) {
                    selectedIds.add( Double.valueOf( id ) );
                }
                imageProperties.selectedLabelIds = selectedIds;
            }
        } else {
            imageProperties.selectedLabelIds = null;
        }
    }

    private void updateDefaultBookmarkSettings () {
        boolean imageIsInDefaultBookmark = projectsCreator.isInDefaultBookmark( imageName, datasetName);
        // if modifying an image that is shown in default bookmark, give option to update that metadata
        if ( showByDefaultCheckbox.isSelected() && imageIsInDefaultBookmark ) {
            if ( updateDefaultBookmarkSettingsDialog() ) {
                projectsCreator.setImagePropertiesInDefaultBookmark( imageName, datasetName, imageProperties);
                projectsCreator.writeDefaultBookmarksJson( datasetName );
            }
        }

        // if show by default has changed, modify the bookmarks json
        if ( !(showByDefaultCheckbox.isSelected() == imageIsInDefaultBookmark) ) {
            if ( showByDefaultCheckbox.isSelected() ) {
                projectsCreator.addImageToDefaultBookmark(imageName, datasetName);
                projectsCreator.writeDefaultBookmarksJson(datasetName);
            } else {
                if ( projectsCreator.getCurrentImagesInDefaultBookmark( datasetName ).size() > 1 ) {
                    projectsCreator.removeImageFromDefaultBookmark( imageName, datasetName );
                    projectsCreator.writeDefaultBookmarksJson( datasetName );
                } else {
                    Utils.log( "can't make image non-default - you need at least one default image" );
                }
            }
        }
    }

    public void updateImageProperties () throws ParseException {

        commitAllEdits();

        String selectedColor = (String) colorCombo.getSelectedItem();
        if ( transparencyCheckBox.isSelected() ) {
            selectedColor = selectedColor + ColoringLuts.ZERO_TRANSPARENT;
        }
        imageProperties.color = selectedColor;

        double contrastLimitMin = amountFormat.parse( contrastLimitMinField.getText() ).doubleValue();
        double contrastLimitMax = amountFormat.parse( contrastLimitMaxField.getText() ).doubleValue();
        imageProperties.contrastLimits = new double[] { contrastLimitMin, contrastLimitMax };

        imageProperties.resolution3dView = amountFormat.parse( resolution3dViewField.getText() ).doubleValue();
        imageProperties.showImageIn3d = showImageIn3dCheckbox.isSelected();

        if ( imageProperties.type.equals("segmentation") ) {
            String colorByColumn = colorByColumnField.getText().trim();
            if ( !colorByColumn.equals("") ) {
                imageProperties.colorByColumn = colorByColumn;
            } else {
                imageProperties.colorByColumn = null;
            }

            double valueLimitMin = amountFormat.parse( valueLimitMinField.getText() ).doubleValue();
            double valueLimitMax = amountFormat.parse( valueLimitMaxField.getText() ).doubleValue();
            if ( valueLimitMin != 0.0 && valueLimitMax != 0.0 ) {
                imageProperties.valueLimits = new double[]{valueLimitMin, valueLimitMax};
            } else {
                // 0 is the default value in the text field, if both are left at zero, then this means no value limits
                // are set i.e. null
                imageProperties.valueLimits = null;
            }

            if ( tablesList != null ) {
                if (tablesList.getSelectedIndices().length > 0) {
                    imageProperties.tables = (ArrayList<String>) tablesList.getSelectedValuesList();
                } else {
                    imageProperties.tables = null;
                }
            } else {
                imageProperties.tables = null;
            }

            updateSelectecLabelIds();
            imageProperties.showSelectedSegmentsIn3d = showSelectedSegmentsIn3dCheckbox.isSelected();
            updateDefaultBookmarkSettings();
        }
    }
}
