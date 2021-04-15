package de.embl.cba.mobie2.view;

import bdv.tools.brightness.ConverterSetup;
import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import de.embl.cba.mobie2.color.AdjustableOpacityColorConverter;
import de.embl.cba.mobie2.color.VolatileAdjustableOpacityColorConverter;
import de.embl.cba.mobie2.display.ImageDisplay;
import de.embl.cba.mobie2.display.Display;
import de.embl.cba.mobie2.open.SourceAndConverterSupplier;
import de.embl.cba.mobie2.transform.TransformerHelper;
import de.embl.cba.tables.color.ColorUtils;
import net.imglib2.Volatile;
import net.imglib2.converter.Converter;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import sc.fiji.bdvpg.bdv.projector.BlendingMode;
import sc.fiji.bdvpg.behaviour.SourceAndConverterContextMenuClickBehaviour;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterBdvDisplayService;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.display.ColorChanger;
import sc.fiji.bdvpg.sourceandconverter.display.ConverterChanger;

import java.util.ArrayList;
import java.util.List;

public class ImageSliceView
{
	private final SourceAndConverterBdvDisplayService displayService;
	private final ImageDisplay imageDisplay;
	private final BdvHandle bdvHandle;
	private final SourceAndConverterSupplier sourceAndConverterSupplier;
	private final SourceAndConverterService sacService;

	public ImageSliceView( ImageDisplay imageDisplay, BdvHandle bdvHandle, SourceAndConverterSupplier sourceAndConverterSupplier  )
	{
		this.imageDisplay = imageDisplay;
		this.bdvHandle = bdvHandle;
		this.sourceAndConverterSupplier = sourceAndConverterSupplier;

		displayService = SourceAndConverterServices.getSourceAndConverterDisplayService();
		sacService = ( SourceAndConverterService ) SourceAndConverterServices.getSourceAndConverterService();

		show();
	}

	private void show( )
	{
		List< SourceAndConverter< ? > > sourceAndConverters = new ArrayList<>();

		// open
		for ( String sourceName : imageDisplay.getSources() )
		{
			sourceAndConverters.add( sourceAndConverterSupplier.get( sourceName ) );
		}

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
			final Converter< ? extends Volatile< ? >, ARGBType > volatileConverter = sourceAndConverter.asVolatile().getConverter();
			sourceAndConverter = new ConverterChanger( sourceAndConverter, new AdjustableOpacityColorConverter(  converter ), new VolatileAdjustableOpacityColorConverter( volatileConverter ) ).get();

			// adapt color
			new ColorChanger( sourceAndConverter, ColorUtils.getARGBType(  imageDisplay.getColor() ) ).run();

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

		sacService.getUI().hide();

		imageDisplay.sourceAndConverters = displayedSourceAndConverters;
	}



	public void close( )
	{
		for ( SourceAndConverter< ? > sourceAndConverter : imageDisplay.sourceAndConverters )
		{
			SourceAndConverterServices.getSourceAndConverterDisplayService().removeFromAllBdvs( sourceAndConverter );
		}
	}
}
