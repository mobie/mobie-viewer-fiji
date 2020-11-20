package develop.s3;

import bdv.util.BdvFunctions;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;

public class OpenN5S3XML
{
	public static void main( String[] args ) throws SpimDataException
	{
		SpimData spimData = new XmlIoSpimData().load( "/Users/tischer/Desktop/josh/mhcl4-n5-aws-s3.xml" );
		BdvFunctions.show( spimData );
	}
}
