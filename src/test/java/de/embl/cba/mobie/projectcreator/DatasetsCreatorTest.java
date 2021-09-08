package de.embl.cba.mobie.projectcreator;

import de.embl.cba.mobie.Project;
import de.embl.cba.mobie.serialize.ProjectJsonParser;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class DatasetsCreatorTest {

    private ProjectCreator projectCreator;
    private DatasetsCreator datasetsCreator;

    @org.junit.jupiter.api.BeforeEach
    void setUp( @TempDir Path tempDir ) throws IOException {
        projectCreator = new ProjectCreator( tempDir.toFile() );
        datasetsCreator = projectCreator.getDatasetsCreator();
    }

    @org.junit.jupiter.api.Test
    void addDataset() throws IOException {
        String datasetName = "test";
        datasetsCreator.addDataset(datasetName);

        Project project = new ProjectJsonParser().parseProject( projectCreator.getProjectJson().getAbsolutePath() );
        assertEquals(project.getDatasets().size(), 1);
        assertEquals(project.getDatasets().get(0), datasetName);
        assertTrue(new File(projectCreator.getDataLocation(), datasetName).exists());
    }

    @org.junit.jupiter.api.Test
    void renameDataset() throws IOException {
        String oldDatasetName = "test";
        String newDatasetName = "newName";
        datasetsCreator.addDataset(oldDatasetName);
        datasetsCreator.renameDataset(oldDatasetName, newDatasetName);

        Project project = new ProjectJsonParser().parseProject( projectCreator.getProjectJson().getAbsolutePath() );
        assertEquals(project.getDatasets().size(), 1);
        assertEquals(project.getDatasets().get(0), newDatasetName);
        assertTrue(new File(projectCreator.getDataLocation(), newDatasetName).exists());
        assertFalse(new File(projectCreator.getDataLocation(), oldDatasetName).exists());
    }

    @org.junit.jupiter.api.Test
    void makeDefaultDataset() throws IOException {
        String dataset1Name = "dataset1";
        String dataset2Name = "dataset2";
        datasetsCreator.addDataset(dataset1Name);
        datasetsCreator.addDataset(dataset2Name);

        Project project;
        project = new ProjectJsonParser().parseProject( projectCreator.getProjectJson().getAbsolutePath() );
        assertEquals( project.getDefaultDataset(), dataset1Name );

        datasetsCreator.makeDefaultDataset( dataset2Name );

        project = new ProjectJsonParser().parseProject( projectCreator.getProjectJson().getAbsolutePath() );
        assertEquals( project.getDefaultDataset(), dataset2Name );
    }
}