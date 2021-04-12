package de.embl.cba.mobie.ui.command;

import de.embl.cba.mobie.projects.projectsCreator.ui.ProjectsCreatorPanel;
import de.embl.cba.mobie.utils.Utils;
import net.imagej.ImageJ;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;

import static de.embl.cba.mobie2.ui.UserInterfaceHelper.setSwingLookAndFeel;
import static org.scijava.ItemVisibility.MESSAGE;

@Plugin(type = Command.class, menuPath = "Plugins>MoBIE>Create>Edit MoBIE Project..." )
public class EditMoBIEProjectCommand implements Command
{
    @Parameter( visibility=MESSAGE, required=false )
    String message = "Choose a MoBIE project folder...";

    @Parameter ( label="MoBIE folder:", style="directory" )
    public File projectLocation;

    @Override
    public void run()
    {
        // using File script parameter changes the look and feel of swing, reset it to default here
        setSwingLookAndFeel();

        File dataLocation = new File( projectLocation, "data" );

        if ( !projectLocation.exists() ) {
            Utils.log( "Edit project failed - MoBIE project does not exist!" );
        } else if ( !dataLocation.exists() ) {
            Utils.log( "Edit project failed - this folder does not contain a valid MoBIE project structure. \n " +
                    "Please choose a MoBIE project folder (this contains a 'data' folder at the top level)" );
        } else {
            ProjectsCreatorPanel panel = new ProjectsCreatorPanel(projectLocation);
            panel.showProjectsCreatorPanel();
        }
    }

    public static void main(final String... args)
    {
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();
    }
}
