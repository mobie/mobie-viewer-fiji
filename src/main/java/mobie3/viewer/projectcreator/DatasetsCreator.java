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
package mobie3.viewer.projectcreator;

import ij.IJ;
import org.embl.mobie.io.util.IOHelper;

import java.io.File;
import java.util.List;

public class DatasetsCreator {

    static { net.imagej.patcher.LegacyInjector.preinit(); }

    private ProjectCreator projectCreator;

    /**
     * Make a datasetsCreator - includes all functions for creating and modifying datasets in projects
     * @param projectCreator projectCreator
     */
    public DatasetsCreator( ProjectCreator projectCreator ) {
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
            File datasetDir = new File ( IOHelper.combinePath( projectCreator.getProjectLocation().getAbsolutePath(),
                    datasetName ) );

            if ( !datasetDir.exists() ) {
                datasetDir.mkdirs();

                // make rest of folders required under dataset
                new File(datasetDir, "images").mkdirs();
                new File(datasetDir, "misc").mkdirs();
                new File(datasetDir, "tables").mkdirs();
                new File(IOHelper.combinePath(datasetDir.getAbsolutePath(), "misc", "views")).mkdirs();

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
                File oldDatasetDir = new File( IOHelper.combinePath(
                        projectCreator.getProjectLocation().getAbsolutePath(), oldName) );
                File newDatasetDir = new File( IOHelper.combinePath(
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
