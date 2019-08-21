package explore;

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
				= RemoteUtils.getDatasetUrlMap( "https://bigdata-cbb.embl.de/" );

		Bdv bdv = null;

		for ( String key : datasetUrlMap.keySet() )
		{
			System.out.println( "Key: " + key );
			System.out.println( "Path: " + datasetUrlMap.get( key ) );
		}

		final String filename = datasetUrlMap.values( ).iterator().next();
		final String title = new File( filename ).getName();
		System.out.println( "Showing: " + title );
		final SpimData spimData = new XmlIoSpimData().load( filename );


		BdvFunctions.show( spimData, BdvOptions.options().addTo( bdv ) ).get( 0 ).getBdvHandle();
	}
}
