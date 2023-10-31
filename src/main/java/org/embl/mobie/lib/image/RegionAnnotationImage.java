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
package org.embl.mobie.lib.image;

import bdv.viewer.Source;
import net.imglib2.Interval;
import net.imglib2.RealLocalizable;
import net.imglib2.position.FunctionRealRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.roi.RealMaskRealInterval;
import net.imglib2.util.Intervals;
import org.embl.mobie.DataStore;
import org.embl.mobie.lib.annotation.AnnotatedRegion;
import org.embl.mobie.lib.select.SelectionModel;
import org.embl.mobie.lib.serialize.display.RegionDisplay;
import org.embl.mobie.lib.source.AnnotationType;
import org.embl.mobie.lib.source.RealRandomAccessibleIntervalTimelapseSource;
import org.embl.mobie.lib.table.AnnData;
import org.embl.mobie.lib.table.saw.TableSawAnnotatedImages;
import org.embl.mobie.lib.transform.TransformHelper;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class RegionAnnotationImage< AR extends AnnotatedRegion > implements AnnotationImage< AR >
{
	private final String name;

	private final AnnData< AR > annData;
	private final Set< Integer > timepoints;
	private final SelectionModel< ? > selectionModel;
	private Source< AnnotationType< AR > > source;
	private SourcePair< AnnotationType< AR > > sourcePair;
	private RealMaskRealInterval mask;

	private boolean debug = false;
	private List< AR > annotations;

	/**
	 * Builds a label image to visualise all {@code AnnotatedRegion} in the
	 *  {@code AnnData} of a {@code RegionDisplay}.
	 *
	 * Currently, the timepoints of the regions in annData are ignored.
	 * This could be changed (there are some comments in the code of this class
	 * for where and which changes would be needed). Instead, all the provided
	 * {@code timepoints} are annotated with all {@code AnnotatedRegion}.
	 * TODO https://github.com/mobie/mobie-viewer-fiji/issues/975
	 *
	 * @param regionDisplay
	 * 				the regionDisplay that "encodes" this image, TODO https://github.com/mobie/mobie-viewer-fiji/issues/818
	 * @param annData
	 * 				annData containing the regions that shall be annotated
	 */
	public RegionAnnotationImage( RegionDisplay< ? > regionDisplay, AnnData< AR > annData )
	{
		this.name = regionDisplay.getName();
		this.annData = annData;
		this.timepoints = regionDisplay.timepoints();
		this.selectionModel = regionDisplay.selectionModel;

		if( debug ) logRegions();
	}

	private void logRegions()
	{
		final ArrayList< AR > annotations = annData.getTable().annotations();
		for ( AR annotatedRegion : annotations )
		{
			// FIXME currently the cast below works because TableSawAnnotatedImages is the only use-case
			//    In general, it may also be something else
			final TableSawAnnotatedImages tableSawAnnotatedImages = ( TableSawAnnotatedImages ) annotatedRegion;
			System.out.println( "RegionLabelImage " + name + ": " + annotatedRegion.regionId() + " images = " + Arrays.toString( tableSawAnnotatedImages.getImageNames().toArray( new String[ 0 ] ) ) + "\n" + TransformHelper.maskToString( annotatedRegion.getMask() ) );
			final List< String > regionImageNames = tableSawAnnotatedImages.getImageNames();
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
			private AR recentAnnotation; // the annotation that was at the recent location

			public LocationToRegion()
			{
				this.recentAnnotation = annotations.get( 0 );
			}

			@Override
			public void accept( RealLocalizable location, AnnotationType< AR > value )
			{
				// It is likely that the next location
				// is within the same mask, thus we test that one first
				// to safe some computations.
				if ( recentAnnotation.getMask().test( location ) )
				{
					value.setAnnotation( recentAnnotation );
					return;
				}

				// It was not in the recent mask,
				// so we need to test all the others.
				for ( AR annotation : annotations )
				{
					if ( annotation == recentAnnotation )
						continue; // that one has been checked already above

					if ( annotation.getMask().test( location ) )
					{
						recentAnnotation = annotation;
						value.setAnnotation( recentAnnotation );
						return;
					}
				}

				// The location is not within any mask => it is background
				value.setAnnotation( null );
			}
		}
	}

	@Override
	public synchronized SourcePair< AnnotationType< AR > > getSourcePair()
	{
		if ( sourcePair == null )
		{
			final Interval interval = Intervals.smallestContainingInterval( getMask() );

			annotations = annData.getTable().annotations();

			// one could add a time point parameter to LocationToAnnotatedRegionSupplier
			// and then make a Map< Timepoint, regions > and modify RealRandomAccessibleIntervalTimelapseSource to consume this map
			final FunctionRealRandomAccessible< AnnotationType< AR > > regions = new FunctionRealRandomAccessible( 3, new LocationToAnnotatedRegionSupplier(), () -> new AnnotationType<>( annotations.get( 0 ) ) );

			// TODO it would be nice if this Source had the same voxel unit
			//   as the other sources, but that would mean touching one of the
			//   annotated images which could be expensive.
			source = new RealRandomAccessibleIntervalTimelapseSource<>( regions, interval, new AnnotationType<>( annotations.get( 0 ) ), new AffineTransform3D(), name, true, timepoints );

			// There is no volatile implementation (yet), because the
			// {@code Source} should be fast enough,
			// and probably a volatile version would need an {@code CachedCellImg},
			// which would require deciding on a specific spatial sampling,
			// which is not nice because a {@code Region} is defined in real space.
			sourcePair = new DefaultSourcePair<>( source, null );
		}

		return sourcePair;
	}

	public String getName()
	{
		return name;
	}

	@Override
	public void transform( AffineTransform3D affineTransform3D )
	{
		if ( annotations.get( 0 ) instanceof TableSawAnnotatedImages )
		{
			// transform all images in all regions
			List< TableSawAnnotatedImages > annotatedImages = ( List< TableSawAnnotatedImages > ) annotations;

			List< Image< ? > > allImages = annotatedImages.stream()
					.map( ai -> ai.getImageNames() )
					.map( ain -> DataStore.getImageSet( ain ) )
					.flatMap( images -> images.stream() )
					.collect( Collectors.toList() );

			allImages.stream().forEach( image -> image.transform( affineTransform3D ) );
		}
		else
		{
			throw new RuntimeException( "Transformation of regions of type "
					+ annotations.get( 0 ).getClass() + " is currently not implemented." );
		}
	}

	@NotNull
	public List< Image< ? > > getSelectedImages()
	{
		if ( annotations.get( 0 ) instanceof TableSawAnnotatedImages )
		{
			Set< TableSawAnnotatedImages > annotatedImagesSet = ( Set< TableSawAnnotatedImages > ) selectionModel.getSelected();
			List< Image< ? > > selectedImages = annotatedImagesSet.stream()
					.map( annotatedImages -> annotatedImages.getImageNames() )
					.map( annotatedImageNames -> DataStore.getImageSet( annotatedImageNames ) )
					.flatMap( images -> images.stream() )
					.collect( Collectors.toList() );
			return selectedImages;
		}
		else
		{
			throw new RuntimeException( "Cannot return images, because the regions are of type "
					+ annotations.get( 0 ).getClass() + " ." );
		}
	}


	@Override
	public RealMaskRealInterval getMask( )
	{
		if ( mask == null )
		{
			mask = TransformHelper.unionBox( annData.getTable().annotations() );
		}

		return mask;
	}

	@Override
	public void setMask( RealMaskRealInterval mask )
	{
		throw new RuntimeException("Setting a mask of a " + this.getClass() + " is currently not supported.");
	}
}
