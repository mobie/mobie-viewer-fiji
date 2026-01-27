package org.embl.mobie.command.create;

import ij.IJ;
import ij.ImagePlus;
import net.imagej.ImageJ;
import org.embl.mobie.command.open.OpenOMEZARRCommand;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class SaveAsOmeZarrCommandTest
{
    public static void main( String[] args ) throws IOException, ExecutionException, InterruptedException
    {
        save();
    }

    private static void save()
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();

        File file = new File( "src/test/resources/collections/mri-stack.tif" );
        ImagePlus imagePlus = IJ.openImage( file.getAbsolutePath() );

        SaveAsOMEZarrCommand saveCommand = new SaveAsOMEZarrCommand();
        saveCommand.imp = imagePlus;
        saveCommand.imageName = "mri-stack";
        saveCommand.imageType = SaveAsOMEZarrCommand.INTENSITY;
        saveCommand.overwrite = true;
        saveCommand.chunkSizeMB = 50;
        saveCommand.outputFolder = new File("src/test/resources");
        saveCommand.run();

        OpenOMEZARRCommand openCommand = new OpenOMEZARRCommand();
        openCommand.containerUri = new File( saveCommand.outputFolder,saveCommand.imageName + ".ome.zarr" ).getAbsolutePath();
        openCommand.run();
    }
}