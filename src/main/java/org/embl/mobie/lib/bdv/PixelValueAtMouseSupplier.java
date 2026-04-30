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
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import org.embl.mobie.lib.data.DataStore;
import org.embl.mobie.lib.image.RegionAnnotationImage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class PixelValueAtMouseSupplier implements Supplier< List< String > >
{
	private final SourcesAtMousePositionSupplier sourcesAtMousePositionSupplier;
	private final BdvHandle bdvHandle;

	public PixelValueAtMouseSupplier( BdvHandle bdvHandle, boolean is2D )
	{
		this.bdvHandle = bdvHandle;
		this.sourcesAtMousePositionSupplier = new SourcesAtMousePositionSupplier( bdvHandle, is2D );
	}

	@Override
	public List< String > get()
	{
		final CalibratedMousePositionProvider positionProvider = new CalibratedMousePositionProvider( bdvHandle );
		final int timePoint = positionProvider.getTimePoint();
		final RealPoint globalPosition = positionProvider.getPositionAsRealPoint();

		final Collection< SourceAndConverter< ? > > sourceAndConverters =
				sourcesAtMousePositionSupplier.get().stream()
				.filter( sac -> ! ( DataStore.sourceToImage().get( sac ) instanceof RegionAnnotationImage ) )
				.collect( Collectors.toList() );
		
		final ArrayList< String > lines = new ArrayList<>();

		for ( SourceAndConverter< ? > sourceAndConverter : sourceAndConverters )
		{
			final Source< ? > source = sourceAndConverter.getSpimSource();
			if ( !source.isPresent( timePoint ) )
				continue;

			final String value = sampleValueAsString( source, timePoint, globalPosition );
			lines.add( source.getName() + ": " + value );
		}

		if ( lines.isEmpty() )
			lines.add( "No source at cursor" );

		return lines;
	}

	private String sampleValueAsString( Source< ? > source, int timePoint, RealPoint globalPosition )
	{
		try
		{
			final AffineTransform3D sourceTransform = new AffineTransform3D();
			source.getSourceTransform( timePoint, 0, sourceTransform );

			final RealPoint positionInSource = new RealPoint( 3 );
			sourceTransform.inverse().apply( globalPosition, positionInSource );

			final Object value = getNearestNeighborValue( source, timePoint, positionInSource );
			return formatValue( value );
		}
		catch ( Exception e )
		{
			return "n/a";
		}
	}

	@SuppressWarnings( { "rawtypes" } )
	private Object getNearestNeighborValue( Source< ? > source, int timePoint, RealPoint positionInSource )
	{
		final RealRandomAccessible interpolatedSource = source.getInterpolatedSource( timePoint, 0, Interpolation.NEARESTNEIGHBOR );
		return interpolatedSource.getAt( positionInSource );
	}

	private String formatValue( Object value )
	{
		if ( value == null )
			return "n/a";

		if ( value instanceof ARGBType )
			return String.format( Locale.US, "0x%08X", ( ( ARGBType ) value ).get() );

		if ( value instanceof IntegerType )
			return Long.toString( ( ( IntegerType< ? > ) value ).getIntegerLong() );

		if ( value instanceof RealType )
			return String.format( Locale.US, "%.6g", ( ( RealType< ? > ) value ).getRealDouble() );

		return value.toString();
	}
}