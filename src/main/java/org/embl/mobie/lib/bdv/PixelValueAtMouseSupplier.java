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
package org.embl.mobie.lib.bdv;

import bdv.util.BdvHandle;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import net.imglib2.RealPoint;
import net.imglib2.RealRandomAccessible;
import net.imglib2.Volatile;
import net.imglib2.realtransform.AffineTransform3D;
import org.embl.mobie.lib.data.DataStore;
import org.embl.mobie.lib.image.RegionAnnotationImage;
import org.embl.mobie.lib.source.AnnotationType;
import org.embl.mobie.lib.source.SourceHelper;
import org.embl.mobie.lib.source.SourceWrapper;
import org.embl.mobie.lib.source.label.AnnotatedLabelSource;
import org.embl.mobie.lib.source.label.VolatileAnnotatedLabelSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public class PixelValueAtMouseSupplier implements Supplier< List< PixelValueAtMouseSupplier.PixelValueSample > >
{
	public enum SampleStatus
	{
		AVAILABLE,
		LOADING,
		NOT_AVAILABLE
	}

	public static class PixelValueSample
	{
		private final String sourceName;
		private final Object value;
		private final int resolutionLevel;
		private final SampleStatus status;

		public PixelValueSample( String sourceName, Object value, int resolutionLevel, SampleStatus status )
		{
			this.sourceName = sourceName;
			this.value = value;
			this.resolutionLevel = resolutionLevel;
			this.status = status;
		}

		public String getSourceName()
		{
			return sourceName;
		}

		public Object getValue()
		{
			return value;
		}

		public int getResolutionLevel()
		{
			return resolutionLevel;
		}

		public SampleStatus getStatus()
		{
			return status;
		}

		@Override
		public boolean equals( Object o )
		{
			if ( this == o ) return true;
			if ( o == null || getClass() != o.getClass() ) return false;
			final PixelValueSample that = ( PixelValueSample ) o;
			return resolutionLevel == that.resolutionLevel
					&& Objects.equals( sourceName, that.sourceName )
					&& Objects.equals( value, that.value )
					&& status == that.status;
		}

		@Override
		public int hashCode()
		{
			return Objects.hash( sourceName, value, resolutionLevel, status );
		}
	}

	private final SourcesAtMousePositionSupplier sourcesAtMousePositionSupplier;
	private final BdvHandle bdvHandle;
	private final RealPoint globalPosition = new RealPoint( 3 );
	private final AffineTransform3D sourceTransform = new AffineTransform3D();
	private final RealPoint positionInSource = new RealPoint( 3 );

	public PixelValueAtMouseSupplier( BdvHandle bdvHandle, boolean is2D )
	{
		this.bdvHandle = bdvHandle;
		this.sourcesAtMousePositionSupplier = new SourcesAtMousePositionSupplier( bdvHandle, is2D );
	}

	@Override
	public List< PixelValueSample > get()
	{
		bdvHandle.getBdvHandle().getViewerPanel().getGlobalMouseCoordinates( globalPosition );
		final int timePoint = bdvHandle.getViewerPanel().state().getCurrentTimepoint();

		final ArrayList< PixelValueSample > samples = new ArrayList<>( 8 );

		for ( SourceAndConverter< ? > sourceAndConverter : sourcesAtMousePositionSupplier.get() )
		{
			if ( DataStore.sourceToImage().get( sourceAndConverter ) instanceof RegionAnnotationImage )
				continue;

			final Source< ? > source = sourceAndConverter.getSpimSource();
			if ( !source.isPresent( timePoint ) )
				continue;

			final Source< ? > sourceToSample = getSourceToSample( sourceAndConverter, source, timePoint );
			if ( sourceToSample == null )
				continue;

			final PixelValueSample sample = sampleBestAvailableValue( source.getName(), sourceToSample, timePoint, globalPosition );
			samples.add( sample );
		}

		return samples;
	}

	private Source< ? > getSourceToSample( SourceAndConverter< ? > sourceAndConverter, Source< ? > source, int timePoint )
	{
		final SourceAndConverter< ? > volatileSac = sourceAndConverter.asVolatile();

		if ( source.getType() instanceof AnnotationType )
		{
			if ( volatileSac != null )
			{
				final VolatileAnnotatedLabelSource< ?, ?, ? > volatileAnnotatedLabelSource =
						SourceHelper.unwrapSource( volatileSac.getSpimSource(), VolatileAnnotatedLabelSource.class );
				if ( volatileAnnotatedLabelSource != null )
				{
					final Source< ? > volatileLabelSource = volatileAnnotatedLabelSource.getWrappedSource();
					if ( volatileLabelSource != null && volatileLabelSource.isPresent( timePoint ) )
						return volatileLabelSource;
				}
			}

			final AnnotatedLabelSource< ?, ? > annotatedLabelSource =
					SourceHelper.unwrapSource( source, AnnotatedLabelSource.class );
			if ( annotatedLabelSource != null )
			{
				final Source< ? > labelSource = annotatedLabelSource.getWrappedSource();
				if ( labelSource != null && labelSource.isPresent( timePoint ) )
					return labelSource;
			}

			final SourceWrapper< ? > sourceWrapper = SourceHelper.unwrapSource( source, SourceWrapper.class );
			if ( sourceWrapper != null )
			{
				final Source< ? > wrappedSource = sourceWrapper.getWrappedSource();
				if ( wrappedSource != null && wrappedSource.isPresent( timePoint ) && !( wrappedSource.getType() instanceof AnnotationType ) )
					return wrappedSource;
			}

			return null;
		}

		if ( volatileSac != null )
		{
			final Source< ? > volatileSource = volatileSac.getSpimSource();
			if ( volatileSource != null && volatileSource.isPresent( timePoint ) )
				return volatileSource;
		}

		return source;
	}

	private PixelValueSample sampleBestAvailableValue( String sourceName, Source< ? > source, int timePoint, RealPoint globalPosition )
	{
		final int numLevels = source.getNumMipmapLevels();
		Object highestResolutionFoundValue = null;
		int highestResolutionFoundLevel = -1;

		for ( int level = numLevels - 1; level >= 0; level-- )
		{
			final Object value = sampleValueAtLevel( source, timePoint, level, globalPosition );

			if ( value != null )
			{
				highestResolutionFoundValue = value;
				highestResolutionFoundLevel = level;
				continue;
			}

			// Stop immediately once one finer step is unavailable, to avoid triggering loads.
			if ( highestResolutionFoundLevel >= 0 )
				return new PixelValueSample( sourceName, highestResolutionFoundValue, highestResolutionFoundLevel, SampleStatus.AVAILABLE );

			return new PixelValueSample( sourceName, null, level, SampleStatus.LOADING );
		}

		if ( highestResolutionFoundLevel >= 0 )
			return new PixelValueSample( sourceName, highestResolutionFoundValue, highestResolutionFoundLevel, SampleStatus.AVAILABLE );

		return new PixelValueSample( sourceName, null, -1, SampleStatus.NOT_AVAILABLE );
	}

	@SuppressWarnings( { "rawtypes" } )
	private Object getNearestNeighborValue( Source< ? > source, int timePoint, int level, RealPoint positionInSource )
	{
		final RealRandomAccessible interpolatedSource = source.getInterpolatedSource( timePoint, level, Interpolation.NEARESTNEIGHBOR );
		return interpolatedSource.getAt( positionInSource );
	}

	private Object sampleValueAtLevel( Source< ? > source, int timePoint, int level, RealPoint globalPosition )
	{
		try
		{
			source.getSourceTransform( timePoint, level, sourceTransform );
			sourceTransform.inverse().apply( globalPosition, positionInSource );

			Object value = getNearestNeighborValue( source, timePoint, level, positionInSource );

			if ( value instanceof Volatile )
			{
				final Volatile< ? > volatileValue = ( Volatile< ? > ) value;
				if ( !volatileValue.isValid() )
					return null;

				value = volatileValue.get();
			}

			return value;
		}
		catch ( Exception e )
		{
			return null;
		}
	}

}