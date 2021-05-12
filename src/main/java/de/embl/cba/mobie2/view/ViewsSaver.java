package de.embl.cba.mobie2.view;

import de.embl.cba.mobie.ui.MoBIEOptions;
import de.embl.cba.mobie2.Dataset;
import de.embl.cba.mobie2.MoBIE2;
import de.embl.cba.mobie2.serialize.AdditionalViewsJsonParser;
import de.embl.cba.mobie2.serialize.DatasetJsonParser;
import de.embl.cba.mobie2.view.additionalviews.AdditionalViews;
import de.embl.cba.tables.FileUtils;
import de.embl.cba.tables.github.GitHubUtils;
import ij.IJ;
import ij.gui.GenericDialog;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

import static de.embl.cba.mobie2.PathHelpers.getPath;
import static de.embl.cba.tables.FileUtils.*;

public class ViewsSaver {

    private MoBIE2 moBIE2;
    private MoBIEOptions options;

    enum ProjectSaveLocation {
        datasetJson,
        viewsJson
    }

    enum ViewsJsonOptions {
        appendToExisting,
        createNew
    }

    public ViewsSaver(MoBIE2 moBIE2) {
        this.moBIE2 = moBIE2;
        this.options = moBIE2.getOptions();
    }

    // TODO for file system project list bookmarks, don't open free-form dialog
    // TODO - directly read existing file to be sure dataset is up to date

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

    private void saveToDatasetJson( View view, String viewName ) throws IOException {
        String datasetJsonPath = getPath(options.values.getProjectLocation(), options.values.getProjectBranch(), moBIE2.getDatasetName(), "dataset.json");
        Dataset dataset = moBIE2.getDataset();
        dataset.views.put( viewName, view );

        if ( isGithub( datasetJsonPath ) ) {
            new ViewsGithubWriter( GitHubUtils.rawUrlToGitLocation( datasetJsonPath ) ).writeViewToDatasetJson( viewName, view );
        } else {
            new DatasetJsonParser().saveDataset( dataset, datasetJsonPath );
        }

        Map<String, View> views = new HashMap<>();
        views.put( viewName, view );
        moBIE2.getUserInterface().addViews( views );

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
        // TODO - choose existing view file, or say make new one
        // if make new one, choose a name for it
        String additionalViewsDirectory = getPath(options.values.getProjectLocation(), options.values.getProjectBranch(), moBIE2.getDatasetName(), "misc", "views");
        String[] existingViewFiles = getFileNamesFromProject(additionalViewsDirectory);

        String jsonFileName;
        if (existingViewFiles != null && existingViewFiles.length > 0) {
            // TODO - give option to choose existing or make new
            jsonFileName = chooseViewsJsonDialog(existingViewFiles);
        } else {
            jsonFileName = chooseViewsFileNameDialog();
        }

        if ( jsonFileName != null ) {
            return getPath(options.values.getProjectLocation(), options.values.getProjectBranch(), moBIE2.getDatasetName(), "misc", "views", jsonFileName);
        } else {
            return null;
        }
    }

    private void saveToProject( String viewName, String uiSelectionGroup, boolean exclusive ) {
        if ( isS3(options.values.getProjectLocation()) ) {
            // TODO - support saving views to s3?
            IJ.log("View saving aborted - saving directly to s3 is not yet supported!");
        } else {
            ProjectSaveLocation projectSaveLocation = chooseSaveLocationDialog();
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

    public void saveCurrentSettingsAsViewDialog() {
        final GenericDialog gd = new GenericDialog("Save current view");
        gd.addStringField("View name", "name");

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

            if (fileLocation == FileUtils.FileLocation.Project) {
                saveToProject( viewName, uiSelectionGroup, exclusive );
            } else {
                saveToFileSystem( viewName, uiSelectionGroup, exclusive );
            }
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

    private ProjectSaveLocation chooseSaveLocationDialog() {
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

    private ProjectSaveLocation chooseViewJsonOption() {
        final GenericDialog gd = new GenericDialog("Options for view json:");
        String[] choices = new String[]{"Append to existing views json", "Make new views json"};
        gd.addChoice("View json options:", choices, choices[0]);
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

    private String chooseViewsFileNameDialog() {
        final GenericDialog gd = new GenericDialog("Choose views json filename");

        gd.addStringField("New view json filename:", "");
        gd.showDialog();

        if (!gd.wasCanceled()) {
            // TODO - check for invalid names e.g. stuff with spaces, punctuation etc...
            String viewFileName =  gd.getNextString();
            if ( !viewFileName.endsWith(".json") ) {
                viewFileName += ".json";
            }
            return viewFileName;
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
                choice = chooseViewsFileNameDialog();
                // TODO - check if that file exists already, if it does - abort and log a warning sayin why
            }
            return choice;
        } else {
            return null;
        }

    }

}
