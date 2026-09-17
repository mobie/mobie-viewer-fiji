package org.embl.mobie.command.create;

import ij.IJ;
import ij.ImagePlus;
import net.imagej.ImageJ;
import net.imglib2.type.numeric.NumericType;
import org.embl.mobie.command.open.OpenOMEZARRCommand;
import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.io.ImageDataOpener;
import org.embl.mobie.io.imagedata.ImageData;
import org.embl.mobie.lib.util.ThreadHelper;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.ExecutionException;

public class SaveAsOmeZarrCommandTest
{
    public static void main( String[] args ) throws IOException, ExecutionException, InterruptedException, URISyntaxException
    {
        saveZarr2();
        //saveZarr3();
    }

    private static void saveZarr2() throws URISyntaxException
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();

        ImagePlus imagePlus = getImagePlus();

        SaveAsOMEZarrCommand saveCommand = new SaveAsOMEZarrCommand();
        saveCommand.imp = imagePlus;
        saveCommand.imageName = "mri-stack-zarr2";
        saveCommand.imageType = SaveAsOMEZarrCommand.INTENSITY;
        saveCommand.overwrite = true;
        saveCommand.chunkSizeMB = 50;
        saveCommand.shardSizeMB = -1;
        saveCommand.outputFolder = new File("src/test/resources");
        saveCommand.run();

        String zarrPath = new File( saveCommand.outputFolder, saveCommand.imageName + ".ome.zarr" ).getAbsolutePath();
        ImageData< ? > imageData = ImageDataOpener.open( zarrPath, ImageDataFormat.OmeZarr, ThreadHelper.sharedQueue );
        int numDatasets = imageData.getNumDatasets();
        NumericType< ? > type = imageData.getSourcePair( 0 ).getA().getSource( 0, 0 ).cursor().next();
    }

    private static void saveZarr3() throws URISyntaxException
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();

        ImagePlus imagePlus = getImagePlus();

        SaveAsOMEZarrCommand saveCommand = new SaveAsOMEZarrCommand();
        saveCommand.imp = imagePlus;
        saveCommand.imageName = "mri-stack-zarr3";
        saveCommand.imageType = SaveAsOMEZarrCommand.INTENSITY;
        saveCommand.overwrite = true;
        saveCommand.chunkSizeMB = 10;
        saveCommand.shardSizeMB = 50;
        saveCommand.outputFolder = new File("src/test/resources");
        saveCommand.run();

        OpenOMEZARRCommand openCommand = new OpenOMEZARRCommand();
        openCommand.containerUri = new File( saveCommand.outputFolder,saveCommand.imageName + ".ome.zarr" ).getAbsolutePath();
        openCommand.run();
    }

    private static ImagePlus getImagePlus() throws URISyntaxException
    {
        URL resource = SaveAsOmeZarrCommandTest.class.getClassLoader().getResource( "collections/mri-stack.tif" );
        URI uri = resource.toURI();
        File file = new File( uri );
        ImagePlus imagePlus = IJ.openImage( file.getAbsolutePath() );
        return imagePlus;
    }
}