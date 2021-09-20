package commands;

import de.embl.cba.n5.ome.zarr.OMEZarrViewer;
import de.embl.cba.n5.ome.zarr.openers.OMEZarrOpener;
import mpicbg.spim.data.SpimData;

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
