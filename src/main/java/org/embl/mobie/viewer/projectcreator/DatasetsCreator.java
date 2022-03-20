package org.embl.mobie.viewer.projectcreator;

import org.embl.mobie.io.util.FileAndUrlUtils;
import ij.IJ;

import java.io.File;
import java.util.List;

public class DatasetsCreator {

    static { net.imagej.patcher.LegacyInjector.preinit(); }

    private ProjectCreator projectCreator;

    /**
     * Make a datasetsCreator - includes all functions for creating and modifying datasets in projects
     * @param projectCreator projectCreator
     */
    public DatasetsCreator(ProjectCreator projectCreator ) {
        this.projectCreator = projectCreator;
    }

    /**
     * Create a new, empty dataset
     * @param datasetName dataset name
     * @param is2D whether dataset only contains 2D images
     */
    public void addDataset ( String datasetName, boolean is2D ) {
        List<String> currentDatasets = projectCreator.getProject().getDatasets();
        boolean contains = currentDatasets.contains(datasetName);
        if ( !contains ) {
            File datasetDir = new File ( FileAndUrlUtils.combinePath( projectCreator.getProjectLocation().getAbsolutePath(),
                    datasetName ) );

            if ( !datasetDir.exists() ) {
                datasetDir.mkdirs();

                // make rest of folders required under dataset
                new File(datasetDir, "images").mkdirs();
                new File(datasetDir, "misc").mkdirs();
                new File(datasetDir, "tables").mkdirs();
                new File(FileAndUrlUtils.combinePath(datasetDir.getAbsolutePath(), "misc", "views")).mkdirs();

                // update project json
                projectCreator.getProjectJsonCreator().addDataset( datasetName );

                // create dataset json
                projectCreator.getDatasetJsonCreator().addDataset( datasetName, is2D );
            } else {
                IJ.log( "Dataset creation failed - this name already exists" );
            }
        } else {
            IJ.log("Add dataset failed - dataset already exists");
        }
    }

    /**
     * Rename an existing dataset
     * @param oldName old dataset name
     * @param newName new dataset name
     */
    public void renameDataset( String oldName, String newName ) {

        if ( !newName.equals(oldName) ) {
            // check not already in datasets
            List<String> currentDatasets = projectCreator.getProject().getDatasets();
            boolean contains = currentDatasets.contains( newName );
            if ( !contains ) {
                File oldDatasetDir = new File( FileAndUrlUtils.combinePath(
                        projectCreator.getProjectLocation().getAbsolutePath(), oldName) );
                File newDatasetDir = new File( FileAndUrlUtils.combinePath(
                        projectCreator.getProjectLocation().getAbsolutePath(), newName ));

                if (oldDatasetDir.exists()) {
                    if (oldDatasetDir.renameTo(newDatasetDir)) {
                        // update project json
                        projectCreator.getProjectJsonCreator().renameDataset( oldName, newName );
                    } else {
                        IJ.log("Rename directory failed");
                    }
                } else {
                    IJ.log("Rename dataset failed - that dataset doesn't exist");
                }
            } else {
                IJ.log( "Rename dataset failed - there is already a dataset of that name" );
            }
        }

    }

    /**
     * Make the dataset the default when MoBIE is opened
     * @param datasetName dataset name
     */
    public void makeDefaultDataset ( String datasetName ) {
        projectCreator.getProjectJsonCreator().setDefaultDataset( datasetName );
    }


    /**
     * Make a dataset 2D or 3D
     * @param datasetName dataset name
     * @param is2D 2D or not
     */
    public void makeDataset2D( String datasetName, boolean is2D ) {
        projectCreator.getDatasetJsonCreator().makeDataset2D( datasetName, is2D );
    }

}
