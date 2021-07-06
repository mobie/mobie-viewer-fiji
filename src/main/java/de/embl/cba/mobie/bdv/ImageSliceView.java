package de.embl.cba.mobie.bdv;

import bdv.tools.brightness.ConverterSetup;
import bdv.util.BdvHandle;
import bdv.util.projector.mixed.BlendingMode;
import bdv.viewer.SourceAndConverter;
import de.embl.cba.bdv.utils.lut.GlasbeyARGBLut;
import de.embl.cba.mobie.MoBIE;
import de.embl.cba.mobie.color.OpacityAdjuster;
import de.embl.cba.mobie.color.opacity.AdjustableOpacityColorConverter;
import de.embl.cba.mobie.color.opacity.VolatileAdjustableOpacityColorConverter;
import de.embl.cba.mobie.display.ImageSourceDisplay;
import de.embl.cba.mobie.open.SourceAndConverterSupplier;
import de.embl.cba.mobie.transform.TransformerHelper;
import de.embl.cba.tables.color.ColorUtils;
import ij.IJ;
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

import static de.embl.cba.bdv.utils.converters.RandomARGBConverter.goldenRatio;

public class ImageSliceView
{
	private final SourceAndConverterBdvDisplayService displayService;
	private final MoBIE moBIE;
	private final ImageSourceDisplay imageDisplay;
	private final BdvHandle bdvHandle;
	private final SourceAndConverterSupplier sourceAndConverterSupplier;
	private final SourceAndConverterService sacService;

	public ImageSliceView( MoBIE moBIE, ImageSourceDisplay imageDisplay, BdvHandle bdvHandle, SourceAndConverterSupplier sourceAndConverterSupplier )
	{
		this.moBIE = moBIE;
		this.imageDisplay = imageDisplay;
		this.bdvHandle = bdvHandle;
		this.sourceAndConverterSupplier = sourceAndConverterSupplier;

		displayService = SourceAndConverterServices.getBdvDisplayService();
		sacService = ( SourceAndConverterService ) SourceAndConverterServices.getSourceAndConverterService();

		show();
	}

	private void show( )
	{
		List< SourceAndConverter< ? > > sourceAndConverters = sourceAndConverterSupplier.get( imageDisplay.getSources() );

		// transform
		sourceAndConverters = TransformerHelper.transformSourceAndConverters( sourceAndConverters, imageDisplay.sourceTransformers );

		// show
		List< SourceAndConverter< ? > > displayedSourceAndConverters = new ArrayList<>();
		for ( SourceAndConverter< ? > sourceAndConverter : sourceAndConverters )
		{
			// replace converter such that one can change the opacity
			// (this changes the hash-code of the sourceAndConverter)

			// TODO: understand this madness
			final Converter< RealType, ARGBType > converter = ( Converter< RealType, ARGBType > ) sourceAndConverter.getConverter();
			final AdjustableOpacityColorConverter adjustableOpacityColorConverter = new AdjustableOpacityColorConverter( converter );
			final Converter< ? extends Volatile< ? >, ARGBType > volatileConverter = sourceAndConverter.asVolatile().getConverter();
			final VolatileAdjustableOpacityColorConverter volatileAdjustableOpacityColorConverter = new VolatileAdjustableOpacityColorConverter( volatileConverter );
			sourceAndConverter = new ConverterChanger( sourceAndConverter, adjustableOpacityColorConverter, volatileAdjustableOpacityColorConverter ).get();

			// set opacity
			OpacityAdjuster.adjustOpacity( sourceAndConverter, imageDisplay.getOpacity() );

			// set color
			adaptImageColor( sourceAndConverter );

			// set blending mode
			if ( imageDisplay.getBlendingMode() != null )
				SourceAndConverterServices.getSourceAndConverterService().setMetadata( sourceAndConverter, BlendingMode.BLENDING_MODE, imageDisplay.getBlendingMode());

			// show
			displayService.show( bdvHandle, sourceAndConverter );

			// adapt contrast limits
			final ConverterSetup converterSetup = displayService.getConverterSetup( sourceAndConverter );
			converterSetup.setDisplayRange( imageDisplay.getContrastLimits()[ 0 ], imageDisplay.getContrastLimits()[ 1 ] );

			displayedSourceAndConverters.add( sourceAndConverter );
		}

		imageDisplay.sourceAndConverters = displayedSourceAndConverters;
	}

	private void adaptImageColor( SourceAndConverter< ? > sourceAndConverter )
	{
		if ( imageDisplay.getColor() != null )
		{
			final String color = imageDisplay.getColor();

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
		for ( SourceAndConverter< ? > sourceAndConverter : imageDisplay.sourceAndConverters )
		{
			moBIE.closeSourceAndConverter( sourceAndConverter );
		}
		imageDisplay.sourceAndConverters.clear();
	}

}
