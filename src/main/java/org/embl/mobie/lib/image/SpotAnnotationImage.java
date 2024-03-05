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
import org.embl.mobie.lib.annotation.AnnotatedSpot;
import org.embl.mobie.lib.source.AnnotationType;
import org.embl.mobie.lib.source.RealRandomAccessibleIntervalTimelapseSource;
import org.embl.mobie.lib.table.AnnData;
import org.embl.mobie.lib.table.DefaultAnnData;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class SpotAnnotationImage< AS extends AnnotatedSpot > implements AnnotationImage< AS >
{
	private final String name;
	private final DefaultAnnData< AS > annData;
	private Source< ? extends Volatile< UnsignedIntType > > volatileSource = null;
	private KDTree< AS > kdTree;
	private RealMaskRealInterval mask;
	private double radius;
	private double[] boundingBoxMin;
	private double[] boundingBoxMax;
	private AffineTransform3D affineTransform3D;
	private Source< AnnotationType< AS > > source;
	private TransformedSource< AnnotationType< AS > > transformedSource;

	public SpotAnnotationImage( String name, DefaultAnnData< AS > annData, double radius, @Nullable double[] boundingBoxMin, @Nullable double[] boundingBoxMax )
	{
		this.name = name;
		this.annData = annData;
		this.radius = radius;
		this.boundingBoxMin = boundingBoxMin;
		this.boundingBoxMax = boundingBoxMax;
		affineTransform3D = new AffineTransform3D();
		createImage();
	}

	public double getRadius()
	{
		return radius;
	}

	public void setRadius( double radius )
	{
		this.radius = radius;
	}

	private void createImage()
	{
		final ArrayList< AS > annotations = annData.getTable().annotations();
		kdTree = new KDTree( annotations, annotations );
		// TODO: there is a KDTreeFloat implementation in this repo that we could use
		//   to save memory

		if ( boundingBoxMin == null )
			boundingBoxMin = kdTree.minAsDoubleArray();

		if ( boundingBoxMax == null )
			boundingBoxMax = kdTree.maxAsDoubleArray();

		mask = GeomMasks.closedBox( boundingBoxMin, boundingBoxMax );

		// TODO: code duplication with RegionLabelImage
		final ArrayList< Integer > timePoints = configureTimePoints();
		final Interval interval = Intervals.smallestContainingInterval( getMask() );
		final AS annotatedSpot = annData.getTable().annotation( 0 );
		final FunctionRealRandomAccessible< AnnotationType< AS > > realRandomAccessible = new FunctionRealRandomAccessible( 3, new LocationToAnnotatedSpotSupplier(), () -> new AnnotationType<>( annotatedSpot ) );
		//final RealRandomAccessible interpolate = Views.interpolate( new NearestNeighborSearchOnKDTree( kdTree ), new NearestNeighborSearchInterpolatorFactory() );
		source = new RealRandomAccessibleIntervalTimelapseSource( realRandomAccessible, interval, new AnnotationType<>( annotatedSpot ), new AffineTransform3D(), name, true, null, new FinalVoxelDimensions( "", 1, 1, 1 ) );
	}

	@Override
	public AnnData< AS > getAnnData()
	{
		return annData;
	}

	class LocationToAnnotatedSpotSupplier implements Supplier< BiConsumer< RealLocalizable, AnnotationType< AS > > >
	{
		public LocationToAnnotatedSpotSupplier()
		{
		}

		@Override
		public BiConsumer< RealLocalizable, AnnotationType< AS > > get()
		{
			return new LocationToAnnotatedSpot();
		}

		private class LocationToAnnotatedSpot implements BiConsumer< RealLocalizable, AnnotationType< AS > >
		{
			private RadiusNeighborSearchOnKDTree< AS > search;

			public LocationToAnnotatedSpot( )
			{
				search = new RadiusNeighborSearchOnKDTree<>( kdTree );
			}

			@Override
			public void accept( RealLocalizable location, AnnotationType< AS > value )
			{
				search.search( location, radius, true );
				if ( search.numNeighbors() > 0 )
				{
					final Sampler< AS > sampler = search.getSampler( 0 );
					final AS annotatedSpot = sampler.get();
					value.setAnnotation( annotatedSpot );
				}
				else
				{
					// background
					value.setAnnotation( null );
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
	public SourcePair< AnnotationType< AS > > getSourcePair()
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
