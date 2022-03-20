package org.embl.mobie.viewer.projectcreator;

import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imagej.patcher.LegacyInjector;
import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.io.SpimDataOpener;
import org.embl.mobie.viewer.Dataset;
import org.embl.mobie.viewer.Project;
import org.embl.mobie.viewer.serialize.DatasetJsonParser;
import org.embl.mobie.viewer.serialize.ProjectJsonParser;
import org.embl.mobie.io.util.FileAndUrlUtils;
import org.embl.mobie.viewer.source.ImageSource;
import org.embl.mobie.viewer.source.SourceSupplier;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import static org.embl.mobie.viewer.projectcreator.ProjectCreatorHelper.getGroupToViewsMap;

public class ProjectCreator {

    static { LegacyInjector.preinit(); }

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
    private final DatasetJsonCreator datasetJsonCreator;
    private final ProjectJsonCreator projectJsonCreator;
    private final RemoteMetadataCreator remoteMetadataCreator;

    public enum ImageType {
        image,
        segmentation
    }

    public enum AddMethod {
        link,
        copy,
        move
    }

    /**
     * Make a project creator, allowing creation / editing of projects
     * @param projectLocation directory that contains the project.json and individual dataset directories
     * @throws IOException
     */
    public ProjectCreator( File projectLocation ) throws IOException {
        this.projectLocation = projectLocation;
        if ( ! projectLocation.exists() )
            projectLocation.mkdirs();
        projectJson = new File( FileAndUrlUtils.combinePath(  projectLocation.getAbsolutePath(), "project.json") );
        if ( projectJson.exists() ) {
            reloadProject();
        } else {
            this.project = new Project();
            project.setDatasets( new ArrayList<>() );
            project.setImageDataFormats( new ArrayList<>() );
            project.setSpecVersion( "0.2.0" );
        }

        this.datasetsCreator = new DatasetsCreator( this );
        this.datasetJsonCreator = new DatasetJsonCreator( this );
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
     * Reload project by parsing the project.json again
     * @throws IOException
     */
    public void reloadProject() throws IOException {
        this.project = new ProjectJsonParser().parseProject( projectJson.getAbsolutePath() );
    }

    public Dataset getDataset( String datasetName ) {
        if ( datasetName.equals(currentDatasetName) ) {
            return currentDataset;
        } else {
            File datasetJson = new File( FileAndUrlUtils.combinePath( projectLocation.getAbsolutePath(), datasetName, "dataset.json") );
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
     * Get names of uiSelectionGroups in this dataset
     * @param datasetName dataset name
     * @return names of uiSelectionGroups
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
     * Get names of views in this dataset and uiSelectionGroup
     * @param datasetName dataset name
     * @param uiSelectionGroup uiSelectionGroup
     * @return names of views
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
     * Reload current dataset by parsing the dataset.json again
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

    public DatasetJsonCreator getDatasetJsonCreator() {
        return datasetJsonCreator;
    }

    public ProjectJsonCreator getProjectJsonCreator() { return projectJsonCreator; }

    public RemoteMetadataCreator getRemoteMetadataCreator() {
        return remoteMetadataCreator;
    }

    private void readVoxelUnitFromImage() throws SpimDataException {
        // open first image (if any exist) to determine voxel unit
        if ( project.getDatasets().size() == 0 ) {
            return;
        }

        for ( String datasetName: project.getDatasets() ) {
            Dataset dataset = getDataset( datasetName );
            if ( dataset != null && dataset.sources.size() > 0 ) {
                for ( SourceSupplier sourceSupplier: dataset.sources.values() ) {
                    ImageSource imageSource = sourceSupplier.get();
                    // open one of the local images
                    for (ImageDataFormat format : imageSource.imageData.keySet()) {
                        if (!format.isRemote()) {
                            String imagePath = FileAndUrlUtils.combinePath( projectLocation.getAbsolutePath(), datasetName,
                                    imageSource.imageData.get(format).relativePath);
                            SpimData spimData = (SpimData) new SpimDataOpener().openSpimData( imagePath, format );
                            VoxelDimensions voxelDimensions = spimData.getSequenceDescription().
                                    getViewSetupsOrdered().get(0).getVoxelSize();
                            voxelUnit = voxelDimensions.unit();
                            return;
                        }
                    }
                }
            }
        }
    }
}
