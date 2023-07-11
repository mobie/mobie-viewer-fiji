/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2023 EMBL
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.embl.mobie.lib.hcs;

import bdv.cache.SharedQueue;
import ch.epfl.biop.bdv.img.imageplus.ImagePlusToSpimData;
import ij.ImagePlus;
import ij.VirtualStack;
import ij.measure.Calibration;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.sequence.VoxelDimensions;
import org.embl.mobie.io.SpimDataOpener;
import org.embl.mobie.io.toml.TPosition;
import org.embl.mobie.io.toml.ZPosition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class SiteSpimDataCreator
{
	public static AbstractSpimData< ? > create( Site site, SharedQueue sharedQueue )
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

		final AbstractSpimData< ? > spimData = ImagePlusToSpimData.getSpimData( imagePlus );
		SpimDataOpener.setSharedQueue( sharedQueue, spimData );

		return spimData;
	}
}
