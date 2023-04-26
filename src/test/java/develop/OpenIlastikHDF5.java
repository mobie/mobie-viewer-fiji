package develop;

import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.lib.ThreadHelper;
import org.embl.mobie.lib.image.CachedCellImage;
import org.embl.mobie.lib.image.SourcePair;

public class OpenIlastikHDF5
{
	public static void main( String[] args )
	{
		final CachedCellImage< ? > test = new CachedCellImage( "test", "/Users/tischer/Desktop/C5_2022-07-12-165037-0000--0.4.0-0-1.4.0--tracking-oids.h5", 0, ImageDataFormat.IlastikHDF5, ThreadHelper.sharedQueue );
		final SourcePair< ? > sourcePair = test.getSourcePair();
	}
}
