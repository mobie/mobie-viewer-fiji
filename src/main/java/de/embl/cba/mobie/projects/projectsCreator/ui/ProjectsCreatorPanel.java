package de.embl.cba.mobie.projects.projectsCreator.ui;

import de.embl.cba.mobie.projects.projectsCreator.Project;
import de.embl.cba.mobie.projects.projectsCreator.ProjectsCreator;
import de.embl.cba.mobie.ui.MoBIE;
import de.embl.cba.mobie.utils.Utils;
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

import static de.embl.cba.mobie.utils.ExportUtils.generateDefaultAffine;
import static de.embl.cba.mobie.utils.ExportUtils.parseAffineString;
import static de.embl.cba.mobie.utils.Utils.tidyString;
import static de.embl.cba.mobie.utils.ui.SwingUtils.*;

public class ProjectsCreatorPanel extends JFrame {
    private ProjectsCreator projectsCreator;
    private Project project;
    private JComboBox<String> datasetComboBox;
    private JComboBox<String> imagesComboBox;

    public ProjectsCreatorPanel ( File projectLocation ) {
        this.projectsCreator = new ProjectsCreator( projectLocation );
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
        String[] datasetNames = project.getDatasetNames();
        datasetComboBox = new JComboBox<>( datasetNames );
        datasetComboBox.setSelectedItem( datasetNames[0] );
        setComboBoxDimensions(datasetComboBox);
        datasetComboBox.setPrototypeDisplayValue( MoBIE.PROTOTYPE_DISPLAY_VALUE);
        datasetComboBox.addActionListener( new SyncImageAndDatasetComboBox() );
    }

    private void createImagesCombobox() {
        String selectedDataset = (String) datasetComboBox.getSelectedItem();
        if ( !selectedDataset.equals("") ) {
            String[] imageNames = project.getDataset( selectedDataset ).getImageNames();
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

        final JButton addButton = getButton( "Add" );
        final JButton editButton = getButton("Edit");

        createImagesCombobox();
        addButton.addActionListener( e ->
        {
            new Thread( () -> { addImageDialog(); } ).start();
        } );

        editButton.addActionListener( e ->
        {
            new Thread( () -> { editImageDialog(); } ).start();
        } );

        horizontalLayoutPanel.add(getJLabel("image", 60, 10));
        horizontalLayoutPanel.add(imagesComboBox);
        horizontalLayoutPanel.add( addButton );
        horizontalLayoutPanel.add( editButton );
        horizontalLayoutPanel.setAlignmentX( Component.LEFT_ALIGNMENT );

        this.getContentPane().add(horizontalLayoutPanel);
    }

    public String chooseDatasetDialog() {
        final GenericDialog gd = new GenericDialog( "Choose a dataset" );
        String[] currentDatasets = project.getDatasetNames();
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

    public void addCurrentOpenImageDialog() {
        String datasetName = (String) datasetComboBox.getSelectedItem();

        if ( !datasetName.equals("") ) {
            ImagePlus currentImage = IJ.getImage();
            String defaultAffineTransform = generateDefaultAffine( currentImage );

            final GenericDialog gd = new GenericDialog( "Add Current Image To MoBIE Project..." );
            gd.addMessage( "Make sure your pixel size, and unit,\n are set properly under Image > Properties...");
            gd.addStringField( "Image Name", "", 35 );
            String[] imageTypes = new String[]{ ProjectsCreator.ImageType.image.toString(),
                    ProjectsCreator.ImageType.segmentation.toString(), ProjectsCreator.ImageType.mask.toString() };
            gd.addChoice( "Image Type", imageTypes, imageTypes[0] );
            // TODO - add OME.ZARR
            String[] bdvFormats = new String[]{ ProjectsCreator.BdvFormat.n5.toString() };
            gd.addChoice( "Bdv format", bdvFormats, bdvFormats[0] );
            gd.addStringField("Affine", defaultAffineTransform, 35 );
            gd.addCheckbox("Use default export settings", true);

            gd.showDialog();

            if ( !gd.wasCanceled() ) {
                String imageName = gd.getNextString();
                ProjectsCreator.ImageType imageType = ProjectsCreator.ImageType.valueOf( gd.getNextChoice() );
                ProjectsCreator.BdvFormat bdvFormat = ProjectsCreator.BdvFormat.valueOf( gd.getNextChoice() );
                String affineTransform = gd.getNextString().trim();
                boolean useDefaultSettings = gd.getNextBoolean();

                // tidy up image name, remove any spaces
                imageName = tidyString( imageName );

                AffineTransform3D sourceTransform = parseAffineString( affineTransform );

                if ( imageName != null && sourceTransform != null ) {
                    projectsCreator.getImagesCreator().addImage( currentImage, imageName,
                            datasetName, bdvFormat, imageType, sourceTransform, useDefaultSettings );
                    updateImagesComboBox( imageName );
                }
            }

        } else {
            Utils.log( "Add image failed - create a dataset first" );
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
                String[] addMethods = new String[]{ ProjectsCreator.AddMethod.link.toString(),
                        ProjectsCreator.AddMethod.copy.toString(), ProjectsCreator.AddMethod.move.toString() };
                gd.addChoice("Add method:", addMethods, addMethods[0]);
                String[] imageTypes = new String[]{ ProjectsCreator.ImageType.image.toString(),
                        ProjectsCreator.ImageType.segmentation.toString(), ProjectsCreator.ImageType.mask.toString() };
                gd.addChoice("Image Type", imageTypes, imageTypes[0]);
                gd.addMessage( "Note: You can only 'link' to images outside \n" +
                        "the project folder for local projects. \n " +
                        "'copy' or 'move' if you wish to upload to s3");

                gd.showDialog();

                if (!gd.wasCanceled()) {
                    ProjectsCreator.AddMethod addMethod = ProjectsCreator.AddMethod.valueOf( gd.getNextChoice() );
                    ProjectsCreator.ImageType imageType = ProjectsCreator.ImageType.valueOf( gd.getNextChoice() );

                    try {
                        projectsCreator.getImagesCreator().addBdvFormatImage( xmlLocation, datasetName, imageType, addMethod );
                    } catch (SpimDataException | IOException e) {
                        e.printStackTrace();
                    }

                    String imageName = FileNameUtils.getBaseName(xmlLocation.getAbsolutePath());
                    updateImagesComboBox( imageName );
                }
            }
        } else {
            Utils.log( "Add image failed - create a dataset first" );
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
            boolean isDefault = project.isDefaultDataset(oldName);

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

    private void editImageDialog() {
        String datasetName = (String) datasetComboBox.getSelectedItem();
        String imageName = (String) imagesComboBox.getSelectedItem();

        if ( !datasetName.equals("") && !imageName.equals("") ) {
            new ImagePropertiesEditor(datasetName,
                    imageName, projectsCreator);
        }
    }

    private void updateDatasetsComboBox( String selection ) {
        updateDatasetsComboBox();
        datasetComboBox.setSelectedItem( selection );
    }

    private void updateDatasetsComboBox () {
        datasetComboBox.removeAllItems();
        for ( String datasetName : project.getDatasetNames() ) {
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
            for ( String imageName : project.getDataset(currentDataset).getImageNames() ) {
                imagesComboBox.addItem(imageName);
            }
        }
    }


}
