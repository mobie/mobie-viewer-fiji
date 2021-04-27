package de.embl.cba.mobie2.bdv;

import bdv.tools.brightness.ConverterSetup;
import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import de.embl.cba.mobie2.color.opacity.AdjustableOpacityColorConverter;
import de.embl.cba.mobie2.color.opacity.VolatileAdjustableOpacityColorConverter;
import de.embl.cba.mobie2.display.ImageDisplay;
import de.embl.cba.mobie2.open.SourceAndConverterSupplier;
import de.embl.cba.mobie2.transform.TransformerHelper;
import de.embl.cba.tables.color.ColorUtils;
import ij.IJ;
import net.imagej.ops.OpEnvironment;
import net.imagej.ops.Ops;
import net.imglib2.Volatile;
import net.imglib2.converter.Converter;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import sc.fiji.bdvpg.bdv.projector.BlendingMode;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterBdvDisplayService;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.display.ColorChanger;
import sc.fiji.bdvpg.sourceandconverter.display.ConverterChanger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
		List< SourceAndConverter< ? > > sourceAndConverters = openParallel();

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

		imageDisplay.sourceAndConverters = displayedSourceAndConverters;
	}

	private List< SourceAndConverter< ? > > openSerial()
	{
		List< SourceAndConverter< ? > > sourceAndConverters = new ArrayList<>();

		// open
		final long start = System.currentTimeMillis();
		for ( String sourceName : imageDisplay.getSources() )
		{
			sourceAndConverters.add( sourceAndConverterSupplier.get( sourceName ) );
		}
		System.out.println( "Fetched " + sourceAndConverters.size() + " image source(s) in " + (System.currentTimeMillis() - start) + " ms ");
		return sourceAndConverters;
	}

	private List< SourceAndConverter< ? > > openParallel()
	{
		List< SourceAndConverter< ? > > sourceAndConverters = new CopyOnWriteArrayList<>();

		// open
		final long start = System.currentTimeMillis();
		final int nThreads = 1;
		final ExecutorService executorService = Executors.newFixedThreadPool( nThreads );
		for ( String sourceName : imageDisplay.getSources() )
		{
			executorService.execute( () -> {
				System.out.println( sourceName );
				sourceAndConverters.add( sourceAndConverterSupplier.get( sourceName ) );
				System.out.println( sourceName + " loaded." );
			} );
		}

		executorService.shutdown();
		try {
			executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		} catch (InterruptedException e) {
		}

		System.out.println( "Fetched " + sourceAndConverters.size() + " image source(s) using " + nThreads + " threads in " + (System.currentTimeMillis() - start) + " ms ");

		return sourceAndConverters;
	}

	public void close( )
	{
		for ( SourceAndConverter< ? > sourceAndConverter : imageDisplay.sourceAndConverters )
		{
			SourceAndConverterServices.getSourceAndConverterDisplayService().removeFromAllBdvs( sourceAndConverter );
		}
	}
}
