/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2022 EMBL
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
import org.embl.mobie.lib.serialize.Project;
import org.embl.mobie.command.open.project.OpenMoBIEProjectCommand;
import org.embl.mobie.lib.create.ImagesCreator;
import org.embl.mobie.lib.create.ProjectCreator;
import org.embl.mobie.lib.create.ProjectCreatorHelper;
import org.embl.mobie.lib.serialize.Dataset;
import org.embl.mobie.lib.ui.SwingHelper;
import org.embl.mobie.lib.ui.UserInterfaceHelper;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import net.imglib2.realtransform.AffineTransform3D;
import org.apache.commons.io.FilenameUtils;
import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.io.SpimDataOpener;
import org.embl.mobie.io.util.IOHelper;
import org.janelia.saalfeldlab.n5.Compression;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.embl.mobie.lib.create.ProjectCreatorHelper.getVoxelSizeString;
import static org.embl.mobie.lib.create.ProjectCreatorHelper.isImageValid;
import static org.embl.mobie.lib.create.ProjectCreatorHelper.isSpimData2D;

public class ProjectsCreatorPanel extends JFrame {

    static { net.imagej.patcher.LegacyInjector.preinit(); }

    private ProjectCreator projectsCreator;
    private JComboBox<String> datasetComboBox;
    private JComboBox<String> sourcesComboBox;
    private JComboBox<String> groupsComboBox;
    private JComboBox<String> viewsComboBox;

    private static ProjectCreator.ImageType imageType = ProjectCreator.ImageType.image;
    private static ImageDataFormat imageDataFormat = ImageDataFormat.BdvN5;
    private static ProjectCreator.AddMethod addMethod = ProjectCreator.AddMethod.link;
    private static boolean useDefaultExportSettings = true;
    private static boolean exclusive = false;
    private static boolean useFileNameAsImageName = true;
    private static String uiSelectionGroup = "Make New Ui Selection Group";
    private static boolean is2D = false;

    private final String[] imageFormats = new String[]{ ImageDataFormat.BdvN5.toString(),
            ImageDataFormat.OmeZarr.toString() };
    private final String[] imageTypes = new String[]{ ProjectCreator.ImageType.image.toString(), ProjectCreator.ImageType.segmentation.toString() };


    public ProjectsCreatorPanel ( File projectLocation ) throws IOException {

        // account for projects with and without the top 'data' directory
        File dataDirectory = ProjectCreatorHelper.getDataLocation( projectLocation );
        this.projectsCreator = new ProjectCreator( dataDirectory );

        addDatasetPanel();
        addSourcesPanel();
        this.getContentPane().add(new JSeparator(SwingConstants.HORIZONTAL));
        this.getContentPane().add( Box.createVerticalStrut( 10 ) );
        addViewsPanel();
        addButtonsPanel();

        String shortenedProjectName = projectLocation.getName();
        if ( shortenedProjectName.length() > 50 ) {
            shortenedProjectName = shortenedProjectName.substring( 0, 47 ) + "...";
        }
        this.setTitle( "Editing MoBIE Project: " + shortenedProjectName );
        this.getContentPane().setLayout( new BoxLayout(this.getContentPane(), BoxLayout.Y_AXIS ) );
        this.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
    }

    public void showProjectsCreatorPanel() {
        this.pack();
        this.setLocationRelativeTo(null);
        this.setVisible( true );
    }

    public ProjectCreator getProjectsCreator() {
        return projectsCreator;
    }

    private void addDatasetPanel() {
        final JPanel horizontalLayoutPanel = SwingUtils.horizontalLayoutPanel();

        final JButton addButton = SwingHelper.createButton("Add");
        final JButton editButton = SwingHelper.createButton("Edit");

        createDatasetComboBox();
        addButton.addActionListener( e ->
        {
            new Thread( () -> { addDatasetDialog(); } ).start();
        } );

        editButton.addActionListener( e ->
        {
            new Thread( () -> { editDatasetDialog(); } ).start();
        } );

        horizontalLayoutPanel.add( SwingHelper.getJLabel("dataset", 60, 10));
        horizontalLayoutPanel.add(datasetComboBox);
        horizontalLayoutPanel.add(addButton);
        horizontalLayoutPanel.add(editButton);
        horizontalLayoutPanel.setAlignmentX( Component.LEFT_ALIGNMENT );

        this.getContentPane().add(horizontalLayoutPanel);
    }

    private String[] getDatasetNames() {
        Project project = projectsCreator.getProject();
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

    private void createDatasetComboBox() {
        String[] datasetNames = getDatasetNames();
        datasetComboBox = new JComboBox<>( datasetNames );
        datasetComboBox.setSelectedItem( datasetNames[0] );
        setComboBoxDimensions(datasetComboBox);
        datasetComboBox.setPrototypeDisplayValue( UserInterfaceHelper.PROTOTYPE_DISPLAY_VALUE);
        datasetComboBox.addItemListener( new SyncAllWithDatasetComboBox() );
    }

    private void createSoucesComboBox() {
        String selectedDataset = (String) datasetComboBox.getSelectedItem();
        Dataset dataset = projectsCreator.getDataset( selectedDataset );
        if ( !selectedDataset.equals("") && dataset != null && dataset.sources().keySet().size() > 0 ) {
            String[] imageNames = projectsCreator.getDataset( selectedDataset ).sources().keySet().toArray(new String[0]);
            sourcesComboBox = new JComboBox<>( imageNames );
            sourcesComboBox.setSelectedItem( imageNames[0] );
        } else {
            sourcesComboBox = new JComboBox<>( new String[] {""} );
            sourcesComboBox.setSelectedItem( "" );
        }
        setComboBoxDimensions(sourcesComboBox);
        sourcesComboBox.setPrototypeDisplayValue( UserInterfaceHelper.PROTOTYPE_DISPLAY_VALUE);
    }

    private void createGroupsCombobox() {
        String selectedDataset = (String) datasetComboBox.getSelectedItem();
        String[] groupNames = null;
        if ( !selectedDataset.equals("") ) {
            groupNames = projectsCreator.getGroups( selectedDataset );
        }

        if ( groupNames != null && groupNames.length > 0 ) {
            groupsComboBox = new JComboBox<>( groupNames );
            groupsComboBox.setSelectedItem( groupNames[0] );
        } else {
            groupsComboBox = new JComboBox<>( new String[] {""} );
            groupsComboBox.setSelectedItem( "" );
        }
        setComboBoxDimensions(groupsComboBox);
        groupsComboBox.setPrototypeDisplayValue( UserInterfaceHelper.PROTOTYPE_DISPLAY_VALUE);
        groupsComboBox.addItemListener( new SyncGroupAndViewComboBox() );
    }

    private void createViewsCombobox() {
        String selectedDataset = (String) datasetComboBox.getSelectedItem();
        String selectedGroup = (String) groupsComboBox.getSelectedItem();
        String[] viewNames = null;
        if ( !selectedDataset.equals("") && !selectedGroup.equals("") ) {
            viewNames = projectsCreator.getViews( selectedDataset, selectedGroup );
        }

        if ( viewNames != null && viewNames.length > 0 ) {
            viewsComboBox = new JComboBox<>( viewNames );
            viewsComboBox.setSelectedItem( viewNames[0] );
        } else {
            viewsComboBox = new JComboBox<>( new String[] {""} );
            viewsComboBox.setSelectedItem( "" );
        }

        setComboBoxDimensions(viewsComboBox);
        viewsComboBox.setPrototypeDisplayValue( UserInterfaceHelper.PROTOTYPE_DISPLAY_VALUE);
    }

    public static void setComboBoxDimensions( JComboBox< String > comboBox )
    {
        comboBox.setPrototypeDisplayValue( UserInterfaceHelper.PROTOTYPE_DISPLAY_VALUE );
        comboBox.setPreferredSize( new Dimension( 350, 20 ) );
        comboBox.setMaximumSize( new Dimension( 350, 20 ) );
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

    private void addSourcesPanel() {
        final JPanel horizontalLayoutPanel = SwingUtils.horizontalLayoutPanel();

        final JButton addButton = SwingHelper.createButton( "Add" );
        // for now we don't support editing any image properties, but this is likely to change in future,
        // so keep this code for now
        // final JButton editButton = createButton("Edit");

        createSoucesComboBox();
        addButton.addActionListener( e ->
        {
            new Thread( () -> { addImageDialog(); } ).start();
        } );

        // editButton.addActionListener( e ->
        // {
        //     new Thread( () -> { editImageDialog(); } ).start();
        // } );

        horizontalLayoutPanel.add( SwingHelper.getJLabel("source", 60, 10));
        horizontalLayoutPanel.add(sourcesComboBox);
        horizontalLayoutPanel.add( addButton );
        horizontalLayoutPanel.add( Box.createHorizontalStrut( SwingHelper.BUTTON_DIMENSION.width ) );
        // horizontalLayoutPanel.add( editButton );
        horizontalLayoutPanel.setAlignmentX( Component.LEFT_ALIGNMENT );

        this.getContentPane().add(horizontalLayoutPanel);
    }

    private void addViewsPanel() {
        final JPanel groupPanel = SwingUtils.horizontalLayoutPanel();
        final JPanel viewsPanel = SwingUtils.horizontalLayoutPanel();

        createGroupsCombobox();
        createViewsCombobox();

        groupPanel.add( SwingHelper.getJLabel("group", 60, 10));
        viewsPanel.add( SwingHelper.getJLabel("view", 60, 10));
        groupPanel.add( groupsComboBox );
        viewsPanel.add( viewsComboBox );
        groupPanel.add( Box.createHorizontalStrut( SwingHelper.BUTTON_DIMENSION.width ) );
        groupPanel.add( Box.createHorizontalStrut( SwingHelper.BUTTON_DIMENSION.width ) );
        viewsPanel.add( Box.createHorizontalStrut( SwingHelper.BUTTON_DIMENSION.width ) );
        viewsPanel.add( Box.createHorizontalStrut( SwingHelper.BUTTON_DIMENSION.width ) );
        groupPanel.setAlignmentX( Component.LEFT_ALIGNMENT );
        viewsPanel.setAlignmentX( Component.LEFT_ALIGNMENT );

        this.getContentPane().add( groupPanel );
        this.getContentPane().add( viewsPanel );
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
                openMoBIE.projectLocation = this.projectsCreator.getProjectLocation().getAbsolutePath();
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
        List <String > datasets = projectsCreator.getProject().datasets();
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
                projectsCreator.getRemoteMetadataCreator().createRemoteMetadata( signingRegion, serviceEndpoint, bucketName, format );
            }
        }
    }

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

    private String selectUiSelectionGroupDialog( String datasetName ) {

        String[] currentSelectionGroups = projectsCreator.getGroups( datasetName );

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
            projectsCreator.getDatasetsCreator().makeDataset2D( datasetName, false );
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

    public void addCurrentOpenImageDialog() {
        String datasetName = (String) datasetComboBox.getSelectedItem();

        if ( !datasetName.equals("") ) {
            ImagePlus currentImage = IJ.getImage();

            if ( !isImageValid( currentImage.getNChannels(), currentImage.getCalibration().getUnit(),
                    projectsCreator.getVoxelUnit(), false ) ) {
                return;
            }

            if ( currentImage.getNDimensions() > 2 && projectsCreator.getDataset( datasetName ).is2D() ) {
                if ( !changeDatasetDimensionDialog(datasetName) ) {
                    return;
                }
            }

            final GenericDialog gd = new GenericDialog( "Add Current Image To MoBIE Project..." );
            gd.addMessage( "Make sure your voxel size, and unit,\n are set properly under Image > Properties...");
            gd.addStringField( "Image Name", FilenameUtils.removeExtension(currentImage.getTitle()), 35 );
            gd.addChoice( "Image Type", imageTypes, imageType.toString() );
            gd.addChoice( "Image format", imageFormats, imageDataFormat.toString() );
            gd.addCheckbox("Use default export settings", useDefaultExportSettings );
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
                imageDataFormat = ImageDataFormat.fromString( gd.getNextChoice() );
                useDefaultExportSettings = gd.getNextBoolean();
                exclusive = gd.getNextBoolean();

                String affineRow1 = gd.getNextString().trim();
                String affineRow2 = gd.getNextString().trim();
                String affineRow3 = gd.getNextString().trim();
                String affineTransform = String.join(",", affineRow1, affineRow2, affineRow3 );

                // tidy up image name, remove any spaces
                imageName = UserInterfaceHelper.tidyString( imageName );
                AffineTransform3D sourceTransform = ProjectCreatorHelper.parseAffineString( affineTransform );

                if ( imageName != null && sourceTransform != null ) {
                    ImagesCreator imagesCreator = projectsCreator.getImagesCreator();

                    boolean overwriteImage = true;
                    if ( imagesCreator.imageExists( datasetName, imageName, imageDataFormat ) ) {
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

                    try {
                        if ( useDefaultExportSettings ) {
                            imagesCreator.addImage(currentImage, imageName, datasetName, imageDataFormat,
                                    imageType, sourceTransform, uiSelectionGroup, exclusive);
                            updateComboBoxesForNewImage(imageName, uiSelectionGroup);
                        } else {
                            ManualExportPanel manualExportPanel = new ManualExportPanel( imageDataFormat );
                            int[][] resolutions = manualExportPanel.getResolutions();
                            int[][] subdivisions = manualExportPanel.getSubdivisions();
                            Compression compression = manualExportPanel.getCompression();

                            if ( resolutions != null && subdivisions != null && compression != null ) {
                                imagesCreator.addImage( currentImage, imageName, datasetName, imageDataFormat, imageType,
                                        sourceTransform, uiSelectionGroup, exclusive, resolutions, subdivisions,
                                        compression );
                                updateComboBoxesForNewImage( imageName, uiSelectionGroup );
                            }
                        }
                    } catch (SpimDataException | IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        } else {
            IJ.log( "Add image failed - create a dataset first" );
        }
    }

    public void addBdvFormatImageDialog() {
        String datasetName = (String) datasetComboBox.getSelectedItem();

        if (!datasetName.equals(""))
        {
            final GenericDialog gd = new GenericDialog( "Add Bdv Format Image To Project..." );

            gd.addMessage( "Note: You can only 'link' to images outside the project folder \n" +
                    " for local projects. 'copy' or 'move' if you wish to upload to s3" );

            gd.addChoice( "Image format", imageFormats, imageDataFormat.toString() );
            String[] addMethods = new String[]{ ProjectCreator.AddMethod.link.toString(),
                    ProjectCreator.AddMethod.copy.toString(), ProjectCreator.AddMethod.move.toString() };
            gd.addChoice( "Add method:", addMethods, addMethod.toString() );
            gd.addChoice( "Image Type", imageTypes, imageType.toString() );
            gd.addCheckbox( "Make view exclusive", exclusive );
            gd.addCheckbox( "Use filename as image name", useFileNameAsImageName );

            gd.showDialog();

            if ( gd.wasCanceled() )
            {
                IJ.log( "Add image failed - create a dataset first" );
                return;
            }

            imageDataFormat = ImageDataFormat.fromString( gd.getNextChoice() );
            addMethod = ProjectCreator.AddMethod.valueOf( gd.getNextChoice() );
            imageType = ProjectCreator.ImageType.valueOf( gd.getNextChoice() );
            exclusive = gd.getNextBoolean();
            useFileNameAsImageName = gd.getNextBoolean();

            if ( imageDataFormat == ImageDataFormat.OmeZarr && addMethod == ProjectCreator.AddMethod.link )
            {
                IJ.log( "link is currently unsupported for ome-zarr. Please choose copy or move instead for this file format." );
                return;
            }

            String filePath = getFilePath();

            if ( filePath != null )
            {
                try
                {
                    addBdvFile( filePath, datasetName );
                } catch ( SpimDataException e )
                {
                    e.printStackTrace();
                }
            }
        }
    }

    private String getFilePath()
    {
        switch ( imageDataFormat )
        {
            case BdvN5:
                return UserInterfaceHelper.selectFilePath( "xml", "bdv .xml file", true );

            case OmeZarr:
                String filePath = UserInterfaceHelper.selectDirectoryPath( ".ome.zarr file", true );

                // quick check that basic criteria for ome-zarr are met i.e. contains right files in top of dir
                if ( ! isValidOMEZarr( filePath ) )
                {
                    IJ.log( "Add image failed - not a valid ome.zarr file." );
                    return null;
                }
                else
                {
                    return filePath;
                }
            default:
                return null;
        }
    }

    private boolean isValidOMEZarr( String filePath )
    {
        return ( new File( IOHelper.combinePath( filePath, ".zgroup" ) ).exists() && new File( IOHelper.combinePath( filePath, ".zattrs" ) ).exists() );
    }

    private void addBdvFile( String filePath, String datasetName ) throws SpimDataException {
        SpimData spimData = ( SpimData ) new SpimDataOpener().open( filePath, imageDataFormat );

        int nChannels = spimData.getSequenceDescription().getViewSetupsOrdered().size();
        String imageUnit = spimData.getSequenceDescription().getViewSetupsOrdered().get(0).getVoxelSize().unit();

        if ( !isImageValid( nChannels, imageUnit, projectsCreator.getVoxelUnit(), true ) ) {
            return;
        }

        if ( !isSpimData2D(spimData) && projectsCreator.getDataset( datasetName ).is2D() ) {
            if ( !changeDatasetDimensionDialog(datasetName) ) {
                return;
            }
        }

        File imageFile = new File( filePath );
        String imageName = imageFile.getName().split("\\.")[0];
        if ( !useFileNameAsImageName ) {
            imageName = imageNameDialog( imageFile );
            if ( imageName == null ) {
                return;
            }
        }

        ImagesCreator imagesCreator = projectsCreator.getImagesCreator();
        boolean overwriteImage = true;
        if ( imagesCreator.imageExists( datasetName, imageName, imageDataFormat ) ) {
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

        try {
            imagesCreator.addBdvFormatImage(spimData, imageName, datasetName, imageType,
                    addMethod, uiSelectionGroup, imageDataFormat, exclusive);
            updateComboBoxesForNewImage(imageName, uiSelectionGroup);
        } catch (SpimDataException | IOException e) {
            e.printStackTrace();
        }
    }

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
                projectsCreator.getDatasetsCreator().addDataset( datasetName, is2D );
                updateDatasetsComboBox( datasetName );
            }
        }
    }

    private void editDatasetDialog() {
        final GenericDialog gd = new GenericDialog( "Edit dataset..." );
        String oldName = (String) datasetComboBox.getSelectedItem();
        if ( !oldName.equals("") ) {
            boolean isDefault = projectsCreator.getProject().getDefaultDataset().equals(oldName);

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
                    projectsCreator.getDatasetsCreator().renameDataset( oldName, newName );
                    updateDatasetsComboBox( newName );
                }

                if (!isDefault) {
                    boolean makeDefault = gd.getNextBoolean();
                    if (makeDefault) {
                        projectsCreator.getDatasetsCreator().makeDefaultDataset( newName );
                    }
                }

            }
        }
    }

    private void updateDatasetsComboBox( String selection ) {
        datasetComboBox.removeAllItems();
        for ( String datasetName : projectsCreator.getProject().datasets() ) {
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
            Dataset dataset = projectsCreator.getDataset( currentDataset );
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
            String[] groups = projectsCreator.getGroups( currentDataset );
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
            String[] views = projectsCreator.getViews( currentDataset, currentGroup );
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
