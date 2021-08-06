package de.embl.cba.mobie.n5.zarr;

import com.google.gson.GsonBuilder;
import de.embl.cba.mobie.MoBIE;
import ij.IJ;
import mpicbg.spim.data.SpimData;
import net.imglib2.util.Cast;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class OMEZarrReader
{
    private static boolean logChunkLoading;

    private final String filePath;
    ;

    public OMEZarrReader(String filePath)
    {
        this.filePath = filePath;
    }

    public static void setLogChunkLoading( boolean logChunkLoading )
    {
        OMEZarrReader.logChunkLoading = logChunkLoading;
        if ( logChunkLoading ) IJ.run("Console");
    }

    public static SpimData openFile(String filePath) throws IOException
    {
        N5OMEZarrImageLoader.logChunkLoading = logChunkLoading;
        OMEZarrReader omeZarrReader = new OMEZarrReader(filePath);
        return omeZarrReader.readFile();
    }

    private SpimData readFile() throws IOException
    {
        N5OMEZarrImageLoader.logChunkLoading = logChunkLoading;
        N5OmeZarrReader reader = new N5OmeZarrReader(this.filePath, new GsonBuilder());
        HashMap<String, Integer> axesMap = reader.getAxes();
        N5OMEZarrImageLoader imageLoader = new N5OMEZarrImageLoader(reader, axesMap, MoBIE.sharedQueue);
        return new SpimData(
                new File(this.filePath),
                Cast.unchecked( imageLoader.getSequenceDescription() ),
                imageLoader.getViewRegistrations());
    }

    private boolean isV3OMEZarr(){
        return true;
    }
}
