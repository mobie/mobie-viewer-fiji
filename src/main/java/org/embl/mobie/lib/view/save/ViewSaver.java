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
package org.embl.mobie.lib.view.save;

import ij.IJ;
import ij.gui.GenericDialog;
import org.embl.mobie.MoBIE;
import org.embl.mobie.io.util.IOHelper;
import org.embl.mobie.lib.create.ui.ViewSaverDialog;
import org.embl.mobie.lib.io.FileLocation;
import org.embl.mobie.lib.serialize.AdditionalViewsJsonParser;
import org.embl.mobie.lib.serialize.Dataset;
import org.embl.mobie.lib.serialize.DatasetJsonParser;
import org.embl.mobie.lib.serialize.transformation.NormalizedAffineViewerTransform;
import org.embl.mobie.lib.util.MoBIEHelper;
import org.embl.mobie.ui.UserInterfaceHelper;
import org.embl.mobie.lib.view.AdditionalViews;
import org.embl.mobie.lib.serialize.View;
import org.apache.commons.io.FilenameUtils;
import org.embl.mobie.io.github.GitHubUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.embl.mobie.io.util.IOHelper.getFileNames;
import static org.embl.mobie.lib.view.save.ViewSavingHelper.writeAdditionalViewsJson;
import static org.embl.mobie.lib.view.save.ViewSavingHelper.writeDatasetJson;
import static org.embl.mobie.io.github.GitHubUtils.isGithub;

public class ViewSaver
{
    public static final String CREATE_NEW_VIEWS_JSON_FILE = "Make New Views JSON file";
    public static final String CREATE_SELECTION_GROUP = "Create New Group";

    static { net.imagej.patcher.LegacyInjector.preinit(); }

    private MoBIE moBIE;

    public ViewSaver( MoBIE moBIE) {
        this.moBIE = moBIE;
    }

    public boolean saveViewDialog( View view )
    {
        ViewSaverDialog dialog = new ViewSaverDialog( view );
        if ( ! dialog.show() )
            return false;

        if ( view.getName() == null )
            view.setName( UserInterfaceHelper.tidyString( dialog.getViewName() ) );

        if ( moBIE.getViews().containsKey( view.getName() ) )
        {
            GenericDialog gd = new GenericDialog( "Overwrite view?" );
            gd.addMessage( "A view named \"" + view.getName() + "\" exists already." +
                    "\nAre you sure you want to overwrite it?" );
            gd.showDialog();
            if ( ! gd.wasOKed() )
                return false;
        }

        view.setDescription( "" ); // TODO

        view.setExclusive( dialog.getMakeViewExclusive() );

        if ( view.isExclusive() )
            view.setViewerTransform( new NormalizedAffineViewerTransform( moBIE.getViewManager().getSliceViewer().getBdvHandle() ) );

        if ( dialog.getViewGroup().equals( CREATE_SELECTION_GROUP ) )
            view.setUiSelectionGroup( dialog.getNewGroup() );
        else
            view.setUiSelectionGroup( dialog.getViewGroup() );

        addViewToUi( view );

        FileLocation fileLocation = dialog.getFileLocation();
        if ( fileLocation == FileLocation.CurrentProject )
            saveNewViewToProject( view, "dataset.json" );
        else if ( fileLocation == FileLocation.ExternalFile )
            saveNewViewToFileSystem( view, dialog.getViewJsonPath() );

        return true;
    }

    private String chooseFileSystemJson() {
        String jsonPath = UserInterfaceHelper.selectFilePath( "json", "json file", false );

        if ( jsonPath != null && !jsonPath.endsWith(".json") ) {
                jsonPath += ".json";
        }

        return jsonPath;
    }

    private void saveNewViewToFileSystem( View view, String jsonPath ) {
        try {
            saveNewViewToAdditionalViewsJson( view, jsonPath );
        } catch (IOException e) {
            throw new RuntimeException( e );
        }
    }

//    private void overwriteExistingViewOnFileSystem( View view ) {
//        new Thread( () -> {
//            String jsonPath = chooseFileSystemJson();
//            if ( jsonPath != null ) {
//                try {
//                    overwriteExistingViewInAdditionalViewsJson( view, jsonPath );
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }).start();
//    }

    private void saveNewViewToProject( View view, String viewJson ) {
        try {
            if ( viewJson.equals( "dataset.json" ) )
            {
                saveViewToDatasetJson( view );
            }
            else
            {
                String jsonPath = chooseAdditionalViewsJson();
                if ( jsonPath != null ) {
                    saveNewViewToAdditionalViewsJson( view, jsonPath);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException( e );
        }
    }

//    private void overwriteExistingViewInProject( View view, ProjectSaveLocation projectSaveLocation )
//    {
//        if ( isS3( moBIE.getProjectLocation() ) )
//        {
//            throw new UnsupportedOperationException("View saving aborted - saving directly to s3 is not yet supported!");
//        }
//        else
//        {
//            try {
//                if (projectSaveLocation == ProjectSaveLocation.datasetJson) {
//                    overwriteExistingViewInDatasetJson( view );
//                } else {
//                    String viewJsonPath = chooseAdditionalViewsJson( false );
//                    if (viewJsonPath != null) {
//                        overwriteExistingViewInAdditionalViewsJson( view, viewJsonPath);
//                    }
//                }
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//        }
//    }

    private void addViewToUi( View view )
    {
        moBIE.getViews().put( view.getName(), view );
        Map<String, View > views = new HashMap<>();
        views.put( view.getName(), view );
        moBIE.getUserInterface().addViews( views );
    }

    public void saveViewToDatasetJson( View view ) throws IOException
    {
        String datasetJsonPath = moBIE.absolutePath( "dataset.json");
        Dataset dataset = new DatasetJsonParser().parseDataset( datasetJsonPath );
        writeDatasetJson( dataset, view, datasetJsonPath );
        IJ.log( "View \"" + view.getName()  + "\" written to dataset.json" );
    }

    // TODO: Delete this? https://github.com/mobie/mobie-viewer-fiji/issues/1150
//    private void overwriteExistingViewInDatasetJson( View view ) throws IOException {
//        String datasetJsonPath = moBIE.absolutePath( "dataset.json");
//        Dataset dataset = new DatasetJsonParser().parseDataset( datasetJsonPath );
//
//        if ( ! dataset.views().keySet().isEmpty() ) {
//            String selectedView = new SelectExistingViewDialog( dataset ).getSelectedView();
//            if ( selectedView != null ) {
//                writeDatasetJson( dataset, view, datasetJsonPath );
//                IJ.log( selectedView + " overwritten in dataset.json" );
//                addViewToUi( selectedView, view );
//            }
//        } else {
//            IJ.log( "View saving aborted - dataset.json contains no views" );
//        }
//    }

    private boolean jsonExists( String jsonPath ) {
        if ( isGithub( jsonPath )) {
            return new ViewsGithubWriter( GitHubUtils.rawUrlToGitLocation(jsonPath) ).jsonExists();
        } else {
            return new File( jsonPath ).exists();
        }
    }

//    private void overwriteExistingViewInAdditionalViewsJson( View view, String jsonPath ) throws IOException {
//
//        if ( !jsonExists( jsonPath ) ) {
//            IJ.log( "View saving aborted - this views json does not exist" );
//            return;
//        }
//
//        AdditionalViews additionalViews = new AdditionalViewsJsonParser().getViews( jsonPath );
//        String selectedView = new SelectExistingViewDialog( additionalViews ).getSelectedView();
//
//        if ( selectedView != null ) {
//            writeAdditionalViewsJson( additionalViews, view, selectedView, jsonPath );
//            IJ.log( selectedView + " overwritten in " + new File(jsonPath).getName() );
//            addViewToUi( selectedView, view );
//        }
//    }

    private void saveNewViewToAdditionalViewsJson( View view, String jsonPath ) throws IOException
    {
        AdditionalViews additionalViews;
        if ( jsonExists( jsonPath ) ) {
            additionalViews = new AdditionalViewsJsonParser().getViews( jsonPath );
        } else {
            additionalViews = new AdditionalViews();
            additionalViews.views = new HashMap<>();
        }

        writeAdditionalViewsJson( additionalViews, view, jsonPath );
        IJ.log( "Saved view \"" + view.getName() + "\" to " + jsonPath );
    }

    private String chooseAdditionalViewsJson( ) {
        String additionalViewsDirectory = moBIE.absolutePath( "misc", "views");
        String[] existingViewFiles = IOHelper.getFileNames( additionalViewsDirectory );

        String jsonFileName = null;
        if ( existingViewFiles != null && existingViewFiles.length > 0 ) {
            jsonFileName = chooseViewsJsonDialog( existingViewFiles, true );
        }
        else
        {
            jsonFileName = makeNewViewFile( existingViewFiles );
        }

        if ( jsonFileName != null ) {
            return moBIE.absolutePath( "misc", "views", jsonFileName);
        } else {
            return null;
        }
    }

    private String chooseViewsJsonDialog( String[] viewFileNames, boolean includeOptionToMakeNewViewJson ) {
        final GenericDialog gd = new GenericDialog("Choose views json");

        String[] choices;
        if ( includeOptionToMakeNewViewJson ) {
            choices = new String[viewFileNames.length + 1];
            choices[0] = CREATE_NEW_VIEWS_JSON_FILE;
            for (int i = 0; i < viewFileNames.length; i++) {
                choices[i + 1] = viewFileNames[i];
            }
        } else {
            choices = viewFileNames;
        }

        gd.addChoice("Choose Views Json:", choices, choices[0]);
        gd.showDialog();

        if (!gd.wasCanceled()) {
            String choice = gd.getNextChoice();
            if ( includeOptionToMakeNewViewJson && choice.equals( CREATE_NEW_VIEWS_JSON_FILE ) )
            {
                choice = makeNewViewFile( viewFileNames );
            }
            return choice;
        } else {
            return null;
        }

    }

    private String makeNewViewFile( String[] existingViewFiles ) {
        String viewFileName = chooseNewViewsFileNameDialog();

        // get rid of any spaces, warn for unusual characters in basename (without the .json)
        if ( viewFileName != null ) {
            viewFileName = UserInterfaceHelper.tidyString( viewFileName);
        }

        if ( viewFileName != null ) {
            viewFileName += ".json";

            boolean alreadyExists = false;
            if ( existingViewFiles != null && existingViewFiles.length > 0 ) {
                alreadyExists = Arrays.asList(existingViewFiles).contains(viewFileName);
            }
            if ( alreadyExists ) {
                viewFileName = null;
                IJ.log("Saving view aborted - new view file already exists");
            }
        }

        return viewFileName;
    }

    private String chooseNewViewsFileNameDialog() {
        final GenericDialog gd = new GenericDialog("Choose views json filename");
        gd.addStringField("New view json filename:", "", 25 );
        gd.showDialog();

        if (!gd.wasCanceled()) {
            String viewFileName =  gd.getNextString();

            // we just want the basename, no extension
            if ( viewFileName != null && viewFileName.endsWith(".json") ) {
                viewFileName = MoBIEHelper.removeExtension( viewFileName );
            }
            return viewFileName;
        } else {
            return null;
        }

    }

}
