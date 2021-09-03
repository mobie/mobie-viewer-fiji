package de.embl.cba.mobie.command;

import de.embl.cba.n5.ome.zarr.openers.OMEZarrS3Opener;
import net.imagej.ImageJ;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.IOException;

@Plugin(type = Command.class, menuPath = "Plugins>BigDataViewer>OME ZARR>Open OME ZARR From S3 (Advanced)..." )
public class OpenOMEZARRFromS3AdvancedCommand extends OpenOMEZARRFromS3Command
{
    @Parameter ( label = "Log chunks loading" )
    public boolean logChunkLoading = true;

    @Override
    public void run()
    {
        try
        {
            OMEZarrS3Opener.setLogChunkLoading( logChunkLoading );
            openAndShow( s3URL );
        }
        catch ( IOException e )
        {
            e.printStackTrace();
        }
    }

    public static void main( String[] args ) throws IOException
    {
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();

//		OMEZarrS3Reader.setLogChunkLoading( true );
//
//		// SpimData spimData = OMEZarrS3Reader.readURL( "https://s3.embl.de/i2k-2020/em-raw.ome.zarr" );
//		SpimData spimData = OMEZarrS3Reader.readURL( "https://s3.embassy.ebi.ac.uk/idr/zarr/v0.1/4495402.zarr" );
//
//		final OMEZarrViewer viewer = new OMEZarrViewer( spimData );
//		viewer.show();
    }
}
