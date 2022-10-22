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

import bdv.tools.transformation.TransformedSource;
import bdv.viewer.Source;
import net.imglib2.Interval;
import net.imglib2.KDTree;
import net.imglib2.RealLocalizable;
import net.imglib2.Sampler;
import net.imglib2.Volatile;
import net.imglib2.neighborsearch.RadiusNeighborSearchOnKDTree;
import net.imglib2.position.FunctionRealRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.roi.RealMaskRealInterval;
import net.imglib2.roi.geom.GeomMasks;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.util.Intervals;
import org.embl.mobie.viewer.annotation.AnnotatedSpot;
import org.embl.mobie.viewer.source.RealRandomAccessibleIntervalTimelapseSource;
import org.embl.mobie.viewer.source.SourcePair;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.ArrayList;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class SpotLabelImage< AS extends AnnotatedSpot > implements Image< UnsignedIntType >
{
	private final String name;
	private final ArrayList< AS > annotatedSpots;
	private Source< ? extends Volatile< UnsignedIntType > > volatileSource = null;
	private KDTree< AS > kdTree;
	private RealMaskRealInterval mask;
	private double radius;
	private double[] boundingBoxMin;
	private double[] boundingBoxMax;
	private AffineTransform3D affineTransform3D;
	private Source< UnsignedIntType > source;
	private TransformedSource< UnsignedIntType > transformedSource;

	public SpotLabelImage( String name, ArrayList< AS > annotatedSpots, double radius, @Nullable double[] boundingBoxMin, @Nullable double[] boundingBoxMax )
	{
		this.name = name;
		this.annotatedSpots = annotatedSpots;
		this.radius = radius;
		this.boundingBoxMin = boundingBoxMin;
		this.boundingBoxMax = boundingBoxMax;
		affineTransform3D = new AffineTransform3D();
		createLabelImage();
	}

	public double getRadius()
	{
		return radius;
	}

	public void setRadius( double radius )
	{
		this.radius = radius;
	}

	private void createLabelImage()
	{
		long start = System.currentTimeMillis();
		// FIXME We could implement a kdTree that just uses float precision
		//   to save memory.
		kdTree = new KDTree( annotatedSpots, annotatedSpots );
		System.out.println( "Built " + name + " tree with " + annotatedSpots.size() + " elements in " + ( System.currentTimeMillis() - start ) + " ms." );

		if ( boundingBoxMin == null )
			boundingBoxMin = kdTree.minAsDoubleArray();

		if ( boundingBoxMax == null )
			boundingBoxMax = kdTree.maxAsDoubleArray();

		mask = GeomMasks.closedBox( boundingBoxMin, boundingBoxMax );

		// TODO: use those for something?
//		final NearestNeighborSearchOnKDTree< AS > nearestNeighborSearchOnKDTree = new NearestNeighborSearchOnKDTree<>( kdTree );
//		final RadiusNeighborSearchOnKDTree< AS > radiusNeighborSearchOnKDTree = new RadiusNeighborSearchOnKDTree<>( kdTree );

		// TODO: code duplication with RegionLabelImage
		final ArrayList< Integer > timePoints = configureTimePoints();
		final Interval interval = Intervals.smallestContainingInterval( getMask() );
		final FunctionRealRandomAccessible< UnsignedIntType > realRandomAccessible = new FunctionRealRandomAccessible( 3, new SpotLocationToLabelSupplier(), UnsignedIntType::new );
		source = new RealRandomAccessibleIntervalTimelapseSource<>( realRandomAccessible, interval, new UnsignedIntType(), new AffineTransform3D(), name, true, timePoints );
	}

	class SpotLocationToLabelSupplier implements Supplier< BiConsumer< RealLocalizable, UnsignedIntType > >
	{
		public SpotLocationToLabelSupplier()
		{
		}

		@Override
		public BiConsumer< RealLocalizable, UnsignedIntType > get()
		{
			return new LocationToLabel();
		}

		private class LocationToLabel implements BiConsumer< RealLocalizable, UnsignedIntType >
		{
			private RadiusNeighborSearchOnKDTree< AS > search;

			public LocationToLabel( )
			{
				search = new RadiusNeighborSearchOnKDTree<>( kdTree );
			}

			@Override
			public void accept( RealLocalizable location, UnsignedIntType value )
			{
				search.search( location, radius, true );
				if ( search.numNeighbors() > 0 )
				{
					final Sampler< AS > sampler = search.getSampler( 0 );
					final AS annotatedRegion = sampler.get();
					// FIXME It is stupid to just return the label here, because
					//   this is already the spot.
					//   We could probably safe speed and memory because we
					//   do not need the annotationAdapter for the spots!

					value.setInteger( annotatedRegion.label() );
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
	public SourcePair< UnsignedIntType > getSourcePair()
	{
		transformedSource = new TransformedSource( source );
		transformedSource.setFixedTransform( affineTransform3D );
		//final TransformedSource volatileTransformedSource = new TransformedSource( volatileSource, transformedSource );
		return new DefaultSourcePair<>( transformedSource, null );
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
