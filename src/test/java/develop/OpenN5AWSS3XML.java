package develop;

import bdv.util.BdvFunctions;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;

import java.util.List;

public class OpenN5AWSS3XML
{
	public static void main( String[] args ) throws SpimDataException
	{
		SpimData spimData = new XmlIoSpimData().load( "/Users/tischer/Desktop/josh/mhcl4-n5-aws-s3.xml" );
		BdvFunctions.show( spimData );
	}
}
