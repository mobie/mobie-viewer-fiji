package org.embl.mobie.viewer.bdv.view;

import bdv.tools.brightness.ConverterSetup;
import bdv.viewer.SourceAndConverter;
import de.embl.cba.bdv.utils.lut.GlasbeyARGBLut;
import de.embl.cba.tables.color.ColorUtils;
import ij.IJ;
import net.imglib2.Volatile;
import net.imglib2.converter.Converter;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.color.opacity.AdjustableOpacityColorConverter;
import org.embl.mobie.viewer.color.opacity.VolatileAdjustableOpacityColorConverter;
import org.embl.mobie.viewer.display.ImageSourceDisplay;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.display.ColorChanger;
import sc.fiji.bdvpg.sourceandconverter.display.ConverterChanger;

import java.util.HashMap;
import java.util.Map;

import static de.embl.cba.bdv.utils.converters.RandomARGBConverter.goldenRatio;

public class ImageSliceView extends AbstractSliceView
{
	static { net.imagej.patcher.LegacyInjector.preinit(); }

	private final ImageSourceDisplay display;

	public ImageSliceView( MoBIE moBIE, ImageSourceDisplay display )
	{
		super( moBIE, display );
		this.display = display;
		show();
	}

	private void show( )
	{
		Map< String, SourceAndConverter< ? > > sourceNameToSourceAndConverter = new HashMap<>();
		for ( String name : display.getSources() ) {
			sourceNameToSourceAndConverter.put( name, moBIE.getTransformedSourceAndConverter( name ) );
		}

		for ( String name : sourceNameToSourceAndConverter.keySet() )
		{
			SourceAndConverter< ? > sourceAndConverter = sourceNameToSourceAndConverter.get( name );
			sourceAndConverter = replaceConverterWithAdjustableOpacityConverter( sourceAndConverter );
			adaptImageColor( sourceAndConverter );

			display.sliceViewer.show( sourceAndConverter, display );

			// adapt contrast limits
			final ConverterSetup converterSetup = SourceAndConverterServices.getSourceAndConverterService().getConverterSetup( sourceAndConverter );
			converterSetup.setDisplayRange( display.getContrastLimits()[ 0 ], display.getContrastLimits()[ 1 ] );
		}
	}

	private SourceAndConverter< ? > replaceConverterWithAdjustableOpacityConverter( SourceAndConverter< ? > sourceAndConverter )
	{
		final Converter< RealType, ARGBType > converter = ( Converter< RealType, ARGBType > ) sourceAndConverter.getConverter();
		final AdjustableOpacityColorConverter adjustableOpacityColorConverter = new AdjustableOpacityColorConverter( converter );
		final Converter< ? extends Volatile< ? >, ARGBType > volatileConverter = sourceAndConverter.asVolatile().getConverter();
		final VolatileAdjustableOpacityColorConverter volatileAdjustableOpacityColorConverter = new VolatileAdjustableOpacityColorConverter( volatileConverter );
		sourceAndConverter = new ConverterChanger( sourceAndConverter, adjustableOpacityColorConverter, volatileAdjustableOpacityColorConverter ).get();
		return sourceAndConverter;
	}

	private void adaptImageColor( SourceAndConverter< ? > sourceAndConverter )
	{
		if ( display.getColor() != null )
		{
			final String color = display.getColor();

			if ( color.equals( "randomFromGlasbey" ) )
			{
				final GlasbeyARGBLut glasbeyARGBLut = new GlasbeyARGBLut();
				double random = sourceAndConverter.getSpimSource().getName().hashCode() * goldenRatio;
				random = random - ( long ) Math.floor( random );
				final int argb = glasbeyARGBLut.getARGB( random );
				new ColorChanger( sourceAndConverter, new ARGBType( argb ) ).run();
			}
			else
			{
				final ARGBType argbType = ColorUtils.getARGBType( color );
				if ( argbType == null )
				{
					IJ.log( "[WARN] Could not parse color: " + color );
				} else
				{
					new ColorChanger( sourceAndConverter, argbType ).run();
				}
			}
		}
	}
}
