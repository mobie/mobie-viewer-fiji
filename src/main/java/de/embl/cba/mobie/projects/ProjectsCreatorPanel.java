package de.embl.cba.mobie.projects;

import bdv.ij.ExportImagePlusAsN5PlugIn;
import de.embl.cba.mobie.h5.ExportImagePlusAsH5;
import de.embl.cba.mobie.image.ImagePropertiesEditor;
import de.embl.cba.mobie.n5.ExportImagePlusAsN5;
import de.embl.cba.mobie.ui.viewer.MoBIEViewer;
import de.embl.cba.mobie.utils.Utils;
import de.embl.cba.tables.SwingUtils;
import ij.gui.GenericDialog;
import mpicbg.spim.data.SpimDataException;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import static de.embl.cba.mobie.utils.ui.SwingUtils.*;

public class ProjectsCreatorPanel extends JFrame {
    private ProjectsCreator projectsCreator;
    private JComboBox<String> datasetComboBox;
    private JComboBox<String> imagesComboBox;

    public ProjectsCreatorPanel ( File projectLocation ) {
        this.projectsCreator = new ProjectsCreator( projectLocation );
        addDatasetPanel();
        addImagesPanel();
        this.setTitle( "Edit MoBIE Project...");
        this.getContentPane().setLayout( new BoxLayout(this.getContentPane(), BoxLayout.Y_AXIS ) );
        this.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
    }

    public void showProjectsCreatorPanel() {
        this.pack();
        this.setLocationRelativeTo(null);
        this.setVisible( true );
    }

    public ProjectsCreator getProjectsCreator() {
        return projectsCreator;
    }

    private void addDatasetPanel() {
        final JPanel horizontalLayoutPanel = SwingUtils.horizontalLayoutPanel();

        final JButton addButton = getButton("Add");
        final JButton editButton = getButton("Edit");

        createDatasetComboBox();
        addButton.addActionListener( e ->
        {
            new Thread( () -> { addDatasetDialog(); } ).start();
        } );

        editButton.addActionListener( e ->
        {
            new Thread( () -> { editDatasetDialog(); } ).start();
        } );

        horizontalLayoutPanel.add(getJLabel("dataset", 60, 10));
        horizontalLayoutPanel.add(datasetComboBox);
        horizontalLayoutPanel.add(addButton);
        horizontalLayoutPanel.add(editButton);
        horizontalLayoutPanel.setAlignmentX( Component.LEFT_ALIGNMENT );

        this.getContentPane().add(horizontalLayoutPanel);
    }

    private void createDatasetComboBox() {
        datasetComboBox = new JComboBox<>( projectsCreator.getCurrentDatasets() );
        setComboBoxDimensions(datasetComboBox);
        datasetComboBox.setPrototypeDisplayValue(MoBIEViewer.PROTOTYPE_DISPLAY_VALUE);
        datasetComboBox.addActionListener( new SyncImageAndDatasetComboBox() );
    }

    private class SyncImageAndDatasetComboBox implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            updateImagesComboBox();
        }
    }

    private void addImagesPanel() {
        final JPanel horizontalLayoutPanel = SwingUtils.horizontalLayoutPanel();

        final JButton addButton = getButton( "Add" );
        final JButton editButton = getButton("Edit");

        imagesComboBox = new JComboBox<>( projectsCreator.getCurrentImages( (String) datasetComboBox.getSelectedItem()) );
        setComboBoxDimensions(imagesComboBox);
        addButton.addActionListener( e ->
        {
            new Thread( () -> { addImageDialog(); } ).start();
        } );

        editButton.addActionListener( e ->
        {
            new Thread( () -> { editImageDialog(); } ).start();
        } );

        imagesComboBox.setPrototypeDisplayValue(MoBIEViewer.PROTOTYPE_DISPLAY_VALUE);

        horizontalLayoutPanel.add(getJLabel("image", 60, 10));
        horizontalLayoutPanel.add(imagesComboBox);
        horizontalLayoutPanel.add( addButton );
        horizontalLayoutPanel.add( editButton );
        horizontalLayoutPanel.setAlignmentX( Component.LEFT_ALIGNMENT );

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

    public void addImageDialog() {
        final GenericDialog gd = new GenericDialog( "Add..." );
        String[] addMethods = new String[] {"current open image", "bdv format image"};
        gd.addChoice("Add:", addMethods, "current open image");
        gd.showDialog();

        if ( !gd.wasCanceled() ) {
            String addMethod = gd.getNextChoice();
            if (addMethod.equals("current open image")) {
                addCurrentOpenImageDialog();
            } else if (addMethod.equals("bdv format image")) {
                addBdvFormatImageDialog();
            }

        }
    }

    private ExportImagePlusAsH5.H5Parameters getH5ManualExportParameters ( String datasetName, String imageName ) {

        final GenericDialog manualSettings = new GenericDialog( "Manual Settings for BigDataViewer XML/H5" );

        // same settings as https://github.com/bigdataviewer/bigdataviewer_fiji/blob/master/src/main/java/bdv/ij/ExportImagePlusPlugIn.java#L357
        // but hiding settings like e.g. export location that shouldn't be set manually

        manualSettings.addStringField( "Subsampling_factors", ExportImagePlusAsH5.getLastManualSubsampling(), 25 );
        manualSettings.addStringField( "Hdf5_chunk_sizes", ExportImagePlusAsH5.getLastManualChunkSizes(), 25 );

        manualSettings.addMessage( "" );
        final String[] minMaxChoices = new String[] { "Use ImageJ's current min/max setting", "Compute min/max of the (hyper-)stack", "Use values specified below" };
        manualSettings.addChoice( "Value_range", minMaxChoices, minMaxChoices[ ExportImagePlusAsH5.getLastManualMinMaxChoice() ] );
        manualSettings.addNumericField( "Min", ExportImagePlusAsH5.getLastManualMin(), 0 );
        manualSettings.addNumericField( "Max", ExportImagePlusAsH5.getLastManualMax(), 0 );

        manualSettings.addMessage( "" );
        manualSettings.addCheckbox( "split_hdf5", ExportImagePlusAsH5.getLastManualSplit() );
        manualSettings.addNumericField( "timepoints_per_partition", ExportImagePlusAsH5.getLastManualTimepointsPerPartition(), 0, 25, "" );
        manualSettings.addNumericField( "setups_per_partition", ExportImagePlusAsH5.getLastManualSetupsPerPartition(), 0, 25, "" );

        manualSettings.addMessage( "" );
        manualSettings.addCheckbox( "use_deflate_compression", ExportImagePlusAsH5.getLastManualDeflate() );

        manualSettings.showDialog();

        if ( !manualSettings.wasCanceled() ) {
            String xmlPath = projectsCreator.getLocalImageXmlPath( datasetName, imageName );
            String subsamplingFactors = manualSettings.getNextString();
            String chunkSizes = manualSettings.getNextString();
            int minMaxChoice = manualSettings.getNextChoiceIndex();
            double min = manualSettings.getNextNumber();
            double max = manualSettings.getNextNumber();
            boolean splitHdf5 = manualSettings.getNextBoolean();
            int timePointsPerPartition = (int) manualSettings.getNextNumber();
            int setupsPerPartition = (int) manualSettings.getNextNumber();
            boolean useDeflateCompression = manualSettings.getNextBoolean();

            return new ExportImagePlusAsH5().getManualParameters( subsamplingFactors, chunkSizes, minMaxChoice,
                    min, max, splitHdf5, timePointsPerPartition, setupsPerPartition, useDeflateCompression, xmlPath );

        } else {
            return null;
        }
    }

    private ExportImagePlusAsN5.N5Parameters getN5ManualExportParameters(String datasetName, String imageName ) {

        final GenericDialog manualSettings = new GenericDialog( "Manual Settings for BigDataViewer XML/N5" );

        // same settings as https://github.com/bigdataviewer/bigdataviewer_fiji/blob/master/src/main/java/bdv/ij/ExportImagePlusAsN5PlugIn.java#L345
        // but hiding settings like e.g. export location that shouldn't be set manually

        manualSettings.addStringField( "Subsampling_factors", ExportImagePlusAsN5.getLastManualSubsampling(), 25 );
        manualSettings.addStringField( "N5_chunk_sizes", ExportImagePlusAsN5.getLastManualChunkSizes(), 25 );
        final String[] compressionChoices = new String[] { "raw (no compression)", "bzip", "gzip", "lz4", "xz" };
        manualSettings.addChoice( "compression", compressionChoices, compressionChoices[ ExportImagePlusAsN5.getLastManualCompressionChoice() ] );

        manualSettings.showDialog();

        if ( !manualSettings.wasCanceled() ) {
            String xmlPath = projectsCreator.getLocalImageXmlPath( datasetName, imageName );
            String subsamplingFactors = manualSettings.getNextString();
            String chunkSizes = manualSettings.getNextString();
            int compressionChoice = manualSettings.getNextChoiceIndex();

            return new ExportImagePlusAsN5().getManualParameters( subsamplingFactors, chunkSizes, compressionChoice, xmlPath );
        } else {
            return null;
        }

    }

    public void addCurrentOpenImageDialog() {
        String datasetName = (String) datasetComboBox.getSelectedItem();

        if (!datasetName.equals("")) {

            String defaultAffineTransform = projectsCreator.getDefaultAffineForCurrentImage();

            final GenericDialog gd = new GenericDialog( "Add Current Image To MoBIE Project..." );
            gd.addMessage( "Make sure your pixel size, and unit,\n are set properly under Image > Properties...");
            gd.addStringField( "Image Name", "" );
            String[] imageTypes = new String[] {"image", "segmentation", "mask"};
            gd.addChoice( "Image Type", imageTypes, "image" );
            String[] bdvFormats = new String[] {"n5", "h5"};
            gd.addChoice( "Bdv format", bdvFormats, "n5" );
            gd.addStringField("Affine", defaultAffineTransform, 32 );
            gd.addCheckbox("Use default export settings", true);

            gd.showDialog();

            if ( !gd.wasCanceled() ) {
                String imageName = gd.getNextString();
                String imageType = gd.getNextChoice();
                String bdvFormat = gd.getNextChoice();
                String affineTransform = gd.getNextString().trim();
                boolean useDefaultSettings = gd.getNextBoolean();

                // tidy up image name, remove any spaces
                imageName = tidyString( imageName );

                if ( imageName != null && isValidAffine( affineTransform ) ) {

                    if ( bdvFormat.equals("h5") ) {
                        // pass
                    } else if ( bdvFormat.equals("n5") ) {
                        ExportImagePlusAsN5.N5Parameters parameters = null;
                        if ( !useDefaultSettings ) {
                            parameters = getN5ManualExportParameters( datasetName, imageName);
                        }

                        if (!affineTransform.equals(defaultAffineTransform)) {
                            projectsCreator.addN5Image(imageName, datasetName, imageType, affineTransform, parameters);
                        } else {
                            projectsCreator.addN5Image(imageName, datasetName, imageType, null, parameters);
                        }
                    }
                    updateDatasetsComboBox(datasetName);
                }
            }

        } else {
            Utils.log( "Add image failed - create a dataset first" );
        }
    }

    private boolean isValidAffine ( String affine ) {
        if ( !affine.matches("^[0-9. ]+$") ) {
            Utils.log( "Invalid affine transform - must contain only numbers and spaces");
            return false;
        }

        String[] splitAffine = affine.split(" ");
        if ( splitAffine.length != 12) {
            Utils.log( "Invalid affine transform - must be of length 12");
            return false;
        }

        return true;
    }

    private String tidyString( String string ) {
        string = string.trim();
        String tidyString = string.replaceAll("\\s+","_");

        if ( !string.equals(tidyString) ) {
            Utils.log( "Spaces were removed from name, and replaced by _");
        }

        // check only contains alphanumerics, or _ -
        if ( !tidyString.matches("^[a-zA-Z0-9_-]+$") ) {
            Utils.log( "Names must only contain letters, numbers, _ or -. Please try again " +
                    "with a different name.");
            tidyString = null;
        }

        return tidyString;
    }

    public void addBdvFormatImageDialog() {
        String datasetName = (String) datasetComboBox.getSelectedItem();

        if (!datasetName.equals("")) {

            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new FileNameExtensionFilter("xml", "xml"));
            chooser.setDialogTitle( "Select bdv xml..." );
            int returnVal = chooser.showOpenDialog(null );
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File xmlLocation = new File( chooser.getSelectedFile().getAbsolutePath() );
                final GenericDialog gd = new GenericDialog("Add Bdv Format Image To Project...");
                String[] addMethods = new String[]{"link to current image location", "copy image", "move image"};
                gd.addChoice("Add method:", addMethods, "link to current image location");
                String[] imageTypes = new String[]{"image", "segmentation", "mask"};
                gd.addChoice("Image Type", imageTypes, "image");
                String[] bdvFormats = new String[]{"n5", "h5"};
                gd.addChoice("Bdv format", bdvFormats, "n5");

                gd.showDialog();

                if (!gd.wasCanceled()) {
                    String addMethod = gd.getNextChoice();
                    String imageType = gd.getNextChoice();
                    String bdvFormat = gd.getNextChoice();

                    try {
                        projectsCreator.addBdvFormatImage( xmlLocation, datasetName, bdvFormat, imageType, addMethod );
                        updateDatasetsComboBox( datasetName );
                    } catch (SpimDataException | IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            Utils.log( "Add image failed - create a dataset first" );
        }
    }

    public void addDatasetDialog () {
        final GenericDialog gd = new GenericDialog( "Create a new dataset" );
        gd.addStringField( "Name of dataset", "");
        gd.showDialog();

        if ( !gd.wasCanceled() ) {
            String datasetName = gd.getNextString();
            datasetName = tidyString( datasetName );

            if ( datasetName != null ) {
                // check not already in datasets
                boolean contains = projectsCreator.isInDatasets(datasetName);
                if (!contains) {
                    projectsCreator.addDataset(datasetName);
                    updateDatasetsComboBox(datasetName);
                } else {
                    Utils.log("Add dataset failed - dataset already exists");
                }
            }
        }
    }

    private void editDatasetDialog() {
        final GenericDialog gd = new GenericDialog( "Edit dataset..." );
        String oldName = (String) datasetComboBox.getSelectedItem();
        if (!oldName.equals("")) {
            boolean isDefault = projectsCreator.isDefaultDataset(oldName);

            gd.addStringField("Dataset name", oldName);
            if (isDefault) {
                gd.addMessage("This dataset is the default");
            } else {
                gd.addCheckbox("Make default dataset", false);
            }
            gd.showDialog();

            if (!gd.wasCanceled()) {
                String newName = gd.getNextString();
                newName = tidyString( newName );
                if ( newName != null && !newName.equals(oldName) ) {
                    // check not already in datasets
                    boolean contains = projectsCreator.isInDatasets(newName);
                    if (!contains) {
                        projectsCreator.renameDataset(oldName, newName);
                        updateDatasetsComboBox(newName);
                    }
                }

                if (!isDefault) {
                    boolean makeDefault = gd.getNextBoolean();
                    if (makeDefault) {
                        projectsCreator.makeDefaultDataset(newName);
                    }
                }

            }
        }
    }

    private void editImageDialog() {
        String datasetName = (String) datasetComboBox.getSelectedItem();
        String imageName = (String) imagesComboBox.getSelectedItem();

        if (!datasetName.equals("") && !imageName.equals("")) {
            new ImagePropertiesEditor(datasetName,
                    imageName, projectsCreator);
        }
    }

    private void updateDatasetsComboBox( String selection ) {
        updateDatasetsComboBox();
        datasetComboBox.setSelectedItem( selection );
    }

    private void updateDatasetsComboBox () {
        if ( datasetComboBox != null ) {
            datasetComboBox.removeAllItems();
            for (String datasetName : projectsCreator.getCurrentDatasets()) {
                datasetComboBox.addItem(datasetName);
            }

            if ( imagesComboBox != null ) {
                updateImagesComboBox();
            }
        }
    }

    private void updateImagesComboBox( String selection ) {
        updateImagesComboBox();
        imagesComboBox.setSelectedItem( selection );
    }

    private void updateImagesComboBox () {
        if ( datasetComboBox != null && imagesComboBox != null ) {
            imagesComboBox.removeAllItems();
            String currentDataset = (String) datasetComboBox.getSelectedItem();
            for (String imageName : projectsCreator.getCurrentImages( currentDataset )) {
                imagesComboBox.addItem(imageName);
            }
        }
    }


}
