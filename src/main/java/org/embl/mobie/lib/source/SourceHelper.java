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

import bdv.AbstractSpimSource;
import bdv.SpimSource;
import bdv.tools.transformation.TransformedSource;
import bdv.util.AbstractSource;
import bdv.util.Affine3DHelpers;
import bdv.util.BdvHandle;
import bdv.util.ResampledSource;
import bdv.viewer.Source;
import mpicbg.spim.data.sequence.FinalVoxelDimensions;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.FinalRealInterval;
import net.imglib2.Interval;
import net.imglib2.Point;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealInterval;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.roi.RealMaskRealInterval;
import net.imglib2.roi.geom.GeomMasks;
import net.imglib2.roi.geom.real.WritableBox;
import net.imglib2.util.Intervals;
import org.embl.mobie.lib.bdv.GlobalMousePositionProvider;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper.getVoxelPositionInSource;

public abstract class SourceHelper
{

	public static < T > T unwrapSource( Source source, Class< T > clazz )
	{
		if ( source == null )
			return null;

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

	public static int getNumTimePoints( Source< ? > source )
	{
		int numSourceTimepoints = 0;
        final int maxNumTimePoints = 10000; // TODO
        for ( int t = 0; t < maxNumTimePoints; t++ )
		{
			if ( source.isPresent( t ) )
            {
                numSourceTimepoints++;
            }
            else
            {
                return numSourceTimepoints;
            }
		}

        if ( numSourceTimepoints == maxNumTimePoints )
		{
			System.err.println( source.getName() + " has more than " + maxNumTimePoints + " time-points. Is this an error?!" );
			return 1;
		}

		return numSourceTimepoints;
	}

	public static List< Integer > getTimePoints( Source< ? > source )
	{
		ArrayList< Integer > timePoints = new ArrayList<>();

		final int maxNumTimePoints = 20000; // TODO: better idea?

		for ( int t = 0; t < maxNumTimePoints; t++ )
		{
			if ( source.isPresent( t ) )
			{
				timePoints.add( t );
			}
		}

		if ( timePoints.size() > maxNumTimePoints / 2 )
		{
			System.err.println( source.getName() + " has " + timePoints.size() + " time-points. Is this correct?!" );
		}

		return timePoints;
	}

	public static void fetchRootSources( Source< ? > source, Collection< Source< ? > > rootSources )
	{
		if ( source instanceof SpimSource )
		{
			rootSources.add( source );
		}
		else if ( source instanceof TransformedSource )
		{
			final Source< ? > wrappedSource = ( ( TransformedSource ) source ).getWrappedSource();

			fetchRootSources( wrappedSource, rootSources );
		}
		else if ( source instanceof SourceWrapper )
		{
			final Source< ? > wrappedSource = (( SourceWrapper ) source).getWrappedSource();

			fetchRootSources( wrappedSource, rootSources );
		}
		// TODO: how do do this now? Do we still need it?
//		else if (  source instanceof MergedGridSource )
//		{
//			final MergedGridSource< ? > mergedGridSource = ( MergedGridSource ) source;
//			final List< ? extends SourceAndConverter< ? > > gridSources = mergedGridSource.getGridSources();
//			for ( SourceAndConverter< ? > gridSource : gridSources )
//			{
//				fetchRootSources( gridSource.getSpimSource(), rootSources );
//			}
//		}
//		else if ( source instanceof StitchedSource )
//		{
//			final StitchedImage< ?, ? > stitchedImage = ( StitchedImage ) source;
//			final List< ? extends Source< ? > > gridSources = stitchedImage.getImages().stream().map( image -> image.getSourcePair().getSource() ).collect( Collectors.toList() );
//			for ( Source< ? > gridSource : gridSources )
//			{
//				fetchRootSources( gridSource, rootSources );
//			}
//		}
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


	public static RealMaskRealInterval getMask( Source< ? > source, int t )
	{
		// fetch the extent of the source in voxel space
		final double[] min = new double[ 3 ];
		final double[] max = new double[ 3 ];

		final Masked masked = SourceHelper.unwrapSource( source, Masked.class );
		if ( masked != null )
		{
			final RealInterval realInterval = masked.getMask();
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

	public static RealMaskRealInterval estimatePhysicalMask( Source< ? > source, int t, boolean includeVoxelDimensions )
	{
		WritableBox box = estimateDataMask( source, t, 0, includeVoxelDimensions );

		// apply the source transformation to get the mask in global space
		final AffineTransform3D sourceTransform = new AffineTransform3D();
		source.getSourceTransform( t, 0, sourceTransform );

		return box.transform( sourceTransform.inverse() );
	}

	@NotNull
	public static WritableBox estimateDataMask( Source< ? > source, int t, int level, boolean includeVoxelDimensions  )
	{
		// determine the extent of the source in voxel space
		//
		final RandomAccessibleInterval< ? > rai = source.getSource( t, level );
		final double[] min = rai.minAsDoubleArray();
		final double[] max = rai.maxAsDoubleArray();

		final AffineTransform3D sourceTransform = new AffineTransform3D();
		source.getSourceTransform( t, level, sourceTransform );

		// extend the bounds in voxel space to include the voxel dimensions
		if ( includeVoxelDimensions )
		{
			try
			{
				final double[] voxelDimensions = source.getVoxelDimensions().dimensionsAsDoubleArray();
				for ( int d = 0; d < min.length; d++ )
				{
					final double scale = Affine3DHelpers.extractScale( sourceTransform, d );
					min[ d ] -= voxelDimensions[ d ] / scale;
					max[ d ] += voxelDimensions[ d ] / scale;
				}
			}
			catch ( Exception e  )
			{
				throw new RuntimeException( e );
			}

		}
		WritableBox box = GeomMasks.closedBox( min, max );
		return box;
	}

	public static FinalRealInterval bounds( Source< ? > source, int t )
	{
		final AffineTransform3D affineTransform3D = new AffineTransform3D();
		source.getSourceTransform( 0, 0, affineTransform3D );
 		final RandomAccessibleInterval< ? > rai = source.getSource( t, 0 );
		final FinalRealInterval bounds = affineTransform3D.estimateBounds( rai );
		return bounds;
	}

	public static List< Source< ? > > filterAtCurrentMousePosition( Collection< Source< ? > > sources, BdvHandle bdvHandle )
	{
		final GlobalMousePositionProvider positionProvider = new GlobalMousePositionProvider( bdvHandle );
		final RealPoint position = positionProvider.getPositionAsRealPoint();
		final int timePoint = positionProvider.getTimePoint();
		final List< Source< ? > > sourcesAtCurrentPosition = sources.stream()
				.filter( source -> isPositionWithinSourceInterval( source, position, timePoint ) )
				.collect( Collectors.toList() );

		return sourcesAtCurrentPosition;
	}

	public static void setVoxelDimensionsToPixels( Source< ? > source, double[] scale, String unit )
	{
		try
		{
			Field voxelDimensions;
			if ( source instanceof AbstractSpimSource )
				voxelDimensions = AbstractSpimSource.class.getDeclaredField("voxelDimensions");
			else if ( source instanceof AbstractSource )
				voxelDimensions = AbstractSource.class.getDeclaredField("voxelDimensions");
			else
			{
				throw new RuntimeException("Cannot access field voxelDimensions in " + source.getClass().getName() );
			}

			voxelDimensions.setAccessible(true);
			voxelDimensions.set( source, new FinalVoxelDimensions( unit, scale ) );
		}
		catch ( Exception e )
		{
			throw new RuntimeException( e );
		}
	}

	public static void setVoxelDimensions( Source< ? > source, VoxelDimensions voxelDimensions )
	{
		try
		{
			Field voxelDimensionsField;
			if ( source instanceof AbstractSpimSource )
				voxelDimensionsField = AbstractSpimSource.class.getDeclaredField("voxelDimensions");
			else if ( source instanceof AbstractSource )
				voxelDimensionsField = AbstractSource.class.getDeclaredField("voxelDimensions");
			else
			{
				throw new RuntimeException("Cannot access field voxelDimensions in " + source.getClass().getName() );
			}

			voxelDimensionsField.setAccessible(true);
			voxelDimensionsField.set( source, voxelDimensions );
		}
		catch ( Exception e )
		{
			throw new RuntimeException( e );
		}
	}

	public static int getLevel( Source< ? > source, long maxNumPixels )
	{
		final int numMipmapLevels = source.getNumMipmapLevels();

		for ( int level = 0; level < numMipmapLevels; level++ )
		{
			long[] dimensions = source.getSource( 0, level ).dimensionsAsLongArray();

			final boolean javaIndexingOK = dimensions[ 0 ] * dimensions[ 1 ] < Integer.MAX_VALUE - 1;

			final boolean sizeOK = dimensions[ 0 ] * dimensions[ 1 ] * dimensions[ 2 ] < maxNumPixels;

			if( javaIndexingOK && sizeOK )
				return level;
		}
		return -1;
	}

	public static boolean isPositionWithinSourceInterval( Source< ? > source, RealPoint realPosition, int timepoint )
	{
		final long[] voxelPositionInSource = getVoxelPositionInSource( source, realPosition, timepoint, 0);
		Interval sourceInterval = source.getSource( timepoint, 0 );
		Point point3d = new Point( voxelPositionInSource );
		return Intervals.contains( sourceInterval, point3d );

//		if (sourceIs2d) {
//			final long[] min = new long[2];
//			final long[] max = new long[2];
//			final long[] positionInSource2D = new long[2];
//			for (int d = 0; d < 2; d++) {
//				min[d] = sourceInterval.min(d);
//				max[d] = sourceInterval.max(d);
//				positionInSource2D[d] = voxelPositionInSource[d];
//			}
//
//			Interval interval2d = new FinalInterval(min, max);
//			Point point2d = new Point(positionInSource2D);
//
//			return Intervals.contains(interval2d, point2d);
//		}
//		else {
	}
}
