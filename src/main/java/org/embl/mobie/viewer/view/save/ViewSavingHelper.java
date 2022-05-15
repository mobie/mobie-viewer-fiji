package org.embl.mobie.viewer.view.save;

import org.embl.mobie.viewer.Dataset;
import org.embl.mobie.viewer.serialize.AdditionalViewsJsonParser;
import org.embl.mobie.viewer.serialize.DatasetJsonParser;
import org.embl.mobie.viewer.view.View;
import org.embl.mobie.viewer.view.AdditionalViews;
import de.embl.cba.tables.github.GitHubUtils;

import java.io.IOException;

import static de.embl.cba.tables.github.GitHubUtils.isGithub;

public class ViewSavingHelper
{
    public static void writeDatasetJson(Dataset dataset, View view, String viewName, String datasetJsonPath ) throws IOException {
        if ( viewName != null ) {
            dataset.views.put(viewName, view);

            if ( isGithub(datasetJsonPath)) {
                new ViewsGithubWriter(GitHubUtils.rawUrlToGitLocation(datasetJsonPath)).writeViewToDatasetJson(viewName, view);
            } else {
                new DatasetJsonParser().saveDataset(dataset, datasetJsonPath);
            }
        }
    }

    public static void writeAdditionalViewsJson( AdditionalViews additionalViews, View view, String viewName, String additionalViewsJsonPath ) throws IOException {
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
