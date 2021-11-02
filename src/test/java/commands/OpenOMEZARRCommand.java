package commands;

import mpicbg.spim.data.SpimData;
import org.embl.mobie.io.ome.zarr.OMEZarrViewer;
import org.embl.mobie.io.ome.zarr.openers.OMEZarrOpener;

import java.io.IOException;

public class OpenOMEZARRCommand
{
    private static void openAndShow( String filePath ) throws IOException
    {
        SpimData spimData = OMEZarrOpener.openFile( filePath );
        final OMEZarrViewer viewer = new OMEZarrViewer( spimData );
        viewer.show();
    }

    public static void main( String[] args )
    {
        try {
            openAndShow( "/home/katerina/Documents/data/v0.3/zyx.ome.zarr" );
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }
}
