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
package org.embl.mobie.lib.bdv.view;

import bdv.tools.brightness.ConverterSetup;
import bdv.viewer.SourceAndConverter;
import ij.IJ;
import net.imglib2.converter.Converter;
import org.embl.mobie.lib.MoBIE;
import org.embl.mobie.lib.color.ColorHelper;
import org.embl.mobie.lib.color.opacity.AdjustableOpacityColorConverter;
import org.embl.mobie.lib.serialize.display.ImageDisplay;
import org.embl.mobie.lib.image.Image;
import net.imglib2.display.RealARGBColorConverter;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.display.ColorChanger;

public class ImageSliceView< T extends NumericType< T > & RealType< T > > extends AbstractSliceView
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

			// below command will configure opacity,
			// blending mode and visibility
			display.sliceViewer.show( sourceAndConverter, display );

			adaptContrastLimits( sourceAndConverter );
		}
	}

	private SourceAndConverter createSourceAndConverter( Image< T > image )
	{
		final Converter< T, ARGBType > converter = createConverterToARGB( image.getSourcePair().getSource().getType() );
		final SourceAndConverter volatileSac = new SourceAndConverter( image.getSourcePair().getVolatileSource(), converter );
		final SourceAndConverter combinedSac = new SourceAndConverter( image.getSourcePair().getSource(), converter, volatileSac );
		return combinedSac;
	}

	private Converter< T, ARGBType > createConverterToARGB( final T t )
	{
		final double typeMin = Math.max( 0, Math.min( t.getMinValue(), 65535 ) );
		final double typeMax = Math.max( 0, Math.min( t.getMaxValue(), 65535 ) );
		final RealARGBColorConverter< T > converter = RealARGBColorConverter.create( t, typeMin, typeMax );
		return new AdjustableOpacityColorConverter( converter );
	}

	private void adaptContrastLimits( SourceAndConverter< ? > sourceAndConverter )
	{
		final double[] contrastLimits = display.getContrastLimits();
		if ( contrastLimits != null )
		{
			final ConverterSetup converterSetup = SourceAndConverterServices.getSourceAndConverterService().getConverterSetup( sourceAndConverter );
			converterSetup.setDisplayRange( contrastLimits[ 0 ], contrastLimits[ 1 ] );
		}
	}

	private void adaptColor( SourceAndConverter< ? > sourceAndConverter )
	{
		if ( display.getColor() == null ) return;

		final String color = display.getColor();

		ARGBType argbType;
		if ( color.equals( "randomFromGlasbey" ) )
			argbType = ColorHelper.getPseudoRandomGlasbeyARGBType( sourceAndConverter.getSpimSource().getName() );
		else
			argbType = ColorHelper.getARGBType( color );

		if ( argbType == null )
		{
			IJ.log( "[WARN] Could not parse color: " + color );
			return;
		}

		new ColorChanger( sourceAndConverter, argbType ).run();
	}

}
