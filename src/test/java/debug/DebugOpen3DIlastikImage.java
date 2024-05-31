package debug;

import net.imagej.ImageJ;
import org.embl.mobie.command.open.OpenImageAndLabelsFilesCommand;

import java.io.File;

public class DebugOpen3DIlastikImage
{
    public static void main( String[] args )
    {
        new ImageJ().ui().showUI();
        OpenImageAndLabelsFilesCommand command = new OpenImageAndLabelsFilesCommand();
        command.image = new File("/Users/tischer/Downloads/export.h5");
        command.run();
    }
}
