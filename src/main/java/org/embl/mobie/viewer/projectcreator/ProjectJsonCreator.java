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
package org.embl.mobie.viewer.projectcreator;

import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.viewer.Project;
import org.embl.mobie.viewer.serialize.ProjectJsonParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ProjectJsonCreator {
    ProjectCreator projectCreator;

    public ProjectJsonCreator( ProjectCreator projectCreator ) {
        this.projectCreator = projectCreator;
    }

    public void addImageDataFormat( ImageDataFormat imageDataFormat ) {
        Project project = projectCreator.getProject();
        if ( project.getImageDataFormats() == null ) {
            project.setImageDataFormats( new ArrayList<>() );
        }

        List< ImageDataFormat > imageDataFormats = project.getImageDataFormats();
        if ( !imageDataFormats.contains( imageDataFormat ) ) {
            imageDataFormats.add( imageDataFormat );
            writeProjectJson();
        }
    }

    public void removeImageDataFormat( ImageDataFormat imageDataFormat ) {
        List<ImageDataFormat> imageDataFormats = projectCreator.getProject().getImageDataFormats();
        if ( imageDataFormats.contains( imageDataFormat ) ) {
            imageDataFormats.remove( imageDataFormat );
            writeProjectJson();
        }
    }

    public void addDataset( String datasetName ) {
        List<String> currentDatasets = projectCreator.getProject().getDatasets();

        // if this is the first dataset, then make this the default
        if ( currentDatasets.size() == 0 ) {
            projectCreator.getProject().setDefaultDataset( datasetName );
        }
        currentDatasets.add( datasetName );
        writeProjectJson();
    }

    public void renameDataset( String oldName, String newName ) {
        if ( projectCreator.getProject().getDefaultDataset().equals(oldName) ) {
            projectCreator.getProject().setDefaultDataset( newName );
        }

        int indexOld = projectCreator.getProject().getDatasets().indexOf(oldName);
        projectCreator.getProject().getDatasets().set(indexOld, newName);

        writeProjectJson();
    }

    public void setDefaultDataset( String datasetName ) {
        projectCreator.getProject().setDefaultDataset( datasetName );
        writeProjectJson();
    }

    public void writeProjectJson() {
        try {
            new ProjectJsonParser().saveProject( projectCreator.getProject(), projectCreator.getProjectJson().getAbsolutePath() );
        } catch (IOException e) {
            e.printStackTrace();
        }

        // whether the project writing succeeded or not, we now read the current state of the project
        try {
            projectCreator.reloadProject();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
