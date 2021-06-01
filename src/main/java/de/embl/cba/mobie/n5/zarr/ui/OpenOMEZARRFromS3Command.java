package de.embl.cba.mobie.n5.zarr.ui;

import de.embl.cba.mobie.n5.zarr.OMEZarrS3Reader;
import de.embl.cba.mobie.n5.zarr.OMEZarrViewer;
import mpicbg.spim.data.SpimData;
import net.imagej.ImageJ;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.IOException;


@Plugin(type = Command.class, menuPath = "Plugins>BigDataViewer>OME ZARR>Open OME ZARR From S3..." )
public class OpenOMEZARRFromS3Command implements Command
{
	@Parameter ( label = "S3 URL" )
	public String s3URL = "https://s3.embl.de/i2k-2020/em-raw.ome.zarr";

	@Override
	public void run()
	{
		try
		{
			openAndShow( s3URL );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
	}

	protected static void openAndShow( String s3URL ) throws IOException
	{
		SpimData spimData = OMEZarrS3Reader.readURL( s3URL );
		final OMEZarrViewer viewer = new OMEZarrViewer( spimData );
		viewer.show();
	}

	public static void main( String[] args ) throws IOException
	{
		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();

		openAndShow( "https://s3.embl.de/i2k-2020/em-raw.ome.zarr" );
		//openAndShow( "https://s3.embassy.ebi.ac.uk/idr/zarr/v0.1/bbbb.zarr" );
	}
}
