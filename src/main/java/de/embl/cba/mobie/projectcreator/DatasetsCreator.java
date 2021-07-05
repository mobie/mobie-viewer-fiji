package de.embl.cba.mobie.projectcreator;

import de.embl.cba.mobie.serialize.ProjectJsonParser;
import de.embl.cba.mobie.source.ImageDataFormat;
import de.embl.cba.tables.FileAndUrlUtils;
import ij.IJ;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static de.embl.cba.mobie.projectcreator.ProjectCreatorHelper.imageFormatToFolderName;

public class DatasetsCreator {

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
                new File(FileAndUrlUtils.combinePath(datasetDir.getAbsolutePath(), "images",
                        imageFormatToFolderName( ImageDataFormat.BdvN5 ))).mkdirs();
                new File(FileAndUrlUtils.combinePath(datasetDir.getAbsolutePath(), "images",
                        imageFormatToFolderName( ImageDataFormat.BdvN5S3 ))).mkdirs();
                new File(FileAndUrlUtils.combinePath(datasetDir.getAbsolutePath(), "misc", "views")).mkdirs();


                // if this is the first dataset, then make this the default
                if ( currentDatasets.size() == 0 ) {
                    projectCreator.getProject().setDefaultDataset( datasetName );
                }
                currentDatasets.add( datasetName );
                writeProjectJson();
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

                        // update json
                        if ( projectCreator.getProject().getDefaultDataset().equals(oldName) ) {
                            projectCreator.getProject().setDefaultDataset( newName );
                        }

                        int indexOld = projectCreator.getProject().getDatasets().indexOf(oldName);
                        projectCreator.getProject().getDatasets().set(indexOld, newName);

                        writeProjectJson();

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
        projectCreator.getProject().setDefaultDataset( datasetName );
        writeProjectJson();
    }

    private void writeProjectJson() {
        try {
            new ProjectJsonParser().saveProject( projectCreator.getProject(), projectCreator.getProjectJson().getAbsolutePath() );
        } catch (IOException e) {
            e.printStackTrace();
        }

        // whether the project writing succeeded or not, we now read the current state of the project
        try {
            projectCreator.reloadProject();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
