package de.embl.cba.mobie.projects;

import de.embl.cba.mobie.image.ImagePropertiesEditor;
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

    public void addCurrentOpenImageDialog() {
        String datasetName = (String) datasetComboBox.getSelectedItem();

        if (!datasetName.equals("")) {

            final GenericDialog gd = new GenericDialog( "Add Current Image To MoBIE Project..." );
            gd.addMessage( "Make sure your pixel size, and unit,\n are set properly under Image > Properties...");
            gd.addStringField( "Image Name", "" );
            String[] imageTypes = new String[] {"image", "segmentation", "mask"};
            gd.addChoice( "Image Type", imageTypes, "image" );
            String[] bdvFormats = new String[] {"n5", "h5"};
            gd.addChoice( "Bdv format", bdvFormats, "n5" );

            // TODO - add a way to add existing bdv xml files also e.g. link to current location, copy to project, move to project
            gd.showDialog();

            if ( !gd.wasCanceled() ) {
                String imageName = gd.getNextString();
                String imageType = gd.getNextChoice();
                String bdvFormat = gd.getNextChoice();
                projectsCreator.addImage( imageName, datasetName, bdvFormat, imageType );
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
                String[] addMethods = new String[]{"link to current image location", "copy image"};
                gd.addChoice("Add method:", addMethods, "link to current image location");
                String[] imageTypes = new String[]{"image", "segmentation", "mask"};
                gd.addChoice("Image Type", imageTypes, "image");
                String[] bdvFormats = new String[]{"n5", "h5"};
                gd.addChoice("Bdv format", bdvFormats, "n5");

                // TODO - add a way to add existing bdv xml files also e.g. link to current location, copy to project, move to project
                gd.showDialog();

                if (!gd.wasCanceled()) {
                    String addMethod = gd.getNextChoice();
                    String imageType = gd.getNextChoice();
                    String bdvFormat = gd.getNextChoice();

                    try {
                        projectsCreator.addBdvFormatImage( xmlLocation, datasetName, bdvFormat, imageType, addMethod );
                    } catch (SpimDataException | IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            Utils.log( "Add image failed - create a dataset first" );
        }

        // TODO - add possiblity to move image too (if don't want to copy it)
    }

    public String addDatasetDialog () {
        final GenericDialog gd = new GenericDialog( "Create a new dataset" );

        gd.addStringField( "Name of dataset", "");
        gd.showDialog();

        if ( !gd.wasCanceled() ) {
            String datasetName = gd.getNextString();
            // check not already in datasets
            boolean contains = projectsCreator.isInDatasets( datasetName );
            if ( !contains ) {
                projectsCreator.addDataset( datasetName );
                updateDatasetsComboBox( datasetName );
            }

            return datasetName;

        } else {
            return null;
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
                String newName = gd.getNextString().trim();
                if (!newName.equals(oldName)) {
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
