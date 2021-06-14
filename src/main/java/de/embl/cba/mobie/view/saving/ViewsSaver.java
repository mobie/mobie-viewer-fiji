package de.embl.cba.mobie.view.saving;

import de.embl.cba.mobie.MoBIESettings;
import de.embl.cba.mobie.Dataset;
import de.embl.cba.mobie.MoBIE;
import de.embl.cba.mobie.serialize.AdditionalViewsJsonParser;
import de.embl.cba.mobie.serialize.DatasetJsonParser;
import de.embl.cba.mobie.view.View;
import de.embl.cba.mobie.view.additionalviews.AdditionalViews;
import de.embl.cba.tables.github.GitHubUtils;
import ij.IJ;
import ij.gui.GenericDialog;
import org.apache.commons.io.FilenameUtils;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.*;
import java.util.Arrays;
import java.util.HashMap;

import static de.embl.cba.mobie.projectcreator.ProjectCreatorHelper.makeNewUiSelectionGroup;
import static de.embl.cba.mobie.ui.UserInterfaceHelper.tidyString;
import static de.embl.cba.mobie.view.saving.ViewSavingHelpers.writeAdditionalViewsJson;
import static de.embl.cba.mobie.view.saving.ViewSavingHelpers.writeDatasetJson;
import static de.embl.cba.mobie.Utils.FileLocation;
import static de.embl.cba.tables.FileAndUrlUtils.getFileNames;
import static de.embl.cba.tables.S3Utils.isS3;
import static de.embl.cba.tables.github.GitHubUtils.isGithub;

public class ViewsSaver {

    private MoBIE moBIE;
    private MoBIESettings settings;

    enum ProjectSaveLocation {
        datasetJson,
        viewsJson
    }

    enum SaveMethod {
        saveAsNewView,
        overwriteExistingView
    }

    public ViewsSaver(MoBIE moBIE) {
        this.moBIE = moBIE;
        this.settings = moBIE.getSettings();
    }

    public void saveCurrentSettingsAsViewDialog() {
        final GenericDialog gd = new GenericDialog("Save current view");

        gd.addChoice("Save to", new String[]{ FileLocation.Project.toString(),
                FileLocation.FileSystem.toString()}, FileLocation.Project.toString());

        String[] currentUiSelectionGroups = moBIE.getUserInterface().getUISelectionGroupNames();
        String[] choices = new String[currentUiSelectionGroups.length + 1];
        choices[0] = "Make New Ui Selection Group";
        for (int i = 0; i < currentUiSelectionGroups.length; i++) {
            choices[i + 1] = currentUiSelectionGroups[i];
        }
        gd.addChoice("Ui Selection Group", choices, choices[0]);

        gd.addCheckbox("exclusive", true);
        gd.addCheckbox("Include viewer transform?", true );
        gd.showDialog();

        if (!gd.wasCanceled()) {
            FileLocation fileLocation = FileLocation.valueOf(gd.getNextChoice());
            String uiSelectionGroup = gd.getNextChoice();
            boolean exclusive = gd.getNextBoolean();
            boolean includeViewerTransform = gd.getNextBoolean();

            if (uiSelectionGroup.equals("Make New Ui Selection Group")) {
                uiSelectionGroup = makeNewUiSelectionGroup(currentUiSelectionGroups);
            }

            if (uiSelectionGroup != null) {
                if (fileLocation == FileLocation.Project) {
                    saveToProject( uiSelectionGroup, exclusive, includeViewerTransform );
                } else {
                    saveToFileSystem( uiSelectionGroup, exclusive, includeViewerTransform );
                }
            }
        }
    }

    private void saveToFileSystem( String uiSelectionGroup, boolean exclusive, boolean includeViewerTransform ) {
        String jsonPath = null;
        final JFileChooser jFileChooser = new JFileChooser();
        jFileChooser.setFileFilter(new FileNameExtensionFilter("json", "json"));
        if (jFileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            jsonPath = jFileChooser.getSelectedFile().getAbsolutePath();
        }

        if (jsonPath != null) {
            if (!jsonPath.endsWith(".json")) {
                jsonPath += ".json";
            }

            View currentView = moBIE.getViewerManager().getCurrentView(uiSelectionGroup, exclusive, includeViewerTransform);
            if ( currentView != null ) {
                try {
                    saveToAdditionalViewsJson(currentView, jsonPath);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void saveToProject( String uiSelectionGroup, boolean exclusive, boolean includeViewerTransform ) {
        if ( isS3(settings.values.getProjectLocation()) ) {
            // TODO - support saving views to s3?
            IJ.log("View saving aborted - saving directly to s3 is not yet supported!");
        } else {
            ProjectSaveLocation projectSaveLocation = chooseProjectSaveLocationDialog();
            if (projectSaveLocation != null) {
                View currentView = moBIE.getViewerManager().getCurrentView(uiSelectionGroup, exclusive, includeViewerTransform);

                if ( currentView != null ) {
                    try {
                        if (projectSaveLocation == ProjectSaveLocation.datasetJson) {
                            saveToDatasetJson(currentView);
                        } else {
                            String viewJsonPath = chooseAdditionalViewsJson();
                            if (viewJsonPath != null) {
                                saveToAdditionalViewsJson(currentView, viewJsonPath);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void saveToDatasetJson( View view ) throws IOException {
        String datasetJsonPath = moBIE.getDatasetPath( "dataset.json");
        Dataset dataset = new DatasetJsonParser().parseDataset( datasetJsonPath );

        String viewName;
        if ( dataset.views.keySet().size() > 0 ) {
            SaveMethod saveMethod = chooseSaveMethodDialog();
            if ( saveMethod != null ) {
                switch( saveMethod ) {
                    case saveAsNewView:
                        viewName = chooseNewViewNameDialog();
                        if ( viewName != null && dataset.views.containsKey( viewName ) ) {
                            IJ.log( "View saving aborted - this view name already exists!" );
                        } else {
                            writeDatasetJson( dataset, view, viewName, datasetJsonPath );
                        }
                        break;
                    case overwriteExistingView:
                        new SelectExistingViewFrame( dataset, view, datasetJsonPath );
                        break;
                }
            }
        } else {
            viewName = chooseNewViewNameDialog();
            writeDatasetJson( dataset, view, viewName, datasetJsonPath );
        }

    }

    private void saveToExistingViewsJson( View view, String jsonPath ) throws IOException {
        AdditionalViews additionalViews = new AdditionalViewsJsonParser().getViews( jsonPath );
        SaveMethod saveMethod = chooseSaveMethodDialog();
        if ( saveMethod != null ) {
            switch( saveMethod ) {
                case saveAsNewView:
                    String viewName = chooseNewViewNameDialog();
                    if ( viewName != null && additionalViews.views.containsKey( viewName ) ) {
                        IJ.log( "View saving aborted - this view name already exists!" );
                    } else {
                        writeAdditionalViewsJson( additionalViews, view, viewName, jsonPath );
                    }
                    break;
                case overwriteExistingView:
                    new SelectExistingViewFrame( additionalViews, view, jsonPath );
                    break;
            }
        }
    }

    private void saveToAdditionalViewsJson( View view, String jsonPath ) throws IOException {

        boolean jsonExists;
        if ( isGithub( jsonPath )) {
            jsonExists = new ViewsGithubWriter( GitHubUtils.rawUrlToGitLocation(jsonPath) ).jsonExists();
        } else {
            jsonExists = new File( jsonPath ).exists();
        }

        String viewName;
        if ( jsonExists ) {
            saveToExistingViewsJson( view, jsonPath );
        } else {
            viewName = chooseNewViewNameDialog();
            AdditionalViews additionalViews = new AdditionalViews();
            additionalViews.views = new HashMap<>();
            writeAdditionalViewsJson( additionalViews, view, viewName, jsonPath );
        }
    }

    private String chooseAdditionalViewsJson() {
        String additionalViewsDirectory = moBIE.getDatasetPath( "misc", "views");
        String[] existingViewFiles = getFileNames(additionalViewsDirectory);

        String jsonFileName;
        if ( existingViewFiles != null && existingViewFiles.length > 0 ) {
            jsonFileName = chooseViewsJsonDialog( existingViewFiles );
        } else {
            jsonFileName = makeNewViewFile( existingViewFiles );
        }

        if ( jsonFileName != null ) {
            return moBIE.getDatasetPath( "misc", "views", jsonFileName);
        } else {
            return null;
        }
    }

    private String chooseViewsJsonDialog(String[] viewFileNames) {
        final GenericDialog gd = new GenericDialog("Choose views json");
        String[] choices = new String[viewFileNames.length + 1];
        choices[0] = "Make new views json file";
        for (int i = 0; i < viewFileNames.length; i++) {
            choices[i + 1] = viewFileNames[i];
        }

        gd.addChoice("Choose Views Json:", choices, choices[0]);
        gd.showDialog();

        if (!gd.wasCanceled()) {
            String choice = gd.getNextChoice();
            if (choice.equals("Make new views json file")) {
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
        if ( viewFileName!= null ) {
            viewFileName = tidyString( viewFileName);
        }

        if ( viewFileName != null ) {
            viewFileName += ".json";
            boolean alreadyExists = Arrays.asList( existingViewFiles ).contains( viewFileName );
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
                viewFileName = FilenameUtils.removeExtension( viewFileName );
            }
            return viewFileName;
        } else {
            return null;
        }

    }

    private SaveMethod chooseSaveMethodDialog() {
        final GenericDialog gd = new GenericDialog("How to save?");
        String[] choices = new String[]{ "Save as new view", "Overwrite existing view" };
        gd.addChoice("Save method:", choices, choices[0] );
        gd.showDialog();

        if (!gd.wasCanceled()) {
            String saveMethod = gd.getNextChoice();
            if ( saveMethod.equals("Save as new view") ) {
                return SaveMethod.saveAsNewView;
            } else if ( saveMethod.equals( "Overwrite existing view" )) {
                return SaveMethod.overwriteExistingView;
            }
        }

        return null;
    }

    public String chooseNewViewNameDialog() {
        final GenericDialog gd = new GenericDialog("Choose view name:");

        gd.addStringField("View name:", "", 25 );
        gd.showDialog();

        if (!gd.wasCanceled()) {
            return tidyString( gd.getNextString() );
            // TODO - check if already exists
        } else {
            return null;
        }
    }

    private ProjectSaveLocation chooseProjectSaveLocationDialog() {
        final GenericDialog gd = new GenericDialog("Save location");
        String[] choices = new String[]{"dataset.json", "views.json"};
        gd.addChoice("Save location:", choices, choices[0]);
        gd.showDialog();

        if (!gd.wasCanceled()) {
            String projectSaveLocation = gd.getNextChoice();
            if (projectSaveLocation.equals("dataset.json")) {
                return ProjectSaveLocation.datasetJson;
            } else if (projectSaveLocation.equals("views.json")) {
                return ProjectSaveLocation.viewsJson;
            }
        }
        return null;
    }

}
