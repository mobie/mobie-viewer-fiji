package commands;

import net.imagej.ImageJ;

import java.io.IOException;

public class OpenOMEZARRFromS3AdvancedCommand
{

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
