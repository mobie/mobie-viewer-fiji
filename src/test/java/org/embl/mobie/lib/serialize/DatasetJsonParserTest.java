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
package org.embl.mobie.lib.serialize;

import net.imagej.ImageJ;
import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.MoBIE;
import org.embl.mobie.MoBIESettings;
import org.embl.mobie.lib.create.JSONValidator;
import org.embl.mobie.lib.io.StorageLocation;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.embl.mobie.lib.create.JSONValidator.validate;

class DatasetJsonParserTest {

    private File tempDir;
    private static JSONObject datasetSchema;
    private Dataset dataset;
    private final String viewName = View.DEFAULT;
    private final String uiSelectionGroup = "bookmark";
    private final boolean isExclusive = true;
    private DatasetJsonParser datasetJsonParser;
    private String datasetJsonName = "dataset.json";


    @BeforeEach
    void setUp( @TempDir Path tempDir ) throws IOException {
        this.tempDir = tempDir.toFile();
        datasetJsonParser = new DatasetJsonParser();
        dataset = new Dataset();

        ImageDataSource imageSource = new ImageDataSource();
        StorageLocation storageLocation = new StorageLocation();
        storageLocation.relativePath = "an/example/path";
        imageSource.imageData = new HashMap<>();
        imageSource.imageData.put( ImageDataFormat.BdvN5, storageLocation);
        dataset.sources().put("testSource", imageSource );
    }


    //@Test // FIXME does that work on CI?
    public void savePlatyView() throws IOException
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();

        final MoBIE moBIE = new MoBIE("https://github.com/mobie/platybrowser-project", MoBIESettings.settings());

        // show a view with a segmentation and
        // selected cells that loads fast, to test saving
        final Map< String, View > views = moBIE.getViews();
        moBIE.getViewManager().show( views.get("Suppl. Fig. 2A: Neuron" ) );

        // grab the current view and save it
        View view = moBIE.getViewManager().createViewFromCurrentState();
        dataset.views().put( viewName, view );

        String datasetJSONPath = new File( tempDir, datasetJsonName ).getAbsolutePath();
        datasetJsonParser.saveDataset( dataset, datasetJSONPath );

        // FIXME this needs assertTrue
        validate(datasetJSONPath, JSONValidator.datasetSchemaURL);
    }
}
