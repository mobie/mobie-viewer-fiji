package de.embl.cba.mobie.bdv.view;

import bdv.tools.brightness.ConverterSetup;
import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import de.embl.cba.bdv.utils.lut.GlasbeyARGBLut;
import de.embl.cba.mobie.MoBIE;
import de.embl.cba.mobie.Utils;
import de.embl.cba.mobie.bdv.render.BlendingMode;
import de.embl.cba.mobie.color.OpacityAdjuster;
import de.embl.cba.mobie.color.opacity.AdjustableOpacityColorConverter;
import de.embl.cba.mobie.color.opacity.VolatileAdjustableOpacityColorConverter;
import de.embl.cba.mobie.display.ImageSourceDisplay;
import de.embl.cba.mobie.transform.TransformHelper;
import de.embl.cba.tables.color.ColorUtils;
import ij.IJ;
import net.imglib2.FinalRealInterval;
import net.imglib2.Volatile;
import net.imglib2.converter.Converter;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterBdvDisplayService;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.display.ColorChanger;
import sc.fiji.bdvpg.sourceandconverter.display.ConverterChanger;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static de.embl.cba.bdv.utils.converters.RandomARGBConverter.goldenRatio;

public class ImageSliceView
{
	private final SourceAndConverterBdvDisplayService displayService;
	private final MoBIE moBIE;
	private final ImageSourceDisplay display;
	private final BdvHandle bdvHandle;
	private final SourceAndConverterService sacService;

	public ImageSliceView( MoBIE moBIE, ImageSourceDisplay display, BdvHandle bdvHandle )
	{
		this.moBIE = moBIE;
		this.display = display;
		this.bdvHandle = bdvHandle;

		displayService = SourceAndConverterServices.getBdvDisplayService();
		sacService = ( SourceAndConverterService ) SourceAndConverterServices.getSourceAndConverterService();

		show();
	}

	private void show( )
	{
		// show
		final List< ? extends SourceAndConverter< ? > > sourceAndConverters = display.getSources().stream().map( name -> moBIE.getSourceAndConverter( name ) ).collect( Collectors.toList() );

		display.sourceAndConverters = new ArrayList<>();
		for ( SourceAndConverter< ? > sourceAndConverter : sourceAndConverters )
		{
			sourceAndConverter = adaptConverter( sourceAndConverter );

			// set opacity
			OpacityAdjuster.adjustOpacity( sourceAndConverter, display.getOpacity() );

			// set color
			adaptImageColor( sourceAndConverter );

			// set blending mode
			if ( display.getBlendingMode() != null )
				SourceAndConverterServices.getSourceAndConverterService().setMetadata( sourceAndConverter, BlendingMode.BLENDING_MODE, display.getBlendingMode() );

			// show
			displayService.show( bdvHandle, display.isVisible(), sourceAndConverter );

			// adapt contrast limits
			final ConverterSetup converterSetup = displayService.getConverterSetup( sourceAndConverter );
			converterSetup.setDisplayRange( display.getContrastLimits()[ 0 ], display.getContrastLimits()[ 1 ] );

			// register	the actually displayed sac (for serialisation)
			display.sourceAndConverters.add( sourceAndConverter );
		}
	}

	private SourceAndConverter< ? > adaptConverter( SourceAndConverter< ? > sourceAndConverter )
	{
		// replace converter such that one can change the opacity
		// (this changes the hash-code of the sourceAndConverter)

		// TODO: understand this madness
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

	public void close( )
	{
		for ( SourceAndConverter< ? > sourceAndConverter : display.sourceAndConverters )
		{
			moBIE.closeSourceAndConverter( sourceAndConverter );
		}
		display.sourceAndConverters.clear();
	}

}
