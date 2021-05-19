package de.embl.cba.mobie2.projectcreator.ui;

import de.embl.cba.mobie.ui.MoBIE;
import de.embl.cba.mobie2.Project;
import de.embl.cba.mobie2.projectcreator.ProjectCreator;
import de.embl.cba.tables.FileAndUrlUtils;
import de.embl.cba.tables.SwingUtils;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import mpicbg.spim.data.SpimDataException;
import net.imglib2.realtransform.AffineTransform3D;
import org.apache.commons.compress.utils.FileNameUtils;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import static de.embl.cba.mobie2.projectcreator.ProjectCreatorHelper.generateDefaultAffine;
import static de.embl.cba.mobie2.projectcreator.ProjectCreatorHelper.parseAffineString;
import static de.embl.cba.mobie2.ui.SwingHelper.createButton;
import static de.embl.cba.mobie2.ui.SwingHelper.getJLabel;
import static de.embl.cba.mobie2.ui.UserInterfaceHelper.tidyString;

public class ProjectsCreatorPanel extends JFrame {
    private ProjectCreator projectsCreator;
    private Project project;
    private JComboBox<String> datasetComboBox;
    private JComboBox<String> imagesComboBox;

    public ProjectsCreatorPanel ( File projectLocation ) throws IOException {

        // account for projects with and without the top 'data' directory
        String dataDirectoryPath = FileAndUrlUtils.combinePath(  projectLocation.getAbsolutePath(), "data");
        File dataDirectory = new File( dataDirectoryPath );
        if (!dataDirectory.exists() ) {
            this.projectsCreator = new ProjectCreator(projectLocation);
        } else {
            this.projectsCreator = new ProjectCreator(dataDirectory);
        }

        this.project = projectsCreator.getProject();
        addDatasetPanel();
        addImagesPanel();
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

        final JButton addButton = createButton("Add");
        final JButton editButton = createButton("Edit");

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

    private String[] getDatasetNames() {
        String[] datasetNames = new String[ project.getDatasets().size() ];
        for ( int i = 0; i<project.getDatasets().size(); i++ ) {
            datasetNames[i] = project.getDatasets().get(i);
        }
        return datasetNames;
    }

    private void createDatasetComboBox() {
        String[] datasetNames = getDatasetNames();
        datasetComboBox = new JComboBox<>( datasetNames );
        datasetComboBox.setSelectedItem( datasetNames[0] );
        setComboBoxDimensions(datasetComboBox);
        datasetComboBox.setPrototypeDisplayValue( MoBIE.PROTOTYPE_DISPLAY_VALUE);
        datasetComboBox.addActionListener( new SyncImageAndDatasetComboBox() );
    }

    private void createImagesCombobox() {
        String selectedDataset = (String) datasetComboBox.getSelectedItem();
        if ( !selectedDataset.equals("") ) {
            String[] imageNames = projectsCreator.getDataset( selectedDataset ).sources.keySet().toArray(new String[0]);
            imagesComboBox = new JComboBox<>( imageNames );
            imagesComboBox.setSelectedItem( imageNames[0] );
        } else {
            imagesComboBox = new JComboBox<>( new String[] {""} );
            imagesComboBox.setSelectedItem( "" );
        }
        setComboBoxDimensions(imagesComboBox);
        imagesComboBox.setPrototypeDisplayValue( MoBIE.PROTOTYPE_DISPLAY_VALUE);
    }

    public static void setComboBoxDimensions( JComboBox< String > comboBox )
    {
        comboBox.setPrototypeDisplayValue( MoBIE.PROTOTYPE_DISPLAY_VALUE );
        comboBox.setPreferredSize( new Dimension( 350, 20 ) );
        comboBox.setMaximumSize( new Dimension( 350, 20 ) );
    }

    private class SyncImageAndDatasetComboBox implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            updateImagesComboBox();
        }
    }

    private void addImagesPanel() {
        final JPanel horizontalLayoutPanel = SwingUtils.horizontalLayoutPanel();

        final JButton addButton = createButton( "Add" );
        // final JButton editButton = createButton("Edit");

        createImagesCombobox();
        addButton.addActionListener( e ->
        {
            new Thread( () -> { addImageDialog(); } ).start();
        } );

        // editButton.addActionListener( e ->
        // {
        //     new Thread( () -> { editImageDialog(); } ).start();
        // } );

        horizontalLayoutPanel.add(getJLabel("image", 60, 10));
        horizontalLayoutPanel.add(imagesComboBox);
        horizontalLayoutPanel.add( addButton );
        // horizontalLayoutPanel.add( editButton );
        horizontalLayoutPanel.setAlignmentX( Component.LEFT_ALIGNMENT );

        this.getContentPane().add(horizontalLayoutPanel);
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

    private String selectUiSelectionGroupDialog() {
        final GenericDialog gd = new GenericDialog( "Ui selection group for image view..." );
        gd.addStringField( "Ui selection group", "", 35 );
        gd.showDialog();

        if ( !gd.wasCanceled() ) {
            return gd.getNextString();
        } else {
            return null;
        }
    }

    public void addCurrentOpenImageDialog() {
        String datasetName = (String) datasetComboBox.getSelectedItem();

        if ( !datasetName.equals("") ) {
            ImagePlus currentImage = IJ.getImage();
            String defaultAffineTransform = generateDefaultAffine( currentImage );

            final GenericDialog gd = new GenericDialog( "Add Current Image To MoBIE Project..." );
            gd.addMessage( "Make sure your pixel size, and unit,\n are set properly under Image > Properties...");
            gd.addStringField( "Image Name", "", 35 );
            String[] imageTypes = new String[]{ ProjectCreator.ImageType.image.toString(),
                    ProjectCreator.ImageType.segmentation.toString() };
            gd.addChoice( "Image Type", imageTypes, imageTypes[0] );
            // TODO - add OME.ZARR
            String[] bdvFormats = new String[]{ ProjectCreator.BdvFormat.n5.toString() };
            gd.addChoice( "Bdv format", bdvFormats, bdvFormats[0] );
            gd.addStringField("Affine", defaultAffineTransform, 35 );
            gd.addCheckbox("Use default export settings", true);
            gd.addCheckbox( "Create view for this image", true );

            gd.showDialog();

            if ( !gd.wasCanceled() ) {
                String imageName = gd.getNextString();
                ProjectCreator.ImageType imageType = ProjectCreator.ImageType.valueOf( gd.getNextChoice() );
                ProjectCreator.BdvFormat bdvFormat = ProjectCreator.BdvFormat.valueOf( gd.getNextChoice() );
                String affineTransform = gd.getNextString().trim();
                boolean useDefaultSettings = gd.getNextBoolean();
                boolean createView = gd.getNextBoolean();

                // tidy up image name, remove any spaces
                imageName = tidyString( imageName );

                AffineTransform3D sourceTransform = parseAffineString( affineTransform );

                if ( imageName != null && sourceTransform != null ) {
                    String uiSelectionGroup = null;
                    if ( createView ) {
                        uiSelectionGroup = selectUiSelectionGroupDialog();
                        uiSelectionGroup = tidyString( uiSelectionGroup );
                        if ( uiSelectionGroup != null ) {
                            projectsCreator.getImagesCreator().addImage( currentImage, imageName,
                                    datasetName, bdvFormat, imageType, sourceTransform, useDefaultSettings, uiSelectionGroup );
                        }
                    } else {
                        projectsCreator.getImagesCreator().addImage( currentImage, imageName,
                                datasetName, bdvFormat, imageType, sourceTransform, useDefaultSettings, uiSelectionGroup );
                    }

                    updateImagesComboBox( imageName );
                }
            }

        } else {
            IJ.log( "Add image failed - create a dataset first" );
        }
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
                String[] addMethods = new String[]{ ProjectCreator.AddMethod.link.toString(),
                        ProjectCreator.AddMethod.copy.toString(), ProjectCreator.AddMethod.move.toString() };
                gd.addChoice("Add method:", addMethods, addMethods[0]);
                String[] imageTypes = new String[]{ ProjectCreator.ImageType.image.toString(),
                        ProjectCreator.ImageType.segmentation.toString() };
                gd.addChoice("Image Type", imageTypes, imageTypes[0]);
                gd.addCheckbox( "Create view for this image", true );
                gd.addMessage( "Note: You can only 'link' to images outside \n" +
                        "the project folder for local projects. \n " +
                        "'copy' or 'move' if you wish to upload to s3");

                gd.showDialog();

                if (!gd.wasCanceled()) {
                    ProjectCreator.AddMethod addMethod = ProjectCreator.AddMethod.valueOf( gd.getNextChoice() );
                    ProjectCreator.ImageType imageType = ProjectCreator.ImageType.valueOf( gd.getNextChoice() );
                    boolean createView = gd.getNextBoolean();

                    try {
                        String uiSelectionGroup = null;
                        if ( createView ) {
                            uiSelectionGroup = selectUiSelectionGroupDialog();
                            uiSelectionGroup = tidyString( uiSelectionGroup );
                            if ( uiSelectionGroup != null ) {
                                projectsCreator.getImagesCreator().addBdvFormatImage( xmlLocation, datasetName, imageType,
                                        addMethod, uiSelectionGroup );
                            }
                        } else {
                            projectsCreator.getImagesCreator().addBdvFormatImage( xmlLocation, datasetName, imageType,
                                    addMethod, uiSelectionGroup );
                        }
                    } catch (SpimDataException | IOException e) {
                        e.printStackTrace();
                    }

                    String imageName = FileNameUtils.getBaseName(xmlLocation.getAbsolutePath());
                    updateImagesComboBox( imageName );
                }
            }
        } else {
            IJ.log( "Add image failed - create a dataset first" );
        }
    }

    public void addDatasetDialog () {
        final GenericDialog gd = new GenericDialog( "Create a new dataset" );
        gd.addStringField( "Name of dataset", "", 35 );
        gd.showDialog();

        if ( !gd.wasCanceled() ) {
            String datasetName = gd.getNextString();
            datasetName = tidyString( datasetName );

            if ( datasetName != null ) {
                projectsCreator.getDatasetsCreator().addDataset(datasetName);
                updateDatasetsComboBox( datasetName );
            }
        }
    }

    private void editDatasetDialog() {
        final GenericDialog gd = new GenericDialog( "Edit dataset..." );
        String oldName = (String) datasetComboBox.getSelectedItem();
        if ( !oldName.equals("") ) {
            boolean isDefault = project.getDefaultDataset().equals(oldName);

            gd.addStringField("Dataset name", oldName, 35);
            if (isDefault) {
                gd.addMessage("     This dataset is the default");
            } else {
                gd.addCheckbox("Make default dataset", false);
            }
            gd.showDialog();

            if (!gd.wasCanceled()) {
                String newName = gd.getNextString();
                newName = tidyString( newName );
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
        updateDatasetsComboBox();
        datasetComboBox.setSelectedItem( selection );
    }

    private void updateDatasetsComboBox () {
        datasetComboBox.removeAllItems();
        for ( String datasetName : project.getDatasets() ) {
            datasetComboBox.addItem(datasetName);
        }

        updateImagesComboBox();
    }

    private void updateImagesComboBox( String selection ) {
        updateImagesComboBox();
        imagesComboBox.setSelectedItem( selection );
    }

    private void updateImagesComboBox () {
        String currentDataset = (String) datasetComboBox.getSelectedItem();

        if ( currentDataset != null && !currentDataset.equals("") ) {
            imagesComboBox.removeAllItems();
            for ( String imageName : projectsCreator.getDataset(currentDataset).sources.keySet() ) {
                imagesComboBox.addItem(imageName);
            }
        }
    }


}
