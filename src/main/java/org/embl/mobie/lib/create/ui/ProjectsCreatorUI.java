/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2024 EMBL
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.embl.mobie.lib.create.ui;

import de.embl.cba.tables.SwingUtils;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import net.imglib2.realtransform.AffineTransform3D;
import org.apache.commons.io.FilenameUtils;
import org.embl.mobie.command.open.project.OpenMoBIEProjectCommand;
import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.io.ImageDataOpener;
import org.embl.mobie.io.util.IOHelper;
import org.embl.mobie.lib.create.ImagesCreator;
import org.embl.mobie.lib.create.ProjectCreator;
import org.embl.mobie.lib.create.ProjectCreatorHelper;
import org.embl.mobie.lib.serialize.Dataset;
import org.embl.mobie.lib.serialize.Project;
import org.embl.mobie.ui.SwingHelper;
import org.embl.mobie.ui.UserInterfaceHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.embl.mobie.lib.create.ProjectCreatorHelper.*;

/**
 * Class for the main user interface of the project creator
 */
public class ProjectsCreatorUI extends JFrame {

    static { net.imagej.patcher.LegacyInjector.preinit(); }

    private ProjectCreator projectCreator;
    private JPanel contentGrid;
    private JComboBox<String> datasetComboBox;
    private JComboBox<String> sourcesComboBox;
    private JComboBox<String> groupsComboBox;
    private JComboBox<String> viewsComboBox;
    private final int labelPaddingX = 15;
    private final int labelPaddingY = 15;

    private static ProjectCreator.ImageType imageType = ProjectCreator.ImageType.Image;
    private static ProjectCreator.AddMethod addMethod = ProjectCreator.AddMethod.Link;
    private static boolean exclusive = false;
    private static boolean useFileNameAsImageName = true;
    private static String uiSelectionGroup = "Make New Ui Selection Group";
    private static boolean is2D = false;

    private final String[] imageFormats = new String[]{ ImageDataFormat.BdvN5.toString(),
            ImageDataFormat.OmeZarr.toString() };
    private final String[] imageTypes = new String[]{ ProjectCreator.ImageType.Image.toString(), ProjectCreator.ImageType.Segmentation.toString() };


    /**
     * Create main project creator panel
     * @param projectLocation project directory
     * @throws IOException
     */
    public ProjectsCreatorUI( File projectLocation ) throws IOException {

        // account for projects with and without the top 'data' directory
        File dataDirectory = ProjectCreatorHelper.getDataLocation( projectLocation );
        this.projectCreator = new ProjectCreator( dataDirectory );

        this.getContentPane().setLayout( new BoxLayout(this.getContentPane(), BoxLayout.Y_AXIS ) );

        // Panel for main content - with a 10 pixel border
        contentGrid = new JPanel();
        contentGrid.setBorder( BorderFactory.createEmptyBorder(10, 10, 10, 10) );
        contentGrid.setLayout( new GridBagLayout());

        addDatasetsToGrid( 0 );
        addSourcesToGrid( 1 );
        addSeparatorToGrid( 2 );
        addViewsToGrid(3 );

        this.getContentPane().add( contentGrid );
        addButtonsPanel();

        String shortenedProjectName = projectLocation.getName();
        if ( shortenedProjectName.length() > 50 ) {
            shortenedProjectName = shortenedProjectName.substring( 0, 47 ) + "...";
        }

        this.setTitle( "Editing MoBIE Project: " + shortenedProjectName );
        this.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
    }

    /**
     * Show the main project creator panel
     */
    public void showProjectsCreatorPanel() {
        this.pack();
        this.setLocationRelativeTo(null);
        this.setVisible( true );
    }

    public ProjectCreator getProjectCreator() {
        return projectCreator;
    }

    private void addDatasetsToGrid(int rowIndex ) {

        final JButton addButton = new JButton("Add");
        final JButton editButton = new JButton("Edit");

        createDatasetComboBox();
        addButton.addActionListener( e ->
        {
            new Thread( () -> { addDatasetDialog(); } ).start();
        } );

        editButton.addActionListener( e ->
        {
            new Thread( () -> { editDatasetDialog(); } ).start();
        } );

        contentGrid.add( createJLabel("dataset"),
                createGridBagConstraints(0.5, 0, rowIndex, labelPaddingX, labelPaddingY));
        contentGrid.add( datasetComboBox, createGridBagConstraints(1, 1, rowIndex, 0, 0) );
        contentGrid.add( addButton, createGridBagConstraints(0.5, 2, rowIndex, 0, 0) );
        contentGrid.add( editButton, createGridBagConstraints(0.5, 3, rowIndex, 0, 0) );
    }

    private String[] getDatasetNames() {
        Project project = projectCreator.getProject();
        if ( project.datasets() != null && project.datasets().size() > 0 ) {
            String[] datasetNames = new String[project.datasets().size()];
            for ( int i = 0; i < project.datasets().size(); i++) {
                datasetNames[i] = project.datasets().get(i);
            }
            return datasetNames;
        } else {
            return new String[]{""};
        }
    }

    private GridBagConstraints createGridBagConstraints(double weightX, int gridX, int gridY, int ipadX, int ipadY) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = weightX;
        constraints.gridx = gridX;
        constraints.gridy = gridY;
        constraints.ipadx = ipadX;
        constraints.ipady = ipadY;
        return constraints;
    }

    private void createDatasetComboBox() {
        String[] datasetNames = getDatasetNames();
        datasetComboBox = new JComboBox<>( datasetNames );
        datasetComboBox.setSelectedItem( datasetNames[0] );
        setComboBoxDisplayParameters(datasetComboBox);
        datasetComboBox.setPrototypeDisplayValue( UserInterfaceHelper.PROTOTYPE_DISPLAY_VALUE);
        datasetComboBox.addItemListener( new SyncAllWithDatasetComboBox() );
    }

    private void createSoucesComboBox() {
        String selectedDataset = (String) datasetComboBox.getSelectedItem();
        Dataset dataset = projectCreator.getDataset( selectedDataset );
        if ( !selectedDataset.equals("") && dataset != null && dataset.sources().keySet().size() > 0 ) {
            String[] imageNames = projectCreator.getDataset( selectedDataset ).sources().keySet().toArray(new String[0]);
            sourcesComboBox = new JComboBox<>( imageNames );
            sourcesComboBox.setSelectedItem( imageNames[0] );
        } else {
            sourcesComboBox = new JComboBox<>( new String[] {""} );
            sourcesComboBox.setSelectedItem( "" );
        }
        setComboBoxDisplayParameters(sourcesComboBox);
        sourcesComboBox.setPrototypeDisplayValue( UserInterfaceHelper.PROTOTYPE_DISPLAY_VALUE);
    }

    private void createGroupsCombobox() {
        String selectedDataset = (String) datasetComboBox.getSelectedItem();
        String[] groupNames = null;
        if ( !selectedDataset.equals("") ) {
            groupNames = projectCreator.getGroups( selectedDataset );
        }

        if ( groupNames != null && groupNames.length > 0 ) {
            groupsComboBox = new JComboBox<>( groupNames );
            groupsComboBox.setSelectedItem( groupNames[0] );
        } else {
            groupsComboBox = new JComboBox<>( new String[] {""} );
            groupsComboBox.setSelectedItem( "" );
        }
        setComboBoxDisplayParameters(groupsComboBox);
        groupsComboBox.setPrototypeDisplayValue( UserInterfaceHelper.PROTOTYPE_DISPLAY_VALUE);
        groupsComboBox.addItemListener( new SyncGroupAndViewComboBox() );
    }

    private void createViewsCombobox() {
        String selectedDataset = (String) datasetComboBox.getSelectedItem();
        String selectedGroup = (String) groupsComboBox.getSelectedItem();
        String[] viewNames = null;
        if ( !selectedDataset.equals("") && !selectedGroup.equals("") ) {
            viewNames = projectCreator.getViews( selectedDataset, selectedGroup );
        }

        if ( viewNames != null && viewNames.length > 0 ) {
            viewsComboBox = new JComboBox<>( viewNames );
            viewsComboBox.setSelectedItem( viewNames[0] );
        } else {
            viewsComboBox = new JComboBox<>( new String[] {""} );
            viewsComboBox.setSelectedItem( "" );
        }

        setComboBoxDisplayParameters(viewsComboBox);
        viewsComboBox.setPrototypeDisplayValue( UserInterfaceHelper.PROTOTYPE_DISPLAY_VALUE);
    }

    private JLabel createJLabel( String text ) {
        JLabel label = new JLabel( text );
        SwingHelper.alignJLabel( label );
        return label;
    }

    private void setComboBoxDisplayParameters(JComboBox< String > comboBox )
    {
        comboBox.setPrototypeDisplayValue( UserInterfaceHelper.PROTOTYPE_DISPLAY_VALUE );
        // Keep preferred height as is, but double the preferred width of the combobox
        // (to ensure there's room to view long names). By basing this on .getPreferredSize(), it will scale with the
        // screen dpi used
        comboBox.setPreferredSize( new Dimension(comboBox.getPreferredSize().width*2, comboBox.getPreferredSize().height ) );
    }

    private class SyncAllWithDatasetComboBox implements ItemListener {
        @Override
        public void itemStateChanged(ItemEvent event) {
            if (event.getStateChange() == ItemEvent.SELECTED) {
                updateSourcesComboBox();
                updateGroupsComboBox();
            }
        }
    }

    private class SyncGroupAndViewComboBox implements ItemListener {
        @Override
        public void itemStateChanged(ItemEvent event) {
            if (event.getStateChange() == ItemEvent.SELECTED) {
                updateViewsComboBox();
            }
        }
    }

    private void addSourcesToGrid(int rowIndex ) {

        final JButton addButton = new JButton( "Add" );
        // for now we don't support editing any image properties, but this is likely to change in future,
        // so keep this code for now
        // final JButton editButton = new JButton("Edit");

        createSoucesComboBox();
        addButton.addActionListener( e ->
        {
            new Thread( () -> { addImageDialog(); } ).start();
        } );

        // editButton.addActionListener( e ->
        // {
        //     new Thread( () -> { editImageDialog(); } ).start();
        // } );

        contentGrid.add( createJLabel("source"),
                createGridBagConstraints(0.5, 0, rowIndex, labelPaddingX, labelPaddingY) );
        contentGrid.add( sourcesComboBox, createGridBagConstraints(1, 1, rowIndex, 0, 0) );
        contentGrid.add( addButton, createGridBagConstraints(0.5, 2, rowIndex, 0, 0) );
    }

    private void addViewsToGrid(int rowIndex ) {

        createGroupsCombobox();
        createViewsCombobox();

        contentGrid.add( createJLabel("group"),
                createGridBagConstraints(0.5, 0, rowIndex, labelPaddingX, labelPaddingY) );
        contentGrid.add( groupsComboBox, createGridBagConstraints(1, 1, rowIndex, 0, 0) );
        contentGrid.add( createJLabel("view"),
                createGridBagConstraints(0.5, 0, rowIndex + 1, labelPaddingX, labelPaddingY));
        contentGrid.add( viewsComboBox, createGridBagConstraints(1, 1, rowIndex+1, 0, 0) );
    }

    private void addSeparatorToGrid( int rowIndex ) {
        GridBagConstraints separatorConstraints = createGridBagConstraints( 0.5, 0, rowIndex, 0, 30 );
        separatorConstraints.gridwidth = contentGrid.getWidth();

        // Have to wrap the separator in another panel to get it to centre properly vertically
        JPanel wrapper = new JPanel(new GridBagLayout());
        GridBagConstraints wrapperConstraints = createGridBagConstraints( 1, 0, 0, 0, 0);
        wrapperConstraints.gridwidth = GridBagConstraints.REMAINDER;
        wrapper.add( new JSeparator(SwingConstants.HORIZONTAL), wrapperConstraints );

        contentGrid.add( wrapper, separatorConstraints );
    }

    private void addButtonsPanel() {
        final JPanel buttonsPanel = SwingUtils.horizontalLayoutPanel();

        JButton remoteButton = new JButton("Add/update remote");
        JButton openMoBIEButton = new JButton("Open in MoBIE");

        remoteButton.addActionListener( e ->
        {
            new Thread( () -> { remoteMetadataSettingsDialog(); } ).start();
        } );

        openMoBIEButton.addActionListener( e ->
        {
            new Thread( () -> {
                OpenMoBIEProjectCommand openMoBIE = new OpenMoBIEProjectCommand();
                openMoBIE.projectLocation = this.projectCreator.getProjectLocation().getAbsolutePath();
                openMoBIE.run();
            } ).start();
        } );

        buttonsPanel.add(remoteButton);
        buttonsPanel.add(openMoBIEButton);

        this.getContentPane().add( buttonsPanel );
    }

    private boolean continueDialog( ImageDataFormat imageDataFormat ) {
        int result = JOptionPane.showConfirmDialog(null,
                "This will overwrite any existing remote metadata for " + imageDataFormat.toString() +
                        " - continue?", "Overwrite remote metadata?",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (result == JOptionPane.YES_OPTION) {
            return true;
        } else {
            return false;
        }
    }

    private void remoteMetadataSettingsDialog()
    {
        List <String > datasets = projectCreator.getProject().datasets();
        if ( datasets == null || datasets.size() == 0 )
        {
            IJ.log( "Remote metadata aborted - there are no datasets in your project!" );
            return;
        }

        final GenericDialog gd = new GenericDialog( "Remote metadata settings..." );
        String[] formats= Arrays.stream( ImageDataFormat.values() ).filter( v -> v.isRemote() ).map( v -> v.toString() ).collect( Collectors.toList() ).toArray( new String[0] );
        gd.addChoice("Image format:", formats, formats[0]);
        gd.addStringField("Signing Region", "", 20);
        gd.addStringField("Service endpoint", "https://...", 20);
        gd.addStringField("Bucket Name", "", 20);

        gd.showDialog();

        if ( !gd.wasCanceled() ) {
            ImageDataFormat format = ImageDataFormat.fromString(gd.getNextChoice());
            String signingRegion = gd.getNextString();
            String serviceEndpoint = gd.getNextString();
            String bucketName = gd.getNextString();

            if ( continueDialog(format) ) {
                projectCreator.getRemoteMetadataCreator().createRemoteMetadata( signingRegion, serviceEndpoint, bucketName, format );
            }
        }
    }

    /**
     * Dialog to choose a dataset
     * @return Name of chosen datset, or null if cancelled
     */
    public String chooseDatasetDialog() {
        final GenericDialog gd = new GenericDialog( "Choose a dataset" );
        String[] currentDatasets = getDatasetNames();
        gd.addChoice("Dataset", currentDatasets, currentDatasets[0]);
        gd.showDialog();

        if ( !gd.wasCanceled() ) {
            return gd.getNextChoice();

        } else {
            return null;
        }

    }

    /**
     * Dialog to add an image to a MoBIE project
     */
    public void addImageDialog() {
        final GenericDialog gd = new GenericDialog( "Add..." );
        String[] addMethods = new String[] {"current displayed image", "stored ome-zarr"};
        gd.addChoice("Add:", addMethods, "current displayed image");
        gd.showDialog();

        if ( !gd.wasCanceled() ) {
            String addMethod = gd.getNextChoice();
            if (addMethod.equals("current displayed image")) {
                addCurrentImageDialog();
            } else if (addMethod.equals("stored ome-zarr")) {
                addOMEZarrDialog();
            }

        }
    }

    public void addOMEZarrDialog() {
        String datasetName = (String) datasetComboBox.getSelectedItem();

        if (!datasetName.equals(""))
        {
            final GenericDialog gd = new GenericDialog( "Add OME-Zarr To Project..." );
            String[] addMethods = new String[]{
                    ProjectCreator.AddMethod.Link.toString(),
                    ProjectCreator.AddMethod.Copy.toString() };
            gd.addChoice( "Add method:", addMethods, addMethod.toString() );
            gd.addChoice( "Image type", imageTypes, imageType.toString() );
            gd.addCheckbox( "Make view exclusive", exclusive );
            gd.addCheckbox( "Use filename as image name", useFileNameAsImageName );

            gd.showDialog();
            if ( gd.wasCanceled() ) return;

            addMethod = ProjectCreator.AddMethod.valueOf( gd.getNextChoice() );
            imageType = ProjectCreator.ImageType.valueOf( gd.getNextChoice() );
            exclusive = gd.getNextBoolean();
            useFileNameAsImageName = gd.getNextBoolean();

            String filePath = getOMEZarrImagePathDialog();

            if ( filePath != null )
            {
                addOMEZarr( filePath, datasetName );
            }
        }
    }

    private void addOMEZarr( String uri, String datasetName )
    {
        if ( ! isImageValid( uri, projectCreator.getVoxelUnit() ) ) {
            return;
        }

        if ( ! is2D( ImageDataOpener.open( uri ) ) && projectCreator.getDataset( datasetName ).is2D() ) {
            if ( ! changeDatasetDimensionDialog (datasetName ) ) {
                return;
            }
        }

        File imageFile = new File( uri );
        String imageName = imageFile.getName().split("\\.")[0];
        if ( !useFileNameAsImageName ) {
            imageName = imageNameDialog( imageFile );
            if ( imageName == null ) {
                return;
            }
        }

        ImagesCreator imagesCreator = projectCreator.getImagesCreator();
        boolean overwriteImage = true;
        if ( imagesCreator.imageExists( datasetName, imageName ) ) {
            overwriteImage = overwriteImageDialog();
        }
        if ( !overwriteImage ) {
            return;
        }

        String chosenUiSelectionGroup = selectUiSelectionGroupDialog(datasetName);
        if ( chosenUiSelectionGroup == null ) {
            return;
        } else {
            uiSelectionGroup = chosenUiSelectionGroup;
        }

        imagesCreator.addOMEZarrImage( uri, imageName, datasetName, imageType, addMethod, uiSelectionGroup, exclusive );
        updateComboBoxesForNewImage(imageName, uiSelectionGroup);

    }



    private String getOMEZarrImagePathDialog()
    {
        String filePath = UserInterfaceHelper.selectDirectoryPath( ".ome.zarr file", true );
        if ( ! isValidOMEZarr( filePath ) )
        {
            IJ.log( "This is not a valid OME-Zarr image." );
            return null;
        }
        else
        {
            return filePath;
        }
    }

    private String selectUiSelectionGroupDialog( String datasetName ) {

        String[] currentSelectionGroups = projectCreator.getGroups( datasetName );

        String chosenGroup = null;
        if ( currentSelectionGroups == null || currentSelectionGroups.length == 0 ) {
            chosenGroup = ProjectCreatorHelper.makeNewUiSelectionGroup( new String[]{} );
        } else {
            final GenericDialog gd = new GenericDialog( "Choose selection group for image view..." );
            String[] choices = new String[currentSelectionGroups.length + 1];
            choices[0] = "Make New Ui Selection Group";
            for (int i = 0; i < currentSelectionGroups.length; i++) {
                choices[i + 1] = currentSelectionGroups[i];
            }
            gd.addChoice("Ui Selection Group", choices, uiSelectionGroup);
            gd.showDialog();

            if ( !gd.wasCanceled() ) {
                String choice = gd.getNextChoice();
                if ( choice.equals("Make New Ui Selection Group") ) {
                    chosenGroup = ProjectCreatorHelper.makeNewUiSelectionGroup( currentSelectionGroups );
                } else {
                    chosenGroup = choice;
                }
            }
        }

        return chosenGroup;
    }

    private void updateComboBoxesForNewImage( String imageName, String uiSelectionGroup ) {
        updateSourcesComboBox( imageName );
        if ( uiSelectionGroup != null ) {
            updateGroupsComboBox( uiSelectionGroup );
            viewsComboBox.setSelectedItem( imageName );
        } else {
            updateGroupsComboBox();
        }
    }

    private boolean overwriteImageDialog() {
        int result = JOptionPane.showConfirmDialog(null,
                "This image name already exists. Overwrite image?", "Are you sure?",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (result == JOptionPane.YES_OPTION) {
            return true;
        } else {
            return false;
        }
    }

    private boolean changeDatasetDimensionDialog( String datasetName ) {
        int result = JOptionPane.showConfirmDialog(null,
                "This image is 3D, but the dataset (" + datasetName + ") is 2D. \n" +
                        "Change the dataset to be 3D?", "Are you sure?",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (result == JOptionPane.YES_OPTION) {
            projectCreator.getDatasetsCreator().makeDataset2D( datasetName, false );
            return true;
        } else {
            IJ.log("Adding image aborted - can't add a 3D image to a 2D dataset" );
            return false;
        }
    }

    private String imageNameDialog( File imageFile ) {
        final GenericDialog gd = new GenericDialog( "Choose image name..." );
        gd.addStringField( "Image Name", imageFile.getName().split("\\.")[0], 35 );
        gd.showDialog();

        String imageName = null;
        if ( !gd.wasCanceled() ) {
            imageName = gd.getNextString();
            // tidy up image name, remove any spaces
            imageName = UserInterfaceHelper.tidyString( imageName );
        }
        return imageName;
    }

    /**
     * Dialog for adding image currently open in ImageJ to the MoBIE project
     */
    public void addCurrentImageDialog() {
        String datasetName = (String) datasetComboBox.getSelectedItem();

        if ( !datasetName.equals("") ) {
            ImagePlus currentImage = IJ.getImage();

            if ( !isImageValid( currentImage.getNChannels(), currentImage.getCalibration().getUnit(),
                    projectCreator.getVoxelUnit(), false ) ) {
                return;
            }

            if ( currentImage.getNDimensions() > 2 && projectCreator.getDataset( datasetName ).is2D() ) {
                if ( !changeDatasetDimensionDialog(datasetName) ) {
                    return;
                }
            }

            final GenericDialog gd = new GenericDialog( "Add Current Image To MoBIE Project..." );
            gd.addMessage( "Make sure your voxel size, and unit,\n are set properly under Image > Properties...");
            gd.addStringField( "Image Name", FilenameUtils.removeExtension(currentImage.getTitle()), 35 );
            gd.addChoice( "Image Type", imageTypes, imageType.toString() );
            gd.addCheckbox("Make view exclusive", exclusive );

            gd.addMessage( getVoxelSizeString( currentImage ) );

            gd.addMessage("Additional view affine transform:");
            gd.addStringField("Row 1", "1.0, 0.0, 0.0, 0.0", 25 );
            gd.addStringField( "Row 2", "0.0, 1.0, 0.0, 0.0", 25 );
            gd.addStringField( "Row 3", "0.0, 0.0, 1.0, 0.0", 25 );

            gd.showDialog();

            if ( !gd.wasCanceled() ) {
                String imageName = gd.getNextString();
                imageType = ProjectCreator.ImageType.valueOf( gd.getNextChoice() );
                exclusive = gd.getNextBoolean();

                String affineRow1 = gd.getNextString().trim();
                String affineRow2 = gd.getNextString().trim();
                String affineRow3 = gd.getNextString().trim();
                String affineTransform = String.join(",", affineRow1, affineRow2, affineRow3 );

                // tidy up image name, remove any spaces
                imageName = UserInterfaceHelper.tidyString( imageName );
                AffineTransform3D sourceTransform = ProjectCreatorHelper.parseAffineString( affineTransform );

                if ( imageName != null && sourceTransform != null ) {
                    ImagesCreator imagesCreator = projectCreator.getImagesCreator();

                    boolean overwriteImage = true;
                    if ( imagesCreator.imageExists( datasetName, imageName ) ) {
                        overwriteImage = overwriteImageDialog();
                    }
                    if ( !overwriteImage ) {
                        return;
                    }

                    String chosenUiSelectionGroup = selectUiSelectionGroupDialog(datasetName);
                    if ( chosenUiSelectionGroup == null ) {
                        return;
                    } else {
                        uiSelectionGroup = chosenUiSelectionGroup;
                    }

                    imagesCreator.addImage(currentImage, imageName, datasetName, imageType, sourceTransform, uiSelectionGroup, exclusive);
                    updateComboBoxesForNewImage(imageName, uiSelectionGroup);
                }
            }

        } else {
            IJ.log( "Add image failed - create a dataset first" );
        }
    }

    // TODO: this could be moved to mobie-io
    private boolean isValidOMEZarr( String uri )
    {
        return ( new File( IOHelper.combinePath( uri, ".zgroup" ) ).exists() && new File( IOHelper.combinePath( uri, ".zattrs" ) ).exists() );
    }

    /**
     * Dialog to add a new dataset to a MoBIE project
     */
    public void addDatasetDialog () {
        final GenericDialog gd = new GenericDialog( "Create a new dataset" );
        gd.addStringField( "Name of dataset", "", 35 );
        gd.addCheckbox("Limit images and display to only 2D?", is2D );
        gd.showDialog();

        if ( !gd.wasCanceled() ) {
            String datasetName = gd.getNextString();
            datasetName = UserInterfaceHelper.tidyString( datasetName );
            is2D = gd.getNextBoolean();

            if ( datasetName != null ) {
                projectCreator.getDatasetsCreator().addDataset( datasetName, is2D );
                updateDatasetsComboBox( datasetName );
            }
        }
    }

    private void editDatasetDialog() {
        final GenericDialog gd = new GenericDialog( "Edit dataset..." );
        String oldName = (String) datasetComboBox.getSelectedItem();
        if ( !oldName.equals("") ) {
            boolean isDefault = projectCreator.getProject().getDefaultDataset().equals(oldName);

            gd.addStringField("Dataset name", oldName, 35);
            if (isDefault) {
                gd.addMessage("     This dataset is the default");
            } else {
                gd.addCheckbox("Make default dataset", false);
            }
            gd.showDialog();

            if (!gd.wasCanceled()) {
                String newName = gd.getNextString();
                newName = UserInterfaceHelper.tidyString( newName );
                if ( newName != null ) {
                    projectCreator.getDatasetsCreator().renameDataset( oldName, newName );
                    updateDatasetsComboBox( newName );
                }

                if (!isDefault) {
                    boolean makeDefault = gd.getNextBoolean();
                    if (makeDefault) {
                        projectCreator.getDatasetsCreator().makeDefaultDataset( newName );
                    }
                }

            }
        }
    }

    private void updateDatasetsComboBox( String selection ) {
        datasetComboBox.removeAllItems();
        for ( String datasetName : projectCreator.getProject().datasets() ) {
            datasetComboBox.addItem(datasetName);
        }

        datasetComboBox.setSelectedItem( selection );
    }

    private void updateSourcesComboBox(String selection ) {
        updateSourcesComboBox();
        sourcesComboBox.setSelectedItem( selection );
    }

    private void updateSourcesComboBox() {
        String currentDataset = (String) datasetComboBox.getSelectedItem();

        if ( currentDataset != null && !currentDataset.equals("") ) {
            sourcesComboBox.removeAllItems();
            Dataset dataset = projectCreator.getDataset( currentDataset );
            if ( dataset != null && dataset.sources().keySet().size() > 0 ) {
                for (String sourceName : dataset.sources().keySet() ) {
                    sourcesComboBox.addItem( sourceName );
                }
            } else {
                sourcesComboBox.addItem( "" );
            }
        }
    }

    private void updateGroupsComboBox( String selection ) {
        updateGroupsComboBox();
        groupsComboBox.setSelectedItem( selection );
    }

    private void updateGroupsComboBox() {
        String currentDataset = (String) datasetComboBox.getSelectedItem();
        if ( currentDataset != null && !currentDataset.equals("") ) {
            groupsComboBox.removeAllItems();
            String[] groups = projectCreator.getGroups( currentDataset );
            if ( groups != null && groups.length > 0 ) {
                for ( String group : groups ) {
                    groupsComboBox.addItem( group );
                }
            } else {
                groupsComboBox.addItem( "" );
            }
        }
    }

    private void updateViewsComboBox() {
        String currentDataset = (String) datasetComboBox.getSelectedItem();
        String currentGroup = (String) groupsComboBox.getSelectedItem();

        if ( currentDataset != null && !currentDataset.equals("") ) {
            viewsComboBox.removeAllItems();
            String[] views = projectCreator.getViews( currentDataset, currentGroup );
            if ( views != null && views.length > 0 ) {
                for ( String view: views ) {
                    viewsComboBox.addItem(view);
                }
            } else {
                viewsComboBox.addItem( "" );
            }
        }
    }


}
