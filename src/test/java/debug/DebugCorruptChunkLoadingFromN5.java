package debug;

import bdv.util.BdvFunctions;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import net.imagej.ImageJ;

public class DebugCorruptChunkLoadingFromN5
{
	public static void main( String[] args ) throws SpimDataException
	{
		new ImageJ(  ).ui().showUI();
		final SpimData spimData = new XmlIoSpimData().load( "/Users/tischer/Desktop/test_data_corruted_s3.xml" );
		BdvFunctions.show( spimData );
	}
}
