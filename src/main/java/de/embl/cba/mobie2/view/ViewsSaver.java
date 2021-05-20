package de.embl.cba.mobie2.view;

import de.embl.cba.mobie.ui.MoBIESettings;
import de.embl.cba.mobie.utils.Utils;
import de.embl.cba.mobie2.Dataset;
import de.embl.cba.mobie2.MoBIE2;
import de.embl.cba.mobie2.serialize.AdditionalViewsJsonParser;
import de.embl.cba.mobie2.serialize.DatasetJsonParser;
import de.embl.cba.mobie2.view.additionalviews.AdditionalViews;
import de.embl.cba.tables.FileUtils;
import de.embl.cba.tables.github.GitHubUtils;
import ij.IJ;
import ij.gui.GenericDialog;
import org.apache.commons.io.FilenameUtils;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.*;
import java.util.Arrays;
import java.util.HashMap;

import static de.embl.cba.tables.FileUtils.*;

public class ViewsSaver {

    private MoBIE2 moBIE2;
    private MoBIESettings settings;

    enum ProjectSaveLocation {
        datasetJson,
        viewsJson
    }

    public ViewsSaver(MoBIE2 moBIE2) {
        this.moBIE2 = moBIE2;
        this.settings = moBIE2.getSettings();
    }

    public void saveCurrentSettingsAsViewDialog() {
        final GenericDialog gd = new GenericDialog("Save current view");
        gd.addStringField("View name", "name", 25);

        String[] currentUiSelectionGroups = moBIE2.getUserInterface().getUISelectionGroupNames();
        String[] choices = new String[currentUiSelectionGroups.length + 1];
        choices[0] = "Make New Ui Selection Group";
        for (int i = 0; i < currentUiSelectionGroups.length; i++) {
            choices[i + 1] = currentUiSelectionGroups[i];
        }
        gd.addChoice("Ui Selection Group", choices, choices[0]);

        gd.addChoice("Save to", new String[]{FileUtils.FileLocation.Project.toString(),
                FileUtils.FileLocation.FileSystem.toString()}, FileUtils.FileLocation.Project.toString());
        gd.addCheckbox("exclusive", true);
        gd.showDialog();

        if (!gd.wasCanceled()) {
            String viewName = gd.getNextString();
            String uiSelectionGroup = gd.getNextChoice();
            FileUtils.FileLocation fileLocation = FileUtils.FileLocation.valueOf(gd.getNextChoice());
            boolean exclusive = gd.getNextBoolean();

            viewName = tidyString( viewName );

            if ( viewName != null ) {

                if (uiSelectionGroup.equals("Make New Ui Selection Group")) {
                    uiSelectionGroup = makeNewUiSelectionGroup(currentUiSelectionGroups);
                }

                if (uiSelectionGroup != null) {
                    if (fileLocation == FileUtils.FileLocation.Project) {
                        saveToProject(viewName, uiSelectionGroup, exclusive);
                    } else {
                        saveToFileSystem(viewName, uiSelectionGroup, exclusive);
                    }
                }
            }
        }
    }

    private void saveToFileSystem( String viewName, String uiSelectionGroup, boolean exclusive ) {
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

            File jsonFile = new File(jsonPath);
            if (jsonFile.exists()) {
                // check if want to append to existing file, otherwise abort
                if (!appendToFileDialog()) {
                    jsonPath = null;
                }
            }

            if (jsonPath != null) {
                View currentView = moBIE2.getViewerManager().getCurrentView(uiSelectionGroup, exclusive);
                try {
                    saveToAdditionalViewsJson( currentView, viewName, jsonPath );
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void saveToProject( String viewName, String uiSelectionGroup, boolean exclusive ) {
        if ( isS3(settings.values.getProjectLocation()) ) {
            // TODO - support saving views to s3?
            IJ.log("View saving aborted - saving directly to s3 is not yet supported!");
        } else {
            ProjectSaveLocation projectSaveLocation = chooseProjectSaveLocationDialog();
            if (projectSaveLocation != null) {
                View currentView = moBIE2.getViewerManager().getCurrentView(uiSelectionGroup, exclusive);

                try {
                    if (projectSaveLocation == ProjectSaveLocation.datasetJson) {
                        saveToDatasetJson(currentView, viewName);
                    } else {
                        String viewJsonPath = chooseAdditionalViewsJson();
                        if (viewJsonPath != null) {
                            saveToAdditionalViewsJson(currentView, viewName, viewJsonPath);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void saveToDatasetJson( View view, String viewName ) throws IOException {
        String datasetJsonPath = moBIE2.getDatasetPath( "dataset.json");
        Dataset dataset = new DatasetJsonParser().parseDataset( datasetJsonPath );
        dataset.views.put( viewName, view );

        if ( isGithub( datasetJsonPath ) ) {
            new ViewsGithubWriter( GitHubUtils.rawUrlToGitLocation( datasetJsonPath ) ).writeViewToDatasetJson( viewName, view );
        } else {
            new DatasetJsonParser().saveDataset( dataset, datasetJsonPath );
        }

    }

    private void saveToAdditionalViewsJson( View view, String viewName, String jsonPath ) throws IOException {
        if ( isGithub( jsonPath ) ) {
            new ViewsGithubWriter( GitHubUtils.rawUrlToGitLocation( jsonPath ) ).writeViewToViewsJson( viewName, view );
        } else {
            AdditionalViews additionalViews;
            if (new File(jsonPath).exists()) {
                additionalViews = new AdditionalViewsJsonParser().getViews(jsonPath);
            } else {
                additionalViews = new AdditionalViews();
                additionalViews.views = new HashMap<>();
            }

            additionalViews.views.put(viewName, view);

            new AdditionalViewsJsonParser().saveViews(additionalViews, jsonPath);
        }
    }

    private String chooseAdditionalViewsJson() {
        String additionalViewsDirectory = moBIE2.getDatasetPath( "misc", "views");
        String[] existingViewFiles = getFileNamesFromProject(additionalViewsDirectory);

        String jsonFileName;
        if ( existingViewFiles != null && existingViewFiles.length > 0 ) {
            jsonFileName = chooseViewsJsonDialog( existingViewFiles );
        } else {
            jsonFileName = makeNewViewFile( existingViewFiles );
        }

        if ( jsonFileName != null ) {
            return moBIE2.getDatasetPath( "misc", "views", jsonFileName);
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

    private String makeNewUiSelectionGroup( String[] currentUiSelectionGroups ) {
        String newUiSelectionGroup = chooseNewSelectionGroupNameDialog();

        // get rid of any spaces, warn for unusual characters
        if ( newUiSelectionGroup != null ) {
            newUiSelectionGroup = tidyString(newUiSelectionGroup);
        }

        if ( newUiSelectionGroup != null ) {
            boolean alreadyExists = Arrays.asList(currentUiSelectionGroups).contains( newUiSelectionGroup );
            if ( alreadyExists ) {
                newUiSelectionGroup = null;
                IJ.log("Saving view aborted - new ui selection group already exists");
            }
        }

        return newUiSelectionGroup;
    }

    private String chooseNewSelectionGroupNameDialog() {
        final GenericDialog gd = new GenericDialog("Choose ui selection group Name:");

        gd.addStringField("New ui selection group name:", "", 25 );
        gd.showDialog();

        if (!gd.wasCanceled()) {
            return gd.getNextString();
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

    public static boolean appendToFileDialog() {
        int result = JOptionPane.showConfirmDialog(null,
                "This Json file already exists - append view to this file?", "Append to file?",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (result != JOptionPane.YES_OPTION) {
            return false;
        } else {
            return true;
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

    private String tidyString( String string ) {
        string = string.trim();
        String tidyString = string.replaceAll("\\s+","_");

        if ( !string.equals(tidyString) ) {
            Utils.log( "Spaces were removed from name, and replaced by _");
        }

        // check only contains alphanumerics, or _ -
        if ( !tidyString.matches("^[a-zA-Z0-9_-]+$") ) {
            Utils.log( "Names must only contain letters, numbers, _ or -. Please try again " +
                    "with a different name.");
            tidyString = null;
        }

        return tidyString;
    }

}
