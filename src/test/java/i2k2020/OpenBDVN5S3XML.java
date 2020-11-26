package i2k2020;

import bdv.util.BdvFunctions;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;

public class OpenBDVN5S3XML
{
	public static void main( String[] args ) throws SpimDataException
	{
		SpimData spimData = new XmlIoSpimData().load( "src/test/resources/prospr-myosin-n5.xml" );
		BdvFunctions.show( spimData );
	}
}
