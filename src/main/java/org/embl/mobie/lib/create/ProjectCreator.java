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
package org.embl.mobie.lib.create;

import mpicbg.spim.data.SpimDataException;
import net.imagej.patcher.LegacyInjector;
import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.io.util.IOHelper;
import org.embl.mobie.lib.MoBIEHelper;
import org.embl.mobie.lib.serialize.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import static org.embl.mobie.lib.create.ProjectCreatorHelper.getGroupToViewsMap;

/**
 * Class to create and edit MoBIE projects.
 */
public class ProjectCreator {

    static { net.imagej.patcher.LegacyInjector.preinit(); }

    private final File projectLocation;
    private final File projectJson;
    private Project project;

    private File currentDatasetJson;
    private String currentDatasetName;
    private Dataset currentDataset;
    private Map<String, ArrayList<String> > currentGrouptoViews;
    private String voxelUnit;

    private final DatasetsCreator datasetsCreator;
    private final ImagesCreator imagesCreator;
    private final DatasetSerializer datasetSerializer;
    private final ProjectJsonCreator projectJsonCreator;
    private final RemoteMetadataCreator remoteMetadataCreator;

    public enum ImageType {
        Image,
        Segmentation
    }

    public enum AddMethod {
        Link,
        Copy
    }

    /**
     * Make a projectCreator - main entry point to create + edit MoBIE projects.
     * This will create the project directory (if it doesn't exist).
     * @param projectLocation Filepath of project directory
     * @throws IOException
     */
    public ProjectCreator( File projectLocation ) throws IOException {
        this.projectLocation = projectLocation;
        if ( ! projectLocation.exists() )
            projectLocation.mkdirs();
        projectJson = new File( IOHelper.combinePath(  projectLocation.getAbsolutePath(), "project.json") );
        if ( projectJson.exists() ) {
            reloadProject();
        } else {
            this.project = new Project();
            project.setDatasets( new ArrayList<>() );
            project.setSpecVersion( "0.3.0" );
        }

        this.datasetsCreator = new DatasetsCreator( this );
        this.datasetSerializer = new DatasetSerializer( this );
        this.projectJsonCreator = new ProjectJsonCreator( this );
        this.imagesCreator = new ImagesCreator( this );
        this.remoteMetadataCreator = new RemoteMetadataCreator( this );

        try {
            readVoxelUnitFromImage();
        } catch (SpimDataException e) {
            e.printStackTrace();
        }
    }

    public File getProjectLocation() { return projectLocation; }

    public Project getProject() {
        return project;
    }

    public File getProjectJson() { return projectJson; }

    /**
     * Reload project by directly reading the project json
     * @throws IOException
     */
    public void reloadProject() throws IOException {
        this.project = new ProjectJsonParser().parseProject( projectJson.getAbsolutePath() );
    }

    public Dataset getDataset( String datasetName ) {
        if ( datasetName.equals(currentDatasetName) ) {
            return currentDataset;
        } else {
            File datasetJson = new File( IOHelper.combinePath( projectLocation.getAbsolutePath(), datasetName, "dataset.json") );
            if ( datasetJson.exists() ) {
                try {
                    currentDatasetJson = datasetJson;
                    currentDatasetName = datasetName;
                    reloadCurrentDataset();
                    return currentDataset;
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            } else {
                return null;
            }
        }
    }

    /**
     * Get all ui selection groups (i.e. MoBIE dropdown menu names) used by views in the named dataset
     * @param datasetName dataset name
     * @return array of ui selection group names
     */
    public String[] getGroups( String datasetName ) {
        if ( !datasetName.equals(currentDatasetName) ) {
            getDataset( datasetName );
        }

        String[] groups = null;
        if ( currentGrouptoViews != null && currentGrouptoViews.keySet().size() > 0 ) {
            groups = new String[currentGrouptoViews.keySet().size()];
            currentGrouptoViews.keySet().toArray(groups);
        }
        return groups;
    }

    /**
     * Get all view names within the given dataset and ui selection group (i.e. MoBIE dropdown menu)
     * @param datasetName dataset name
     * @param uiSelectionGroup ui selection group name
     * @return array of view names
     */
    public String[] getViews( String datasetName, String uiSelectionGroup ) {
        if ( !datasetName.equals(currentDatasetName) ) {
            getDataset( datasetName );
        }

        String[] views = null;
        if ( currentGrouptoViews != null && currentGrouptoViews.keySet().size() > 0 ) {
            views = currentGrouptoViews.get(uiSelectionGroup).toArray(new String[0]);
        }

        return views;
    }

    public String getVoxelUnit() {
        return voxelUnit;
    }

    public void setVoxelUnit(String voxelUnit) {
        this.voxelUnit = voxelUnit;
    }

    /**
     * Reload dataset by directly reading the dataset json
     * @throws IOException
     */
    public void reloadCurrentDataset() throws IOException {
        if ( currentDatasetName != null ) {
            this.currentDataset = new DatasetJsonParser().parseDataset(currentDatasetJson.getAbsolutePath());
            this.currentGrouptoViews = getGroupToViewsMap(this.currentDataset);
        }
    }

    public DatasetsCreator getDatasetsCreator() {
        return datasetsCreator;
    }

    public ImagesCreator getImagesCreator() {
        return imagesCreator;
    }

    public DatasetSerializer getDatasetJsonCreator() {
        return datasetSerializer;
    }

    public ProjectJsonCreator getProjectJsonCreator() { return projectJsonCreator; }

    public RemoteMetadataCreator getRemoteMetadataCreator() {
        return remoteMetadataCreator;
    }

    private void readVoxelUnitFromImage() throws SpimDataException {
        // open first image (if any exist) to determine voxel unit
        if ( project.datasets().size() == 0 ) {
            return;
        }

        for ( String datasetName: project.datasets() ) {
            Dataset dataset = getDataset( datasetName );
            if ( dataset != null && dataset.sources().size() > 0 ) {
                for ( DataSource dataSource : dataset.sources().values() ) {
                    ImageDataSource imageSource = ( ImageDataSource ) dataSource;
                    // open one of the local images
                    for ( ImageDataFormat format : imageSource.imageData.keySet() ) {
                        if ( ! format.isRemote() ) {
                            String imagePath = IOHelper.combinePath( projectLocation.getAbsolutePath(), datasetName,
                                    imageSource.imageData.get(format).relativePath);
                            voxelUnit = MoBIEHelper.fetchVoxelDimensions( imagePath ).unit();
                            return;
                        }
                    }
                }
            }
        }
    }
}
