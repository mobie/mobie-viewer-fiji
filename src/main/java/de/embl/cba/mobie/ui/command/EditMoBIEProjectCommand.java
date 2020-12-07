package de.embl.cba.mobie.ui.command;

import de.embl.cba.mobie.image.ImagePropertiesEditor;
import de.embl.cba.mobie.projects.ProjectsCreator;
import de.embl.cba.mobie.projects.ProjectsCreatorPanel;
import de.embl.cba.mobie.ui.viewer.MoBIEOptions;
import de.embl.cba.mobie.ui.viewer.MoBIEViewer;
import net.imagej.ImageJ;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;

@Plugin(type = Command.class, menuPath = "Plugins>MoBIE>Create>Edit MoBIE Project..." )
public class EditMoBIEProjectCommand implements Command
{
    @Parameter ( label = "Project Location", style="directory" )
    public File projectLocation;

    @Override
    public void run()
    {
        ProjectsCreatorPanel panel = new ProjectsCreatorPanel( projectLocation );
        panel.showProjectsCreatorPanel();
    }

    public static void main(final String... args)
    {
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();

        ProjectsCreatorPanel panel = new ProjectsCreatorPanel( new File("C:\\Users\\meechan\\Documents\\temp\\mobie_test\\ruse" ));
        panel.showProjectsCreatorPanel();
    }
}
