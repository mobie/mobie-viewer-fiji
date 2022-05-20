package org.embl.mobie.viewer.projectcreator;

import org.embl.mobie.io.util.IOHelper;
import org.embl.mobie.viewer.Dataset;
import org.embl.mobie.viewer.Project;
import org.embl.mobie.viewer.serialize.DatasetJsonParser;
import org.embl.mobie.viewer.serialize.ProjectJsonParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatasetsCreatorTest {

    private ProjectCreator projectCreator;
    private DatasetsCreator datasetsCreator;

    @BeforeEach
    void setUp( @TempDir Path tempDir ) throws IOException {
        projectCreator = new ProjectCreator( tempDir.toFile() );
        datasetsCreator = projectCreator.getDatasetsCreator();
    }

    @Test
    void addDataset() throws IOException {
        String datasetName = "test";
        datasetsCreator.addDataset(datasetName, false);

        Project project = new ProjectJsonParser().parseProject( projectCreator.getProjectJson().getAbsolutePath() );
        assertEquals(project.getDatasets().size(), 1);
        assertEquals(project.getDatasets().get(0), datasetName);
        assertTrue(new File(projectCreator.getProjectLocation(), datasetName).exists());
    }

    @Test
    void renameDataset() throws IOException {
        String oldDatasetName = "test";
        String newDatasetName = "newName";
        datasetsCreator.addDataset(oldDatasetName, false);
        datasetsCreator.renameDataset(oldDatasetName, newDatasetName);

        Project project = new ProjectJsonParser().parseProject( projectCreator.getProjectJson().getAbsolutePath() );
        assertEquals(project.getDatasets().size(), 1);
        assertEquals(project.getDatasets().get(0), newDatasetName);
        assertTrue(new File(projectCreator.getProjectLocation(), newDatasetName).exists());
        assertFalse(new File(projectCreator.getProjectLocation(), oldDatasetName).exists());
    }

    @Test
    void makeDefaultDataset() throws IOException {
        String dataset1Name = "dataset1";
        String dataset2Name = "dataset2";
        datasetsCreator.addDataset(dataset1Name, false);
        datasetsCreator.addDataset(dataset2Name, false);

        Project project;
        project = new ProjectJsonParser().parseProject( projectCreator.getProjectJson().getAbsolutePath() );
        assertEquals( project.getDefaultDataset(), dataset1Name );

        datasetsCreator.makeDefaultDataset( dataset2Name );

        project = new ProjectJsonParser().parseProject( projectCreator.getProjectJson().getAbsolutePath() );
        assertEquals( project.getDefaultDataset(), dataset2Name );
    }

    @Test
    void makeDataset2D() throws IOException {
        String datasetName = "test";
        String datasetJsonPath = IOHelper.combinePath( projectCreator.getProjectLocation().getAbsolutePath(),
                datasetName, "dataset.json" );

        Dataset dataset;
        datasetsCreator.addDataset(datasetName, false);
        dataset = new DatasetJsonParser().parseDataset( datasetJsonPath );
        assertFalse( dataset.is2D );

        datasetsCreator.makeDataset2D(datasetName, true);
        dataset = new DatasetJsonParser().parseDataset( datasetJsonPath );
        assertTrue( dataset.is2D );
    }
}