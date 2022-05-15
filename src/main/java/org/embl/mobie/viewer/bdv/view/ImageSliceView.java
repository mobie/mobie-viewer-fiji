package org.embl.mobie.viewer.bdv.view;

import bdv.tools.brightness.ConverterSetup;
import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import de.embl.cba.bdv.utils.lut.GlasbeyARGBLut;
import net.imglib2.realtransform.AffineTransform3D;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.bdv.render.BlendingMode;
import org.embl.mobie.viewer.color.OpacityAdjuster;
import org.embl.mobie.viewer.color.opacity.AdjustableOpacityColorConverter;
import org.embl.mobie.viewer.color.opacity.VolatileAdjustableOpacityColorConverter;
import org.embl.mobie.viewer.display.ImageSourceDisplay;
import de.embl.cba.tables.color.ColorUtils;
import ij.IJ;
import net.imglib2.Volatile;
import net.imglib2.converter.Converter;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.display.ColorChanger;
import sc.fiji.bdvpg.sourceandconverter.display.ConverterChanger;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static de.embl.cba.bdv.utils.converters.RandomARGBConverter.goldenRatio;

public class ImageSliceView
{
	static { net.imagej.patcher.LegacyInjector.preinit(); }

	private final MoBIE moBIE;
	private final ImageSourceDisplay display;
	private final BdvHandle bdvHandle;

	public ImageSliceView( MoBIE moBIE, ImageSourceDisplay display, BdvHandle bdvHandle )
	{
		this.moBIE = moBIE;
		this.display = display;
		this.bdvHandle = bdvHandle;

		show();
	}

	private void show( )
	{
		// show
		Map< String, SourceAndConverter< ? > > sourceNameToSourceAndConverter = new HashMap<>();
		for ( String name : display.getSources() ) {
			sourceNameToSourceAndConverter.put( name, moBIE.getTransformedSourceAndConverter( name ) );
		}

		display.sourceNameToSourceAndConverter = new HashMap<>();
		for ( String name : sourceNameToSourceAndConverter.keySet() )
		{
			SourceAndConverter< ? > sourceAndConverter = sourceNameToSourceAndConverter.get( name );
			sourceAndConverter = adaptConverter( sourceAndConverter );

			// set opacity
			OpacityAdjuster.adjustOpacity( sourceAndConverter, display.getOpacity() );

			// set color
			adaptImageColor( sourceAndConverter );

			// set blending mode
			if ( display.getBlendingMode() != null )
				SourceAndConverterServices.getSourceAndConverterService().setMetadata( sourceAndConverter, BlendingMode.BLENDING_MODE, display.getBlendingMode() );

			// show
			SourceAndConverterServices.getBdvDisplayService().show( bdvHandle, display.isVisible(), sourceAndConverter );

			final AffineTransform3D transform3D = new AffineTransform3D();
			sourceAndConverter.getSpimSource().getSourceTransform( 0,0, transform3D );

			// adapt contrast limits
			final ConverterSetup converterSetup = SourceAndConverterServices.getSourceAndConverterService().getConverterSetup( sourceAndConverter );
			converterSetup.setDisplayRange( display.getContrastLimits()[ 0 ], display.getContrastLimits()[ 1 ] );

			// register	the actually displayed sac (for serialisation)
			display.sourceNameToSourceAndConverter.put( name, sourceAndConverter );
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

	public boolean isDisplayVisible() {
		Collection<SourceAndConverter<?>> sourceAndConverters = display.sourceNameToSourceAndConverter.values();
		// check if first source is visible
		return SourceAndConverterServices.getBdvDisplayService().isVisible( sourceAndConverters.iterator().next(), bdvHandle );
	}

	public void close( boolean closeImgLoader )
	{
		for ( SourceAndConverter< ? > sourceAndConverter : display.sourceNameToSourceAndConverter.values() )
		{
			moBIE.closeSourceAndConverter( sourceAndConverter, closeImgLoader );
		}
		display.sourceNameToSourceAndConverter.clear();
	}

}
