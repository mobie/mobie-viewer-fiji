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
package org.embl.mobie.lib.image;

import bdv.tools.transformation.TransformedSource;
import bdv.viewer.Source;
import mpicbg.spim.data.sequence.FinalVoxelDimensions;
import net.imglib2.*;
import net.imglib2.neighborsearch.RadiusNeighborSearchOnKDTree;
import net.imglib2.position.FunctionRealRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealViews;
import net.imglib2.roi.RealMaskRealInterval;
import net.imglib2.roi.geom.GeomMasks;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.integer.UnsignedLongType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.util.Intervals;
import org.embl.mobie.lib.annotation.AnnotatedSpot;
import org.embl.mobie.lib.source.RealRandomAccessibleIntervalTimelapseSource;
import org.embl.mobie.lib.table.DefaultAnnData;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class SpotLabelImage< AS extends AnnotatedSpot, T extends IntegerType< T > > implements Image< T >
{
	private final String name;
	private final DefaultAnnData< AS > annData;
	private KDTree< AS > kdTree;
	private RealMaskRealInterval mask;
	private Double spotRadius;
	private double[] imageBoundsMin;
	private double[] imageBoundsMax;
	private AffineTransform3D affineTransform3D;
	private Source< T > source;
	private TransformedSource< T > transformedSource;
	private DefaultSourcePair sourcePair;

	public SpotLabelImage(
			String name,
			DefaultAnnData< AS > annData,
			@Nullable Double spotRadius,
			@Nullable double[] imageBoundsMin,
			@Nullable double[] imageBoundsMax )
	{
		this.name = name;
		this.annData = annData;
		this.spotRadius = spotRadius != null ? spotRadius : 1.0;
		this.imageBoundsMin = imageBoundsMin;
		this.imageBoundsMax = imageBoundsMax;

		createImage();
	}

	public Double getSpotRadius()
	{
		return spotRadius;
	}

	public void setSpotRadius( Double spotRadius )
	{
		if ( spotRadius != null )
		{
			// the spotRadius is used in the kdTree which generates the image
			this.spotRadius = spotRadius;
		}
	}

	private void createImage()
	{
		final ArrayList< AS > annotations = annData.getTable().annotations();
		int numAnnotations = annotations.size();
		System.out.println("Building KDTree with numElements = " + numAnnotations );
		kdTree = new KDTree( annotations, annotations );

		if ( imageBoundsMin == null )
		{
			setImageBounds();
		}

		double[] size = new double[ 3 ];
		for ( int d = 0; d < 3; d++ )
		{
			size[ d ] = imageBoundsMax[ d ] - imageBoundsMin[ d ];
		}

		// create the image mask
		mask = GeomMasks.closedBox( imageBoundsMin, imageBoundsMax );

		// create the actual image
		RealRandomAccessible< IntegerType > rra =
				new FunctionRealRandomAccessible(
						kdTree.numDimensions(),
						new LocationToSpotLabelSupplier(),
						UnsignedShortType::new );

		if ( kdTree.numDimensions() == 2 )
			rra = RealViews.addDimension( rra );

		// Make the RRA zeroMin
		// This makes it easier to render the data in BVV
		// Moreover, any downstream computation will be easier,
		// because the support for non-zero-min random accessible in ImgLib2 is
		// not very consistent.
		AffineTransform3D translateToZeroMin = new AffineTransform3D();
		translateToZeroMin.translate( Arrays.stream( imageBoundsMin ).map( x -> -x ).toArray() );
		rra = RealViews.affineReal( rra, translateToZeroMin );
		final ArrayList< Integer > timePoints = configureTimePoints();
		FinalRealInterval zeroMinRealInterval = new FinalRealInterval( new double[ 3 ], size );
		final Interval containingZeroMinIntegerInterval = Intervals.smallestContainingInterval( zeroMinRealInterval );

		// accommodate the zeroMin shift of the RRA
		affineTransform3D = translateToZeroMin.copy().inverse();

		// TODO: if the spot coordinates are very much sub-integer
		//       this will not work well in BVV, because BVV renders from the
		//       voxel grid data; to improve this we would need to upscale the
		//       spot coordinates and then accommodate for this
		//       in the source's affineTransform
		AffineTransform3D sourceTransform = new AffineTransform3D();

		source = new RealRandomAccessibleIntervalTimelapseSource(
				rra,
				containingZeroMinIntegerInterval,
				selectApropriateTyoe( numAnnotations ),
				sourceTransform,
				name,
				true,
				null,
				new FinalVoxelDimensions( "", 1, 1, 1 ) );
	}

	private void setImageBounds()
	{
		imageBoundsMin = new double[ 3 ];
		kdTree.realMin( imageBoundsMin );
		imageBoundsMax = new double[ 3 ];
		kdTree.realMax( imageBoundsMax );

		// enlarge bounding box
		// such that all spots are fully rendered
		double[] size = new double[ 3 ];
		for ( int d = 0; d < 3; d++ )
		{
			size[ d ] = imageBoundsMax[ d ] - imageBoundsMin[ d ];
			// enlarge relative to size
			imageBoundsMin[ d ] -= 0.1 * size[ d ];
			imageBoundsMax[ d ] += 0.1 * size[ d ];
		}
	}

	@NotNull
	private static Type selectApropriateTyoe( int numAnnotations )
	{
		Type type;
		if ( numAnnotations < new UnsignedShortType().getMaxValue() )
			type = new UnsignedShortType();
		else if ( numAnnotations < new UnsignedLongType().getMaxValue() )
			type = new UnsignedLongType();
		else
		{
			throw new UnsupportedOperationException("There are " + numAnnotations + " which are " +
					"too many to be displayed.");
		}
		return type;
	}


	class LocationToSpotLabelSupplier implements Supplier< BiConsumer< RealLocalizable, IntegerType > >
	{
		public LocationToSpotLabelSupplier()
		{
		}

		@Override
		public BiConsumer< RealLocalizable, IntegerType > get()
		{
			return new LocationToSpotLabel();
		}

		private class LocationToSpotLabel implements BiConsumer< RealLocalizable, IntegerType >
		{
			private RadiusNeighborSearchOnKDTree< AS > search;

			public LocationToSpotLabel( )
			{
				search = new RadiusNeighborSearchOnKDTree<>( kdTree );
			}

			@Override
			public void accept( RealLocalizable location, IntegerType value )
			{
				search.search( location, spotRadius, true );
				if ( search.numNeighbors() > 0 )
				{
					final Sampler< AS > sampler = search.getSampler( 0 );
					final AS annotatedSpot = sampler.get();
					value.setInteger( annotatedSpot.label() );
				}
				else
				{
					// background
					value.setInteger( 0 );
				}
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
	public SourcePair< T > getSourcePair()
	{
		if ( sourcePair == null )
		{
			transformedSource = new TransformedSource( source );
			transformedSource.setFixedTransform( affineTransform3D );
			sourcePair = new DefaultSourcePair( transformedSource, null );
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
		if ( mask != null )
		{
			// The mask contains potential previous transforms already,
			// thus we add the new transform on top.
			mask = mask.transform( affineTransform3D.inverse() );
		}

		this.affineTransform3D.preConcatenate( affineTransform3D );
		if ( transformedSource != null )
			transformedSource.setFixedTransform( this.affineTransform3D );

		for ( ImageListener listener : listeners.list )
			listener.imageChanged();
	}

	@Override
	public RealMaskRealInterval getMask()
	{
		return mask;
	}

	@Override
	public void setMask( RealMaskRealInterval mask )
	{
		this.mask = mask;
	}
}
