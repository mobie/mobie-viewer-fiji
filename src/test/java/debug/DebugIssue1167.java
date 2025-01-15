package debug;

import net.imagej.ImageJ;
import org.embl.mobie.command.open.OpenImageAndLabelsCommand;
import org.embl.mobie.command.open.OpenMultipleImagesAndLabelsCommand;

import java.io.File;

public class DebugIssue1167
{
    public static void main( String[] args )
    {
        new ImageJ().ui().showUI();
        final OpenImageAndLabelsCommand command = new OpenImageAndLabelsCommand();
        command.image = "/Users/tischer/Downloads/20240524_1_s2.zarr";
        command.run();
    }
}
