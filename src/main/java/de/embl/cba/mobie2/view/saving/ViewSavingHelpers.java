package de.embl.cba.mobie2.view.saving;

import de.embl.cba.mobie2.Dataset;
import de.embl.cba.mobie2.serialize.AdditionalViewsJsonParser;
import de.embl.cba.mobie2.serialize.DatasetJsonParser;
import de.embl.cba.mobie2.view.View;
import de.embl.cba.mobie2.view.additionalviews.AdditionalViews;
import de.embl.cba.tables.github.GitHubUtils;

import java.io.IOException;

import static de.embl.cba.mobie2.PathHelpers.isGithub;

public class ViewSavingHelpers {
    public static void writeDatasetJson( Dataset dataset, View view, String viewName, String datasetJsonPath ) throws IOException {
        if ( viewName != null ) {
            dataset.views.put(viewName, view);

            if (isGithub(datasetJsonPath)) {
                new ViewsGithubWriter(GitHubUtils.rawUrlToGitLocation(datasetJsonPath)).writeViewToDatasetJson(viewName, view);
            } else {
                new DatasetJsonParser().saveDataset(dataset, datasetJsonPath);
            }
        }
    }

    public static void writeAdditionalViewsJson( AdditionalViews additionalViews, View view, String viewName,
                                                 String additionalViewsJsonPath ) throws IOException {
        if ( viewName != null ) {
            additionalViews.views.put(viewName, view);

            if (isGithub( additionalViewsJsonPath )) {
                new ViewsGithubWriter( GitHubUtils.rawUrlToGitLocation( additionalViewsJsonPath ) ).writeViewToViewsJson( viewName, view );
            } else {
                new AdditionalViewsJsonParser().saveViews(additionalViews, additionalViewsJsonPath );
            }
        }
    }
}
