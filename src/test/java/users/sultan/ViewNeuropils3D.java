package users.sultan;

//import bvv.util.BvvFunctions;
//import bvv.util.BvvOptions;
//import bvv.util.BvvStackSource;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import net.imglib2.type.numeric.ARGBType;

import java.util.List;

public class ViewNeuropils3D
{

	public static void main( String[] args ) throws SpimDataException
	{
		SpimData platyBrowserNeuropil = new XmlIoSpimData().load( "/Volumes/cba/exchange/Sultan/prospr_neuropile_0.4um.xml" );
		SpimData xRayNeuropil = new XmlIoSpimData().load( "/Volumes/cba/exchange/Sultan/platy_90_02_neuropile_1um.xml" );
		SpimData xRayNeuropilAligned = new XmlIoSpimData().load( "/Volumes/cba/exchange/Sultan/platy_90_02_neuropile_1um-transform.xml-aligned.xml" );


//		final List< BvvStackSource< ? > > show = BvvFunctions.show( xRayNeuropil );
//		show.get( 0 ).setDisplayRange( 0, 65535 );
//
//		final List< BvvStackSource< ? > > show2 = BvvFunctions.show( platyBrowserNeuropil, BvvOptions.options().addTo( show.get( 0 ).getBvvHandle() ) );
//		show2.get( 0 ).setDisplayRange( 0, 255 );
//		show2.get( 0 ).setColor( new ARGBType( 0xff00ff00 ) );
//
//		final List< BvvStackSource< ? > > show3 = BvvFunctions.show( xRayNeuropilAligned, BvvOptions.options().addTo( show.get( 0 ).getBvvHandle() ) );
//		show3.get( 0 ).setDisplayRange( 0, 255 );
//		show3.get( 0 ).setColor( new ARGBType( 0xff00ffff ) );

	}
}
