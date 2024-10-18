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
package org.embl.mobie.lib.bdv.view;

import bdv.tools.brightness.ConverterSetup;
import bdv.viewer.SourceAndConverter;
import ij.IJ;
import net.imglib2.converter.Converter;
import net.imglib2.display.ScaledARGBConverter;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import org.embl.mobie.MoBIE;
import org.embl.mobie.lib.color.ColorHelper;
import org.embl.mobie.lib.color.opacity.MoBIEColorConverter;
import org.embl.mobie.lib.image.Image;
import org.embl.mobie.lib.serialize.display.ImageDisplay;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.display.ColorChanger;

public class ImageSliceView< T extends NumericType< T > > extends AbstractSliceView
{
	static { net.imagej.patcher.LegacyInjector.preinit(); }

	private final ImageDisplay< T > display;

	public ImageSliceView( MoBIE moBIE, ImageDisplay< T > display )
	{
		super( moBIE, display );
		this.display = display;
		show();
	}

	private void show( )
	{
		for ( Image< T > image : display.images() )
		{
			SourceAndConverter< ? > sourceAndConverter = createSourceAndConverter( image );

			adaptColor( sourceAndConverter );

			// set LUT inversion
			Converter< ?, ARGBType > converter = sourceAndConverter.getConverter();
			if ( converter instanceof MoBIEColorConverter ) // should be always true within MoBIE
			{
				( ( MoBIEColorConverter ) converter ).invert( display.invert() );
			}

			// below command will configure opacity,
			// blending mode and visibility, which are generic to all Displays
			display.sliceViewer.show( image, sourceAndConverter, display );

			// adapt Image specific contrast limits after showing the image,
			// because we need the ConverterSetup to exist
			adaptContrastLimits( sourceAndConverter );
		}
	}

	private SourceAndConverter createSourceAndConverter( Image< T > image )
	{
		final T type = image.getSourcePair().getSource().getType();

		if ( type instanceof ARGBType )
		{
			final SourceAndConverter volatileSac = new SourceAndConverter( image.getSourcePair().getVolatileSource(), new ScaledARGBConverter.VolatileARGB( 0, 255 ) );
			final SourceAndConverter sac = new SourceAndConverter( image.getSourcePair().getSource(), new ScaledARGBConverter.ARGB( 0, 255 ), volatileSac );
			return sac;
		}
		else
		{
			final Converter< T, ARGBType > converter = createConverterToARGB( type );
			final SourceAndConverter volatileSac = new SourceAndConverter( image.getSourcePair().getVolatileSource(), converter );
			final SourceAndConverter sac = new SourceAndConverter( image.getSourcePair().getSource(), converter, volatileSac );
			return sac;
		}
	}

	private Converter< T, ARGBType > createConverterToARGB( final T type )
	{
		if ( type instanceof RealType )
		{
			final RealType< ? > realType = ( RealType< ? > ) type;
			final double typeMin = Math.max( 0, Math.min( realType.getMinValue(), 65535 ) );
			final double typeMax = Math.max( 0, Math.min( realType.getMaxValue(), 65535 ) );
			MoBIEColorConverter< ? extends RealType< ? > > converter = new MoBIEColorConverter<>( realType, typeMin, typeMax );
			return ( Converter< T, ARGBType > ) converter;
		}
		else
		{
			throw new UnsupportedOperationException( "Unsupported type " + type.getClass() );
		}
	}

	private void adaptContrastLimits( SourceAndConverter< ? > sourceAndConverter )
	{
		double[] contrastLimits = display.getContrastLimits(
				sourceAndConverter.getSpimSource().getName() );

		if ( contrastLimits != null )
		{
			final ConverterSetup converterSetup =
					SourceAndConverterServices
							.getSourceAndConverterService()
							.getConverterSetup( sourceAndConverter );
			converterSetup.setDisplayRange( contrastLimits[ 0 ], contrastLimits[ 1 ] );
		}
	}

	private void adaptColor( SourceAndConverter< ? > sourceAndConverter )
	{
		if ( display.getColor() == null ) return;

		final String color = display.getColor();

		ARGBType argbType;
		if ( color.equals( "randomFromGlasbey" ) )
			argbType = ColorHelper.getRandomGlasbeyARGBType( sourceAndConverter.getSpimSource().getName() );
		else
			argbType = ColorHelper.getARGBType( color );

		new ColorChanger( sourceAndConverter, argbType ).run();
	}

}
