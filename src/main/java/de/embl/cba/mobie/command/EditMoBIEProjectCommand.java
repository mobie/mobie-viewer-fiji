package de.embl.cba.mobie.command;

import de.embl.cba.mobie.projectcreator.ui.ProjectsCreatorPanel;
import ij.IJ;
import net.imagej.ImageJ;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;
import java.io.IOException;

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

        if ( !projectLocation.exists() ) {
            IJ.log( "Edit project failed - MoBIE project does not exist!" );
        } else {
            try {
                ProjectsCreatorPanel panel = new ProjectsCreatorPanel( projectLocation );
                panel.showProjectsCreatorPanel();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(final String... args)
    {
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();
    }
}
