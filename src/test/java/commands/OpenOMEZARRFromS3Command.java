package commands;

import de.embl.cba.n5.ome.zarr.OMEZarrViewer;
import de.embl.cba.n5.ome.zarr.openers.OMEZarrS3Opener;
import mpicbg.spim.data.SpimData;

import java.io.IOException;

public class OpenOMEZARRFromS3Command
{
    protected static void openAndShow( String s3URL ) throws IOException
    {
        SpimData spimData = OMEZarrS3Opener.readURL( s3URL );
        final OMEZarrViewer viewer = new OMEZarrViewer( spimData );
        viewer.show();
    }

    public static void main( String[] args ) throws IOException
    {
        try {
            openAndShow( "https://s3.embl.de/i2k-2020/em-raw.ome.zarr" );
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }
}
