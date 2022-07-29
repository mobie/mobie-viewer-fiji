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
package org.embl.mobie.viewer.view.save;

import com.google.gson.GsonBuilder;
import com.google.gson.internal.LinkedTreeMap;
import ij.IJ;
import ij.Prefs;
import ij.gui.GenericDialog;
import org.embl.mobie.viewer.serialize.AdditionalViewsJsonParser;
import org.embl.mobie.viewer.serialize.Dataset;
import org.embl.mobie.viewer.serialize.DatasetJsonParser;
import org.embl.mobie.viewer.view.AdditionalViews;
import org.embl.mobie.viewer.view.View;
import org.embl.mobie.io.github.GitHubContentGetter;
import org.embl.mobie.io.github.GitHubFileCommitter;
import org.embl.mobie.io.github.GitLocation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;

public class ViewsGithubWriter {

    static { net.imagej.patcher.LegacyInjector.preinit(); }

    public static final String ACCESS_TOKEN = "MoBIE.GitHub access token";
    private String accessToken;
    private GitLocation viewJsonGitLocation;

    ViewsGithubWriter( GitLocation viewJsonGitLocation ) {
        this.viewJsonGitLocation = viewJsonGitLocation;

        // Git location should be a path to a json file, so remove any trailing /
        String path = this.viewJsonGitLocation.path;
        if ( path.endsWith("/") ) {
            this.viewJsonGitLocation.path = path.substring(0, path.length() - 1);
        }
    }

    private static String writeAdditionalViewsToBase64String ( AdditionalViews additionalViews ) {
        String jsonString = new AdditionalViewsJsonParser().viewsToJsonString( additionalViews, true );
        jsonString += "\n";
        byte[] jsonBytes = jsonString.getBytes( StandardCharsets.UTF_8);
        return Base64.getEncoder().encodeToString(jsonBytes);
    }

    private static String writeDatasetToBase64String ( Dataset dataset ) {
        String jsonString = new DatasetJsonParser().datasetToJsonString( dataset, true );
        jsonString += "\n";
        byte[] jsonBytes = jsonString.getBytes( StandardCharsets.UTF_8);
        return Base64.getEncoder().encodeToString(jsonBytes);
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
            if ( viewJsonGitLocation.path.endsWith( "dataset.json") ) {
                final GitHubContentGetter contentGetter =
                        new GitHubContentGetter(viewJsonGitLocation.repoUrl, viewJsonGitLocation.path, viewJsonGitLocation.branch, null);

                Dataset dataset;
                try {
                    String content = contentGetter.getContent();
                    if (content != null) {
                        FilePathAndSha filePathAndSha = getFilePathAndSha(content);
                        dataset = new DatasetJsonParser().parseDataset(filePathAndSha.filePath);
                        dataset.views.put(viewName, view);

                        final String datasetBase64String = writeDatasetToBase64String(dataset);
                        overwriteExistingFile(filePathAndSha, datasetBase64String);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                IJ.log( "Aborting saving view to github - path is not a dataset.json" );
            }
        }
    }

    public boolean jsonExists() {
        final GitHubContentGetter contentGetter =
                new GitHubContentGetter(viewJsonGitLocation.repoUrl, viewJsonGitLocation.path, viewJsonGitLocation.branch, null);
        int responseCode = contentGetter.getContentResponseCode();
        if ( responseCode == 404 ) {
            return false;
        } else {
            return true;
        }
    }

    public void writeViewToViewsJson( String viewName, View view ) {
        if ( showDialog() ) {
            if ( viewJsonGitLocation.path.endsWith( ".json" )) {
                AdditionalViews additionalViews;
                // if 404, then file doesn't exist, so make new one
                if ( !jsonExists() ) {
                    additionalViews = new AdditionalViews();
                    additionalViews.views = new HashMap<>();
                    additionalViews.views.put(viewName, view);

                    final String additionalViewsBase64String = writeAdditionalViewsToBase64String(additionalViews);
                    writeNewFile(additionalViewsBase64String);
                    // otherwise, append to file
                } else {
                    try {
                        final GitHubContentGetter contentGetter =
                                new GitHubContentGetter(viewJsonGitLocation.repoUrl, viewJsonGitLocation.path, viewJsonGitLocation.branch, null);
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
            } else {
                IJ.log( "Aborting saving view to github - path is not a .json" );
            }
        }
    }

}
