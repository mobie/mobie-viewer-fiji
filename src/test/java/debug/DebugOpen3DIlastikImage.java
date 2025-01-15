package debug;

import net.imagej.ImageJ;
import org.embl.mobie.command.open.OpenImageAndLabelsCommand;

import java.io.File;

public class DebugOpen3DIlastikImage
{
    public static void main( String[] args )
    {
        new ImageJ().ui().showUI();
        OpenImageAndLabelsCommand command = new OpenImageAndLabelsCommand();
        command.image = "/Users/tischer/Downloads/export.h5";
        command.run();
    }
}
