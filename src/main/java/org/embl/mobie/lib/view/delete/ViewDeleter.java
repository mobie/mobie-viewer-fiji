package org.embl.mobie.lib.view.delete;

import ij.IJ;
import org.apache.commons.lang.NotImplementedException;
import org.embl.mobie.MoBIE;
import org.embl.mobie.lib.io.FileLocation;
import org.embl.mobie.lib.serialize.AdditionalViewsJsonParser;
import org.embl.mobie.lib.serialize.Dataset;
import org.embl.mobie.lib.serialize.DatasetJsonParser;
import org.embl.mobie.lib.serialize.View;
import org.embl.mobie.lib.view.AdditionalViews;
import org.embl.mobie.lib.view.save.SelectExistingViewDialog;
import org.embl.mobie.ui.UserInterfaceHelper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.embl.mobie.io.github.GitHubUtils.isGithub;
import static org.embl.mobie.io.util.S3Utils.isS3;

public class ViewDeleter {

    static { net.imagej.patcher.LegacyInjector.preinit(); }

    private MoBIE moBIE;

    /**
     * Delete views from the current dataset, or an external file
     */
    public ViewDeleter( MoBIE moBIE ) {
        this.moBIE = moBIE;
    }

    public void deleteViewDialog()
    {
        new Thread( () -> {
            try {
                FileLocation fileLocation = UserInterfaceHelper.loadFromProjectOrFileSystemDialog("Delete from");

                if ( fileLocation == FileLocation.CurrentProject ) {
                    removeViewsFromCurrentProject();
                } else if ( fileLocation == FileLocation.ExternalFile ) {
                    removeViewsFromExternalFile();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void removeViewsFromCurrentProject() throws IOException {
        String datasetJson = moBIE.absolutePath( "dataset.json");

        if ( isGithub(datasetJson) || isS3(datasetJson) ) {
            throw new NotImplementedException("View deletion is only implemented for local projects");
        }

        // Read views directly from dataset json rather than from MoBIE.getViews() (otherwise could include
        // views loaded from external files via Load Additional Views)
        Dataset dataset = new DatasetJsonParser().parseDataset( datasetJson );
        Map<String, View> views = dataset.views();
        // Remove default view, as this shouldn't be deleted
        views.remove( View.DEFAULT );

        if ( views.isEmpty() ) {
            IJ.log("No valid views in dataset - can't remove default view");
            return;
        }

        Map<String, View> viewsToRemove = selectViewsToRemove( views );
        if ( viewsToRemove == null ) {
            return;
        }
        removeViewsFromDatasetJson( viewsToRemove, datasetJson );
        removeViewsFromUI( viewsToRemove );
    }

    private void removeViewsFromExternalFile() throws IOException {
        String selectedFilePath = UserInterfaceHelper.selectFilePath( "json", "View", true );
        if ( selectedFilePath == null ) {
            return;
        }

        Map<String, View> views = new AdditionalViewsJsonParser().getViews( selectedFilePath ).views;
        if ( views.isEmpty() ) {
            IJ.log("No valid views in file");
            return;
        }

        Map<String, View> viewsToRemove = selectViewsToRemove( views );
        if ( viewsToRemove == null ) {
            return;
        }
        removeViewsFromAdditionalViewsJson( viewsToRemove, selectedFilePath );
        removeViewsFromUI( viewsToRemove );
    }

    private Map<String, View> selectViewsToRemove( Map<String, View> views ) {
        String selectedView = new SelectExistingViewDialog( views ).getSelectedView(
                "Choose a view to delete..."
        );
        if ( selectedView == null ) {
            return null;
        }

        Map<String, View> viewsToRemove = new HashMap<>();
        viewsToRemove.put( selectedView, views.get(selectedView) );

        return viewsToRemove;
    }

    public void removeViewsFromDatasetJson( Map<String, View> views, String datasetJsonPath) throws IOException {
        Dataset dataset = new DatasetJsonParser().parseDataset( datasetJsonPath );
        dataset.views().keySet().removeAll( views.keySet() );

        new DatasetJsonParser().saveDataset( dataset, datasetJsonPath );
        IJ.log( "Views \"" + views.keySet()  + "\" removed from dataset.json" );
    }

    public void removeViewsFromAdditionalViewsJson( Map<String, View> views, String jsonPath ) throws IOException
    {
        AdditionalViews additionalViews = new AdditionalViewsJsonParser().getViews( jsonPath );
        additionalViews.views.keySet().removeAll( views.keySet() );

        new AdditionalViewsJsonParser().saveViews( additionalViews, jsonPath );
        IJ.log( "Views \"" + views.keySet() + "\" removed from " + jsonPath );
    }

    public void removeViewsFromUI( Map<String, View> views )
    {
        moBIE.getViews().keySet().removeAll( views.keySet() );
        moBIE.getUserInterface().removeViews( views );
        IJ.log( "The following views were removed:\n" + views.keySet() );
    }
}
