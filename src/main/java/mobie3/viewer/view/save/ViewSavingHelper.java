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
package mobie3.viewer.view.save;

import mobie3.viewer.serialize.AdditionalViewsJsonParser;
import mobie3.viewer.serialize.Dataset;
import mobie3.viewer.serialize.DatasetJsonParser;
import mobie3.viewer.view.AdditionalViews;
import mobie3.viewer.view.View;
import org.embl.mobie.io.github.GitHubUtils;

import java.io.IOException;

import static de.embl.cba.tables.github.GitHubUtils.isGithub;

public class ViewSavingHelper
{
    public static void writeDatasetJson( Dataset dataset, View view, String viewName, String datasetJsonPath ) throws IOException {
        if ( viewName != null ) {
            dataset.views.put(viewName, view);

            if ( isGithub(datasetJsonPath)) {
                new ViewsGithubWriter( GitHubUtils.rawUrlToGitLocation(datasetJsonPath)).writeViewToDatasetJson(viewName, view);
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
