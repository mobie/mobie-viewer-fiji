package playground;

import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;
import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvOptions;
import de.embl.cba.platynereis.remote.RemoteUtils;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;

import java.io.File;
import java.io.IOException;
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
			final SpimData spimData = new XmlIoSpimData().load( filename );
			System.out.println( "Showing: " + key + "( " + title + " )" );
			bdv = BdvFunctions.show( spimData, BdvOptions.options().addTo( bdv ) ).get( 0 ).getBdvHandle();
			break;
		}

	}
}
