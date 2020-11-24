package de.embl.cba.mobie.ui.command;

import de.embl.cba.mobie.ui.viewer.MoBIEOptions;
import de.embl.cba.mobie.ui.viewer.MoBIEViewer;
import ij.IJ;
import net.imagej.ImageJ;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;

@Plugin(type = Command.class, menuPath = "Plugins>MoBIE>Create>Create new MoBIE Project..." )
public class CreateNewMoBIEProjectCommand implements Command {

    @Parameter( label = "Project Location", style="directory" )
    public File projectLocation;


    @Override
    public void run()
    {
        File dataDirectory = new File( projectLocation, "data");
        if ( !dataDirectory.exists() ) {
            dataDirectory.mkdirs();
        }
        IJ.log(projectLocation.toString());
    }

    public static void main(final String... args)
    {
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();



    }

}
