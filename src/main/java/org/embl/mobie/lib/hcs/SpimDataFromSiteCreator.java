package org.embl.mobie.lib.hcs;

import ch.epfl.biop.bdv.img.imageplus.ImagePlusToSpimData;
import ij.IJ;
import ij.ImagePlus;
import ij.VirtualStack;
import mpicbg.spim.data.generic.AbstractSpimData;
import org.embl.mobie.lib.io.TPosition;
import org.embl.mobie.lib.io.ZPosition;

import java.util.Map;

public class SpimDataFromSiteCreator
{
	public static AbstractSpimData< ? > create( Site site )
	{
		VirtualStack virtualStack = null;

		final Map< TPosition, Map< ZPosition, String > > paths = site.getPaths();

		for ( TPosition t : paths.keySet() )
		{
			for ( ZPosition z : paths.get( t ).keySet() )
			{
				if ( virtualStack == null )
				{
					final int[] pixelDimensions = site.getPixelDimensions();
					virtualStack = new VirtualStack( pixelDimensions[ 0 ], pixelDimensions[ 1 ], null, "" );
				}

				virtualStack.addSlice( paths.get( t ).get( z ) );
			}
		}

		final ImagePlus imagePlus = new ImagePlus( site.getName(), virtualStack );
		return ImagePlusToSpimData.getSpimData( imagePlus );
	}
}
