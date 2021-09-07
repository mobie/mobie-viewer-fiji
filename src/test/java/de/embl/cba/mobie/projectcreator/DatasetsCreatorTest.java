package de.embl.cba.mobie.projectcreator;

import de.embl.cba.mobie.Project;
import de.embl.cba.mobie.serialize.ProjectJsonParser;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class DatasetsCreatorTest {

    private ProjectCreator projectCreator;
    private DatasetsCreator datasetsCreator;

    @org.junit.jupiter.api.BeforeEach
    void setUp( @TempDir Path tempDir ) {
        projectCreator = new ProjectCreator(tempDir);
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
    void renameDataset() {
    }

    @org.junit.jupiter.api.Test
    void makeDefaultDataset() {
    }
}