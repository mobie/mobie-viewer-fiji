package org.embl.mobie.viewer.command;

import org.embl.mobie.viewer.projectcreator.ui.ProjectsCreatorPanel;
import ij.IJ;
import net.imagej.ImageJ;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;
import java.io.IOException;

import static org.embl.mobie.viewer.ui.UserInterfaceHelper.*;

@Plugin(type = Command.class, menuPath = "Plugins>MoBIE>Create>Create new MoBIE Project..." )
public class CreateNewMoBIEProjectCommand implements Command {

    @Parameter( label= "Choose a project name:")
    public String projectName;

    @Parameter( label = "Choose a folder to save your project in:", style="directory" )
    public File folderLocation;


    @Override
    public void run()
    {
        String tidyProjectName = tidyString( projectName );
        if ( tidyProjectName != null ) {
            File projectLocation = new File(folderLocation, tidyProjectName);

            if ( projectLocation.exists() ) {
                IJ.log("Project creation failed - this project already exists!");
            } else {
                File dataDirectory = new File(projectLocation, "data");
                dataDirectory.mkdirs();

                try {
                    ProjectsCreatorPanel panel = new ProjectsCreatorPanel( projectLocation );
                    panel.showProjectsCreatorPanel();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public static void main(final String... args)
    {
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();
    }

}
