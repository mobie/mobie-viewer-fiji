package org.embl.mobie.viewer.projectcreator;

import org.embl.mobie.io.util.FileAndUrlUtils;
import ij.IJ;

import java.io.File;
import java.util.List;

public class DatasetsCreator {

    static { net.imagej.patcher.LegacyInjector.preinit(); }

    private ProjectCreator projectCreator;

    public DatasetsCreator(ProjectCreator projectCreator ) {
        this.projectCreator = projectCreator;
    }

    public void addDataset ( String datasetName ) {
        List<String> currentDatasets = projectCreator.getProject().getDatasets();
        boolean contains = currentDatasets.contains(datasetName);
        if ( !contains ) {
            File datasetDir = new File ( FileAndUrlUtils.combinePath( projectCreator.getDataLocation().getAbsolutePath(),
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
            } else {
                IJ.log( "Dataset creation failed - this name already exists" );
            }
        } else {
            IJ.log("Add dataset failed - dataset already exists");
        }
    }

    public void renameDataset( String oldName, String newName ) {

        if ( !newName.equals(oldName) ) {
            // check not already in datasets
            List<String> currentDatasets = projectCreator.getProject().getDatasets();
            boolean contains = currentDatasets.contains( newName );
            if ( !contains ) {
                File oldDatasetDir = new File( FileAndUrlUtils.combinePath(
                        projectCreator.getDataLocation().getAbsolutePath(), oldName) );
                File newDatasetDir = new File( FileAndUrlUtils.combinePath(
                        projectCreator.getDataLocation().getAbsolutePath(), newName ));

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

    public void makeDefaultDataset ( String datasetName ) {
        projectCreator.getProjectJsonCreator().setDefaultDataset( datasetName );
    }

}
