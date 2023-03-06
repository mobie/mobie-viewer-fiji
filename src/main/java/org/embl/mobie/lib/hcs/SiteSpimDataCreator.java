package org.embl.mobie.lib.hcs;

import ch.epfl.biop.bdv.img.imageplus.ImagePlusToSpimData;
import ij.ImagePlus;
import ij.VirtualStack;
import ij.measure.Calibration;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.sequence.VoxelDimensions;
import org.embl.mobie.lib.io.TPosition;
import org.embl.mobie.lib.io.ZPosition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class SiteSpimDataCreator
{
	public static AbstractSpimData< ? > create( Site site )
	{
		VirtualStack virtualStack = null;

		final Map< TPosition, Map< ZPosition, String > > paths = site.getPaths();

		final ArrayList< TPosition > tPositions = new ArrayList<>( paths.keySet() );
		Collections.sort( tPositions );
		int nT = tPositions.size();
		int nZ = 1;
		for ( TPosition t : tPositions )
		{
			final Set< ZPosition > zPositions = paths.get( t ).keySet();
			nZ = zPositions.size();
			for ( ZPosition z : zPositions )
			{
				if ( virtualStack == null )
				{
					final int[] dimensions = site.getDimensions();
					virtualStack = new VirtualStack( dimensions[ 0 ], dimensions[ 1 ], null, "" );
				}

				virtualStack.addSlice( paths.get( t ).get( z ) );
			}
		}

		final ImagePlus imagePlus = new ImagePlus( site.getName(), virtualStack );

		final Calibration calibration = new Calibration();
		final VoxelDimensions voxelDimensions = site.getVoxelDimensions();
		calibration.setUnit( voxelDimensions.unit() );
		calibration.pixelWidth = voxelDimensions.dimension( 0 );
		calibration.pixelHeight = voxelDimensions.dimension( 1 );
		calibration.pixelDepth = voxelDimensions.dimension( 2 );
		imagePlus.setCalibration( calibration );

		// TODO: is could be zSlices!
		imagePlus.setDimensions( 1, nZ, nT );
		return ImagePlusToSpimData.getSpimData( imagePlus );
	}
}
