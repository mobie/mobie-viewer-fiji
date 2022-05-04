package org.embl.mobie.viewer.serialize;

import de.embl.cba.tables.FileAndUrlUtils;
import net.imagej.ImageJ;
import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.viewer.Dataset;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.MoBIESettings;
import org.embl.mobie.viewer.source.ImageSource;
import org.embl.mobie.viewer.source.SourceSupplier;
import org.embl.mobie.viewer.source.StorageLocation;
import org.embl.mobie.viewer.view.View;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

class DatasetJsonParserTest {

    private File tempDir;
    private static JSONObject datasetSchema;
    private Dataset dataset;
    private final String viewName = "default";
    private final String uiSelectionGroup = "bookmark";
    private final boolean isExclusive = true;
    private DatasetJsonParser datasetJsonParser;
    private String datasetJsonName = "dataset.json";

    @BeforeAll
    static void downloadSchema() throws IOException {
        try( InputStream schemaInputStream = FileAndUrlUtils.getInputStream(
                "https://raw.githubusercontent.com/mobie/mobie.github.io/master/schema/dataset.schema.json") ) {
            datasetSchema = new JSONObject(new JSONTokener(schemaInputStream));
        }
    }

    @BeforeEach
    void setUp( @TempDir Path tempDir ) throws IOException {
        this.tempDir = tempDir.toFile();
        datasetJsonParser = new DatasetJsonParser();
        dataset = new Dataset();
        dataset.sources = new HashMap<>();

        ImageSource imageSource = new ImageSource();
        StorageLocation storageLocation = new StorageLocation();
        storageLocation.relativePath = "an/example/path";
        imageSource.imageData = new HashMap<>();
        imageSource.imageData.put( ImageDataFormat.BdvN5, storageLocation);
        SourceSupplier sourceSupplier = new SourceSupplier( imageSource );

        dataset.sources.put("testSource", sourceSupplier );

    }

    void validateJSON( String jsonPath ) throws IOException {

        try( InputStream jsonInputStream = new FileInputStream( jsonPath ) ) {
            JSONObject jsonSubject = new JSONObject(new JSONTokener(jsonInputStream));

            // library only supports up to draft 7 json schema - specify here, otherwise errors when reads 2020-12 in
            // the schema file
            SchemaLoader loader = SchemaLoader.builder()
                    .schemaJson(datasetSchema)
                    .draftV7Support()
                    .build();
            Schema schema = loader.load().build();

            schema.validate(jsonSubject);
        }
    }

    @Test
    public void savePlatyView() throws IOException
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();

        final MoBIE moBIE = new MoBIE("https://github.com/mobie/platybrowser-project", MoBIESettings.settings());

        // show a view with a segmentation and selected cells that loads fast, to test saving
        final Map< String, View > views = moBIE.getViews();
        moBIE.getViewManager().show( views.get("Suppl. Fig. 2A: Neuron" ) );

        // grab the current view and save it
        View view  = moBIE.getViewManager().getCurrentView( uiSelectionGroup, isExclusive, true );
        dataset.views = new HashMap<>();
        dataset.views.put( viewName, view );

        String jsonPath = new File( tempDir, datasetJsonName ).getAbsolutePath();
        datasetJsonParser.saveDataset( dataset, jsonPath );

        try {
            validateJSON(jsonPath);
        } catch ( ValidationException e ) {
            // print details of individual errors - so error message is more informative, then re-throw the exception
            System.out.println(e.getMessage());
            e.getCausingExceptions().stream()
                    .map(ValidationException::getMessage)
                    .forEach(System.out::println);
            throw e;
        }
    }
}