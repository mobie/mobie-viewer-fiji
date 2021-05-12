package de.embl.cba.mobie2.view;

import com.google.gson.GsonBuilder;
import com.google.gson.internal.LinkedTreeMap;
import de.embl.cba.mobie2.Dataset;
import de.embl.cba.mobie2.serialize.AdditionalViewsJsonParser;
import de.embl.cba.mobie2.serialize.DatasetJsonParser;
import de.embl.cba.mobie2.view.additionalviews.AdditionalViews;
import de.embl.cba.tables.github.GitHubContentGetter;
import de.embl.cba.tables.github.GitHubFileCommitter;
import de.embl.cba.tables.github.GitLocation;
import ij.Prefs;
import ij.gui.GenericDialog;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ViewsGithubWriter {
    public static final String ACCESS_TOKEN = "MoBIE.GitHub access token";
    private String accessToken;
    private GitLocation viewJsonGitLocation;

    ViewsGithubWriter( GitLocation viewJsonGitLocation ) {
        this.viewJsonGitLocation = viewJsonGitLocation;
    }

    private static String writeAdditionalViewsToBase64String ( AdditionalViews additionalViews ) {
        String jsonString = new AdditionalViewsJsonParser().viewsToJsonString( additionalViews );
        byte[] jsonBytes = jsonString.getBytes( StandardCharsets.UTF_8);
        return Base64.getEncoder().encodeToString(jsonBytes);
        // TODO - add new line at end?
    }

    private static String writeDatasetToBase64String ( Dataset dataset ) {
        String jsonString = new DatasetJsonParser().datasetToJsonString( dataset );
        byte[] jsonBytes = jsonString.getBytes( StandardCharsets.UTF_8);
        return Base64.getEncoder().encodeToString(jsonBytes);
        // TODO - add new line at end?
    }

    class FilePathAndSha {
        String filePath;
        String sha;
    }

    private FilePathAndSha getFilePathAndSha ( String content ) {
        GsonBuilder builder = new GsonBuilder();
        FilePathAndSha filePathAndSha = new FilePathAndSha();

        LinkedTreeMap linkedTreeMap = ( LinkedTreeMap ) builder.create().fromJson( content, Object.class );
        filePathAndSha.filePath = ( String ) linkedTreeMap.get( "download_url" );
        filePathAndSha.sha = (String) linkedTreeMap.get( "sha" );
        return filePathAndSha;
    }

    private boolean showDialog()
    {
        final GenericDialog gd = new GenericDialog( "Save to github" );

        gd.addMessage( "To save directly to your github project, you will need a personal \n access token and push rights to the repository" );
        gd.addStringField( "GitHub access token", Prefs.get( ACCESS_TOKEN, "1234567890" ));
        gd.showDialog();

        if ( gd.wasCanceled() ) return false;

        accessToken = gd.getNextString();

        Prefs.set( ACCESS_TOKEN, accessToken );

        return true;
    }


    public void overwriteExistingFile( FilePathAndSha filePathAndSha, String jsonBase64String ) {
        final GitHubFileCommitter fileCommitter = new GitHubFileCommitter(
                    viewJsonGitLocation.repoUrl, accessToken, viewJsonGitLocation.branch,
                    viewJsonGitLocation.path, filePathAndSha.sha );
        fileCommitter.commitStringAsFile("Add new views from UI", jsonBase64String );
    }

    public void writeNewFile( String jsonBase64String ) {
        final GitHubFileCommitter fileCommitter = new GitHubFileCommitter(
                    viewJsonGitLocation.repoUrl, accessToken, viewJsonGitLocation.branch,
                    viewJsonGitLocation.path );
        fileCommitter.commitStringAsFile("Add new views from UI", jsonBase64String );
    }

    public void writeViewToDatasetJson( String viewName, View view ) {
        if ( showDialog() ) {
            final GitHubContentGetter contentGetter =
                    new GitHubContentGetter(viewJsonGitLocation.repoUrl, viewJsonGitLocation.path, viewJsonGitLocation.branch, null);

            Dataset dataset;
            try {
                String content = contentGetter.getContent();
                if (content != null) {
                    FilePathAndSha filePathAndSha = getFilePathAndSha(content);
                    dataset = new DatasetJsonParser().getDataset(filePathAndSha.filePath);
                    dataset.views.put(viewName, view);

                    final String datasetBase64String = writeDatasetToBase64String(dataset);
                    overwriteExistingFile(filePathAndSha, datasetBase64String);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void writeViewToViewsJson( String viewName, View view ) {
        if ( showDialog() ) {
            final GitHubContentGetter contentGetter =
                    new GitHubContentGetter(viewJsonGitLocation.repoUrl, viewJsonGitLocation.path, viewJsonGitLocation.branch, null);
            int responseCode = contentGetter.getContentResponseCode();

            AdditionalViews additionalViews;
            // if 404, then file doesn't exist, so make new one
            if (responseCode == 404) {
                additionalViews = new AdditionalViews();
                additionalViews.views = new HashMap<>();
                additionalViews.views.put(viewName, view);

                final String additionalViewsBase64String = writeAdditionalViewsToBase64String(additionalViews);
                writeNewFile(additionalViewsBase64String);
            // otherwise, append to file
            } else {
                try {
                    String content = contentGetter.getContent();
                    if (content != null) {
                        FilePathAndSha filePathAndSha = getFilePathAndSha(content);
                        additionalViews = new AdditionalViewsJsonParser().getViews(filePathAndSha.filePath);
                        additionalViews.views.put(viewName, view);

                        final String additionalViewsBase64String = writeAdditionalViewsToBase64String(additionalViews);
                        overwriteExistingFile(filePathAndSha, additionalViewsBase64String);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
