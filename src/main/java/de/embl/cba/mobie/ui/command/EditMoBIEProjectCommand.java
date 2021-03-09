package de.embl.cba.mobie.ui.command;

import de.embl.cba.mobie.projects.projectsCreator.ui.ProjectsCreatorPanel;
import net.imagej.ImageJ;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;

import static de.embl.cba.mobie.utils.ui.SwingUtils.resetSwingLookAndFeel;

@Plugin(type = Command.class, menuPath = "Plugins>MoBIE>Create>Edit MoBIE Project..." )
public class EditMoBIEProjectCommand implements Command
{
    @Parameter ( label = "Project Location", style="directory" )
    public File projectLocation;

    @Override
    public void run()
    {
        // using File script parameter changes the look and feel of swing, reset it to default here
        resetSwingLookAndFeel();

        ProjectsCreatorPanel panel = new ProjectsCreatorPanel( projectLocation );
        panel.showProjectsCreatorPanel();
    }

    public static void main(final String... args)
    {
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();
    }
}
