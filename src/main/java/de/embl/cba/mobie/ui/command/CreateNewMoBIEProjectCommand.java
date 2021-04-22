package de.embl.cba.mobie.ui.command;

import de.embl.cba.mobie.projects.projectsCreator.ui.ProjectsCreatorPanel;
import de.embl.cba.mobie.utils.Utils;
import net.imagej.ImageJ;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;

import static de.embl.cba.mobie.utils.Utils.tidyString;
import static de.embl.cba.mobie2.ui.UserInterfaceHelper.setMoBIESwingLookAndFeel;

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
                Utils.log("Project creation failed - this project already exists!");
            } else {
                File dataDirectory = new File(projectLocation, "data");
                dataDirectory.mkdirs();

                // using File script parameter changes the look and feel of swing, reset it to default here
                setMoBIESwingLookAndFeel();

                ProjectsCreatorPanel panel = new ProjectsCreatorPanel(projectLocation);
                panel.showProjectsCreatorPanel();
            }
        }

    }

    public static void main(final String... args)
    {
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();
    }

}
