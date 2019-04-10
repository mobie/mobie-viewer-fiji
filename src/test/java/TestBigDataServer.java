import bdv.BigDataViewer;
import bdv.ij.util.ProgressWriterIJ;
import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;
import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import bdv.viewer.ViewerOptions;
import de.embl.cba.platynereis.remote.RemoteUtils;
import ij.ImagePlus;
import mpicbg.spim.data.SpimDataException;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class TestBigDataServer
{
	public static void main( String[] args ) throws IOException, SpimDataException
	{
		final Map< String, String > datasetUrlMap
				= RemoteUtils.getDatasetUrlMap( "http://10.11.4.195:8000" );

		Bdv bdv = null;

		for ( String key : datasetUrlMap.keySet() )
		{
			final String filename = datasetUrlMap.get( key );
			final String title = new File( filename ).getName();
			final SpimDataMinimal spimData = new XmlIoSpimDataMinimal().load( filename );

			bdv = BdvFunctions.show( spimData, BdvOptions.options().addTo( bdv ) ).get( 0 ).getBdvHandle();
		}

	}
}
