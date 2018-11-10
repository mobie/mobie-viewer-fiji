import bdv.util.BdvFunctions;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import mpicbg.spim.data.sequence.SetupImgLoader;

import java.io.File;

public class TestSpimDataLoading
{

	public static void main( String[] args )
	{

		final File file = new File( "/Users/tischer/Desktop/bdv_test_data/bdv_mipmap-labels.xml" );
		SpimData spimData = null;
		try
		{
			spimData = new XmlIoSpimData().load( file.toString() );
		}
		catch ( SpimDataException e )
		{
			e.printStackTrace();
		}

		BdvFunctions.show( spimData );

		// Notes
		// Loader class autodiscovery happens here:
		// - https://github.com/bigdataviewer/spimdata/blob/master/src/main/java/mpicbg/spim/data/generic/sequence/ImgLoaders.java#L53

	}
}
