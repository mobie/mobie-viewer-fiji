/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2024 EMBL
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
package org.embl.mobie.lib.source;

import bdv.util.Affine3DHelpers;
import bdv.viewer.Source;
import ij.ImagePlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import java.util.ArrayList;

public class SourceToImagePlusConverter < T extends NumericType< T > >
{
	private final Source< T > source;

	public SourceToImagePlusConverter( Source< T > source )
	{
		this.source = source;
	}

	public ImagePlus getImagePlus( int level )
	{
		final AffineTransform3D sourceTransform = new AffineTransform3D();
		source.getSourceTransform( 0, level, sourceTransform );

		double[] sourceScale = new double[ 3 ];
		for ( int d = 0; d < 3; d++ )
		{
			sourceScale[ d ] = Affine3DHelpers.extractScale( sourceTransform, d );
		}

		final ImagePlus imagePlus = getImagePlus( source, level );
		imagePlus.getCalibration().setUnit( source.getVoxelDimensions().unit() );
		imagePlus.getCalibration().pixelWidth = sourceScale[ 0 ];
		imagePlus.getCalibration().pixelHeight = sourceScale[ 1 ];
		imagePlus.getCalibration().pixelDepth = sourceScale[ 2 ];
		return imagePlus;
	}

	private ImagePlus getImagePlus( Source< T > source, int level )
	{
		final RandomAccessibleInterval< T > raiXYZT = asRAIXYZT( source, level );
		final IntervalView< T > raiXYZTC = Views.addDimension( raiXYZT, 0, 0 );
		final IntervalView< T > raiXYCZT = Views.permute( Views.permute( raiXYZTC, 4, 3 ), 3, 2);
		final ImagePlus imagePlus = ImageJFunctions.wrap( raiXYCZT, source.getName() );
		return imagePlus;
	}

	private RandomAccessibleInterval< T > asRAIXYZT( Source< T > source, int level )
	{
		int numTimepoints = 0;

		while ( source.isPresent( numTimepoints ) )
		{
			numTimepoints++;
			if ( numTimepoints >= Integer.MAX_VALUE )
			{
				throw new RuntimeException("The source " + source.getName() + " appears to contain more than " + Integer.MAX_VALUE + " time points; maybe something is wrong?");
			}
		}

		return asRAIXYZT( source, level, numTimepoints );
	}

	private RandomAccessibleInterval< T > asRAIXYZT( Source< T > source, int level, int numTimepoints )
	{
		final ArrayList< RandomAccessibleInterval< T > > rais = new ArrayList<>();
		for ( int t = 0; t < numTimepoints; t++ )
		{
			rais.add( source.getSource( t, level ) );
		}
		return Views.stack( rais );
	}

}
