package users.sultan;

import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;

import java.util.List;

public class ViewNeuropils
{
	public static void main( String[] args ) throws SpimDataException
	{
		SpimData platyBrowserNeuropil = new XmlIoSpimData().load( "/Volumes/cba/exchange/Sultan/prospr_neuropile_0.4um.xml" );
		SpimData xRayNeuropil = new XmlIoSpimData().load( "/Volumes/cba/exchange/Sultan/platy_90_02_neuropile_1um.xml" );


		final List< BdvStackSource< ? > > show = BdvFunctions.show( platyBrowserNeuropil );
		BdvFunctions.show( xRayNeuropil, BdvOptions.options().addTo( show.get( 0 ) ) );
	}
}
