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
import net.imglib2.Volatile;
import net.imglib2.position.FunctionRealRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.roi.RealMaskRealInterval;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.util.Intervals;
import org.embl.mobie.viewer.DataStore;
import org.embl.mobie.viewer.annotation.AnnotatedRegion;
import org.embl.mobie.viewer.source.RealRandomAccessibleIntervalTimelapseSource;
import org.embl.mobie.viewer.source.SourcePair;
import org.embl.mobie.viewer.table.saw.TableSawAnnotatedRegion;
import org.embl.mobie.viewer.transform.TransformHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class RegionLabelImage< AR extends AnnotatedRegion > implements Image< UnsignedIntType >
{
	private final String name;
	private final Set< AR > annotatedRegions;
	private Source< UnsignedIntType > source;
	private Source< ? extends Volatile< UnsignedIntType > > volatileSource = null;

	public RegionLabelImage( String name, Set< AR > annotatedRegions )
	{
		this.name = name;
		this.annotatedRegions = annotatedRegions;
		createLabelImage();
	}

	private void createLabelImage()
	{
		for ( AR annotatedRegion : annotatedRegions )
		{
			final TableSawAnnotatedRegion tableSawAnnotatedRegion = ( TableSawAnnotatedRegion ) annotatedRegion;
			//System.out.println( "RegionLabelImage " + name + ": " + annotatedRegion.regionId() + " images = " + Arrays.toString( tableSawAnnotatedRegion.getRegionImageNames().toArray( new String[ 0 ] ) ) + "\n" + TransformHelper.maskToString( annotatedRegion.getMask() ) );
			final List< String > regionImageNames = tableSawAnnotatedRegion.getRegionImageNames();
			for ( String regionImageName : regionImageNames )
			{
				final Image< ? > viewImage = DataStore.getImage( regionImageName );
				//System.out.println( "Region :" + viewImage.getName() + ": " + Arrays.toString( viewImage.getMask().minAsDoubleArray() ) + " - " + Arrays.toString( viewImage.getMask().maxAsDoubleArray() ) );
			}
		}
		final ArrayList< Integer > timePoints = configureTimePoints();
		final Interval interval = Intervals.smallestContainingInterval( getMask() );
		final FunctionRealRandomAccessible< UnsignedIntType > realRandomAccessible = new FunctionRealRandomAccessible( 3, new LocationToLabelSupplier(), UnsignedIntType::new );
		source = new RealRandomAccessibleIntervalTimelapseSource<>( realRandomAccessible, interval, new UnsignedIntType(), new AffineTransform3D(), name, true, timePoints );

		// TODO MAYBE
		//   Create volatile source by means of a CachedCellImg?!
		//   However this not so nice thing is that then I need to decide
		//   on some specific sampling.
		//   Currently I can simply create the annotations in real
		//   space, based on the (real)mask of the images.
	}

	class LocationToLabelSupplier implements Supplier< BiConsumer< RealLocalizable, UnsignedIntType > >
	{
		@Override
		public BiConsumer< RealLocalizable, UnsignedIntType > get()
		{
			return new LocationToLabel();
		}

		private class LocationToLabel implements BiConsumer< RealLocalizable, UnsignedIntType >
		{
			private RealMaskRealInterval recentMask;
			private HashMap< RealMaskRealInterval, Integer > maskToLabel;

			public LocationToLabel()
			{
				maskToLabel = new HashMap<>();
				for ( AR regionAnnotation : annotatedRegions )
				{
					final RealMaskRealInterval mask = regionAnnotation.getMask();
					maskToLabel.put( mask, regionAnnotation.label() );
				}
				this.recentMask = maskToLabel.keySet().iterator().next();
			}

			@Override
			public void accept( RealLocalizable location, UnsignedIntType value )
			{
				// TODO MUST
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
					value.setInteger( maskToLabel.get( recentMask) );
					return;
				}

				for ( Map.Entry< RealMaskRealInterval, Integer > entry : maskToLabel.entrySet() )
				{
					final RealMaskRealInterval mask = entry.getKey();

					if ( mask == recentMask )
						continue;

					if ( mask.test( location ) )
					{
						recentMask = mask;
						value.setInteger( entry.getValue() );
						return;
					}
				}

				// background
				value.setInteger( 0 );
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
	public SourcePair< UnsignedIntType > getSourcePair()
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
		return TransformHelper.getUnionMask( annotatedRegions, 0 );
	}

	@Override
	public void setMask( RealMaskRealInterval mask )
	{
		// TODO
	}

}
