package de.embl.cba.mobie.projects;

import bdv.tools.brightness.ConverterSetup;
import bdv.tools.brightness.SliderPanelDouble;
import bdv.util.BoundedValueDouble;
import de.embl.cba.bdv.utils.BrightnessUpdateListener;
import de.embl.cba.mobie.dataset.DatasetsParser;
import de.embl.cba.mobie.image.ImageProperties;
import de.embl.cba.mobie.image.ImagePropertiesEditor;
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
import net.imglib2.ops.parse.token.Int;
import net.imglib2.type.numeric.ARGBType;
import org.apache.commons.compress.utils.FileNameUtils;
import org.apache.commons.lang.WordUtils;
import voltex.Mask;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.MaskFormatter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

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
        addButton.addActionListener(e -> addDatasetDialog());
        editButton.addActionListener(e -> editDatasetDialog());

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

        final JButton editButton = getButton("Edit");

        imagesComboBox = new JComboBox<>( projectsCreator.getCurrentImages( (String) datasetComboBox.getSelectedItem()) );
        setComboBoxDimensions(imagesComboBox);
        editButton.addActionListener(e -> editImageDialog() );
        imagesComboBox.setPrototypeDisplayValue(MoBIEViewer.PROTOTYPE_DISPLAY_VALUE);

        horizontalLayoutPanel.add(getJLabel("image", 60, 10));
        horizontalLayoutPanel.add(imagesComboBox);
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
        boolean isDefault = projectsCreator.isDefaultDataset( oldName );

        gd.addStringField( "Dataset name", oldName);
        if ( isDefault ) {
            gd.addMessage( "This dataset is the default");
        } else {
            gd.addCheckbox("Make default dataset", false);
        }
        gd.showDialog();

        if ( !gd.wasCanceled() ) {
            String newName = gd.getNextString().trim();
            if ( !newName.equals(oldName) ) {
                // check not already in datasets
                boolean contains = projectsCreator.isInDatasets(newName);
                if (!contains) {
                    projectsCreator.renameDataset(oldName, newName);
                    updateDatasetsComboBox( newName );
                }
            }

            if ( !isDefault ) {
                boolean makeDefault = gd.getNextBoolean();
                if ( makeDefault ) {
                    projectsCreator.makeDefaultDataset( newName );
                }
            }

        }
    }

    private void editImageDialog() {
        new ImagePropertiesEditor( (String) datasetComboBox.getSelectedItem(),
                (String) imagesComboBox.getSelectedItem(), projectsCreator);
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
