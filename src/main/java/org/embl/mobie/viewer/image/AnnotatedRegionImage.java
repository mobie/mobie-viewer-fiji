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
package org.embl.mobie.viewer.image;

import bdv.viewer.Source;
import net.imglib2.Interval;
import net.imglib2.RealLocalizable;
import net.imglib2.position.FunctionRealRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.roi.RealMaskRealInterval;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.util.Intervals;
import org.embl.mobie.viewer.DataStore;
import org.embl.mobie.viewer.annotation.AnnotatedRegion;
import org.embl.mobie.viewer.source.AnnotationType;
import org.embl.mobie.viewer.source.RealRandomAccessibleIntervalTimelapseSource;
import org.embl.mobie.viewer.source.SourcePair;
import org.embl.mobie.viewer.source.VolatileAnnotationType;
import org.embl.mobie.viewer.table.AnnData;
import org.embl.mobie.viewer.table.saw.TableSawAnnotatedRegion;
import org.embl.mobie.viewer.transform.TransformHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class AnnotatedRegionImage< AR extends AnnotatedRegion > implements AnnotationImage< AR >
{
	private final String name;

	private final AnnData< AR > annData;

	private Source< AnnotationType< AR > > source;

	// No volatile implementation, because the
	// {@code source} should be fast enough,
	// and probably a volatile version would need an {@code CachedCellImg},
	// which would require deciding on a specific spatial sampling,
	// which is not nice because a {@code region} is defined in real space.
	private Source< ? extends VolatileAnnotationType< AR > > volatileSource = null;

	public AnnotatedRegionImage( String name, AnnData< AR > annData )
	{
		this.name = name;
		this.annData = annData;
		createImage();
	}

	private void createImage()
	{
		debugLogging();

		final ArrayList< Integer > timePoints = configureTimePoints();
		final Interval interval = Intervals.smallestContainingInterval( getMask() );
		final FunctionRealRandomAccessible< AnnotationType< AR > > realRandomAccessible = new FunctionRealRandomAccessible( 3, new LocationToAnnotatedRegionSupplier(), UnsignedIntType::new );
		final AR annotatedRegion = annData.getTable().annotations().get( 0 );
		final AnnotationType< AR > annotationType = new AnnotationType<>( annotatedRegion );
		source = new RealRandomAccessibleIntervalTimelapseSource<>( realRandomAccessible, interval, annotationType, new AffineTransform3D(), name, true, timePoints );
	}

	private void debugLogging()
	{
		final ArrayList< AR > annotations = annData.getTable().annotations();
		for ( AR annotatedRegion : annotations )
		{
			final TableSawAnnotatedRegion tableSawAnnotatedRegion = ( TableSawAnnotatedRegion ) annotatedRegion;
			//System.out.println( "RegionLabelImage " + name + ": " + annotatedRegion.regionId() + " images = " + Arrays.toString( tableSawAnnotatedRegion.getRegionImageNames().toArray( new String[ 0 ] ) ) + "\n" + TransformHelper.maskToString( annotatedRegion.getMask() ) );
			final List< String > regionImageNames = tableSawAnnotatedRegion.getRegionImageNames();
			for ( String regionImageName : regionImageNames )
			{
				final Image< ? > viewImage = DataStore.getImage( regionImageName );
				//System.out.println( "Region: " + viewImage.getName() + ": " + Arrays.toString( viewImage.getMask().minAsDoubleArray() ) + " - " + Arrays.toString( viewImage.getMask().maxAsDoubleArray() ) );
			}
		}
	}

	@Override
	public AnnData< AR > getAnnData()
	{
		return annData;
	}

	class LocationToAnnotatedRegionSupplier implements Supplier< BiConsumer< RealLocalizable, AnnotationType< AR > > >
	{
		@Override
		public BiConsumer< RealLocalizable, AnnotationType< AR > > get()
		{
			return new LocationToRegion();
		}

		private class LocationToRegion implements BiConsumer< RealLocalizable, AnnotationType< AR > >
		{
			private RealMaskRealInterval recentMask;
			private HashMap< RealMaskRealInterval, AR > maskToAnnotatedRegion;

			public LocationToRegion()
			{
				maskToAnnotatedRegion = new HashMap<>();
				final ArrayList< AR > annotations = annData.getTable().annotations();
				for ( AR annotatedRegion : annotations )
				{
					final RealMaskRealInterval mask = annotatedRegion.getMask();
					maskToAnnotatedRegion.put( mask, annotatedRegion );
				}
				this.recentMask = maskToAnnotatedRegion.keySet().iterator().next();
			}

			@Override
			public void accept( RealLocalizable location, AnnotationType< AR > value )
			{
				// TODO
				//    There is a lof of background, which is expensive as one
				//    needs to traverse the whole loop => implement a background mask
				//    that is tested first.
				//    This needs however: https://github.com/imglib/imglib2-roi/pull/63
				//    Also this only makes sense if one can cache the mask such that it
				//    it does not need to traverse (thus maybe not possible)?
				//    Alternative: Is there some data structure that would allow to
				//    first look for image annotations that are close to the point?
				//    Maybe the idea with the most recent one is
				//    still the best (see above)?

				// It is likely that the next asked location
				// is within the same mask, thus we test that one first
				// to safe some computations.
				if ( recentMask.test( location ) )
				{
					value.setAnnotation( maskToAnnotatedRegion.get( recentMask) );
					return;
				}

				for ( Map.Entry< RealMaskRealInterval, AR > entry : maskToAnnotatedRegion.entrySet() )
				{
					final RealMaskRealInterval mask = entry.getKey();

					if ( mask == recentMask )
						continue;

					if ( mask.test( location ) )
					{
						recentMask = mask;
						value.setAnnotation( entry.getValue() );
						return;
					}
				}

				// background
				value.setAnnotation( null );
			}
		}
	}

	private ArrayList< Integer > configureTimePoints()
	{
		final ArrayList< Integer > timePoints = new ArrayList<>();
		timePoints.add( 0 );
		return timePoints;
	}

	@Override
	public SourcePair< AnnotationType< AR > > getSourcePair()
	{
		return new DefaultSourcePair<>( source, volatileSource );
	}

	public String getName()
	{
		return name;
	}

	@Override
	public void transform( AffineTransform3D affineTransform3D )
	{
		// TODO
		//   Not sure this actually makes sense, because the regions
		//   are the entities that should be transformed.
		throw new RuntimeException();
	}

	@Override
	public RealMaskRealInterval getMask( )
	{
		return TransformHelper.getUnionMask( annData.getTable().annotations(), 0 );
	}

	@Override
	public void setMask( RealMaskRealInterval mask )
	{
		throw new RuntimeException("Setting a mask of a " + this.getClass() + " is currently not supported.");
	}
}
