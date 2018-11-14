import bdv.util.BdvFunctions;
import bdv.viewer.Source;
import de.embl.cba.bdv.utils.labels.luts.ARGBConvertedUnsignedLongTypeLabelsSource;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import net.imglib2.type.volatiles.VolatileARGBType;

import java.io.File;

public class TestSpimDataUsignedLongLabelsLoading
{

	public static void main( String[] args ) throws SpimDataException
	{

		// Loader class auto-discovery happens here:
		// https://github.com/bigdataviewer/spimdata/blob/master/src/main/java/mpicbg/spim/data/generic/sequence/ImgLoaders.java#L53

		final File file = new File( "/Volumes/arendt/EM_6dpf_segmentation/EM-Prospr/em-segmented-cells-parapodium-labels-test.xml" );

		//final File file = new File( "/Users/tischer/Desktop/bdv_test_data/bdv_mipmap-labels.xml" );

		SpimData spimData = new XmlIoSpimData().load( file.toString() );

		final Source< VolatileARGBType > labelSource = new ARGBConvertedUnsignedLongTypeLabelsSource( spimData, 0 );

		BdvFunctions.show( labelSource );


	}
}
