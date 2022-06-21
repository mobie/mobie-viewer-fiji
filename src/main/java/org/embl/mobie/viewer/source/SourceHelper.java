/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2022 EMBL
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
package org.embl.mobie.viewer.source;

import bdv.SpimSource;
import bdv.tools.transformation.TransformedSource;
import bdv.util.BdvStackSource;
import bdv.util.ResampledSource;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import net.imglib2.FinalRealInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converter;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.roi.RealMaskRealInterval;
import net.imglib2.roi.geom.GeomMasks;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Cast;
import net.imglib2.util.Intervals;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import org.embl.mobie.viewer.transform.RealIntervalProvider;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class SourceHelper
{
    // TODO: one could get rid of this if Sources could
    //   return their mask directly with some other method!
	//   Is this still used?
	@Deprecated
    public static double[][] getMinMax( Source< ? > source )
    {
        final double[][] minMax = new double[2][];
        final Source< ? > rootSource = fetchRootSource( source );
        if ( rootSource instanceof LazySpimSource )
        {
            minMax[ 0 ] = ( ( LazySpimSource ) rootSource ).getMin();
            minMax[ 1 ] = ( ( LazySpimSource ) rootSource ).getMax();
        }
        else
        {
            final RandomAccessibleInterval< ? > rai = source.getSource( 0, 0 );
            minMax[ 0 ] = rai.minAsDoubleArray();
            minMax[ 1 ] = rai.maxAsDoubleArray();
        }

        return minMax;
    }

	public static < T > T unwrapSource( Source source, Class< T > clazz )
	{
		if ( clazz.isInstance( source ) )
		{
			return ( T ) source;
		}
		else if ( source instanceof TransformedSource )
		{
			final Source< ? > wrappedSource = ( ( TransformedSource< ? > ) source ).getWrappedSource();
			return unwrapSource( wrappedSource, clazz );
		}
		else if ( source instanceof SourceWrapper )
		{
			final Source< ? > wrappedSource = ( ( SourceWrapper< ? > ) source ).getWrappedSource();
			return unwrapSource( wrappedSource, clazz );
		}
		else
		{
			return null;
		}
	}

	public static int getNumTimepoints( SourceAndConverter< ? > source )
	{
		int numSourceTimepoints = 0;
        final int maxNumTimePoints = 10000; // TODO
        for ( int t = 0; t < maxNumTimePoints; t++ )
		{
			if ( source.getSpimSource().isPresent( t ) )
            {
                numSourceTimepoints++;
            }
            else
            {
                return numSourceTimepoints;
            }
		}

        if ( numSourceTimepoints == maxNumTimePoints )
            System.err.println( source.getSpimSource().getName() + " has more than " + maxNumTimePoints + " time-points. Is this an error?!" );

		return numSourceTimepoints;
	}

	/**
	 * Recursively fetch all root sources
	 * @param source
	 * @param rootSources
	 */
	public static void fetchRootSources( Source< ? > source, Set< Source< ? > > rootSources )
	{
		if ( source instanceof SpimSource )
		{
			rootSources.add( source );
		}
		else if ( source instanceof LazySpimSource )
		{
			rootSources.add( source );
		}
		else if ( source instanceof TransformedSource )
		{
			final Source< ? > wrappedSource = ( ( TransformedSource ) source ).getWrappedSource();

			fetchRootSources( wrappedSource, rootSources );
		}
		else if (  source instanceof BoundarySource )
		{
			final Source< ? > wrappedSource = (( BoundarySource ) source).getWrappedSource();

			fetchRootSources( wrappedSource, rootSources );
		}
		else if (  source instanceof MergedGridSource )
		{
			final MergedGridSource< ? > mergedGridSource = ( MergedGridSource ) source;
			final List< ? extends SourceAndConverter< ? > > gridSources = mergedGridSource.getGridSources();
			for ( SourceAndConverter< ? > gridSource : gridSources )
			{
				fetchRootSources( gridSource.getSpimSource(), rootSources );
			}
		}
		else if (  source instanceof FunctionGridSource )
		{
			final FunctionGridSource< ? > functionGridSource = ( FunctionGridSource ) source;
			final List< ? extends Source< ? > > gridSources = functionGridSource.getGridSources();
			for ( Source< ? > gridSource : gridSources )
			{
				fetchRootSources( gridSource, rootSources );
			}
		}
		else if (  source instanceof ResampledSource )
		{
			final ResampledSource resampledSource = ( ResampledSource ) source;
			final Source< ? > wrappedSource = resampledSource.getOriginalSource();
			fetchRootSources( wrappedSource, rootSources );
		}
		else
		{
			throw new IllegalArgumentException("For sources of type " + source.getClass().getName() + " the root source currently cannot be determined.");
		}
	}

	public static Source< ? > fetchRootSource( Source< ? > source )
	{
		final Set< Source< ? > > rootSources = new HashSet<>();
		fetchRootSources( source, rootSources );
		return rootSources.iterator().next();
	}

	public static RealMaskRealInterval getUnionMask( List< ? extends Source< ? > > sources, int t )
	{
		RealMaskRealInterval union = null;

		for ( Source< ? > source : sources )
		{
			final RealMaskRealInterval mask = SourceHelper.getMask( source, t );

			if ( union == null )
			{
				union = mask;
			}
			else
			{
				if ( Intervals.equals( mask, union ) )
				{
					continue;
				}
				else
				{
					union = union.or( mask );
				}
			}
		}

		return union;
	}

	public static RealMaskRealInterval getMask( Source< ? > source, int t )
	{
		// fetch the extent of the source in voxel space
		final double[] min = new double[ 3 ];
		final double[] max = new double[ 3 ];

		final RealIntervalProvider realIntervalProvider = SourceHelper.unwrapSource( source, RealIntervalProvider.class );
		if ( realIntervalProvider != null )
		{
			final FinalRealInterval realInterval = realIntervalProvider.getRealInterval( t );
			realInterval.realMin( min );
			realInterval.realMax( max );
		}
		else
		{
			final RandomAccessibleInterval< ? > rai = source.getSource( t, 0 );
			rai.realMin( min );
			rai.realMax( max );
		}
		final AffineTransform3D sourceTransform = new AffineTransform3D();
		source.getSourceTransform( 0, 0, sourceTransform );

		// expand with voxel dimensions
		final double[] voxelSizes = new double[ 3 ];
		source.getVoxelDimensions().dimensions( voxelSizes );
		for ( int d = 0; d < 3; d++ )
		{
			min[ d ] -= voxelSizes[ d ];
			max[ d ] += voxelSizes[ d ];
		}

		// create mask
		// as compared with estimateBounds this has the
		// advantage that it can represent a rotated box
		final RealMaskRealInterval mask = GeomMasks.closedBox( min, max ).transform( sourceTransform.inverse() );
		return mask;
	}
}
