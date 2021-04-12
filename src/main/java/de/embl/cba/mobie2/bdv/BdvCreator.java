package de.embl.cba.mobie2.bdv;

import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import bdv.viewer.Interpolation;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.ByteArray;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.integer.ByteType;
import sc.fiji.bdvpg.bdv.projector.AccumulateAverageProjectorARGB;
import sc.fiji.bdvpg.bdv.projector.AccumulateMixedProjectorARGBFactory;
import sc.fiji.bdvpg.bdv.projector.Projector;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterBdvDisplayService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import java.util.function.Supplier;

@Deprecated
public class BdvCreator implements Supplier< BdvHandle  >
{
	private String windowTitle;
	private boolean is2D;
	private String projector;
	private boolean interpolate;
	private int numTimePoints;

	public BdvCreator( String windowTitle, boolean is2D, String projector, boolean interpolate, int numTimePoints )
	{
		this.windowTitle = windowTitle;
		this.is2D = is2D;
		this.projector = projector;
		this.interpolate = interpolate;
		this.numTimePoints = numTimePoints;
	}

	@Override
	public BdvHandle get()
	{
		BdvOptions bdvOptions = createBdvOptions();
		BdvHandle bdvHandle = createBdv( bdvOptions, interpolate, numTimePoints );
		registerAtBdvDisplayService( bdvHandle );
		return bdvHandle;
	}

	private void registerAtBdvDisplayService( BdvHandle bdvHandle )
	{
		registerProjectionMode( bdvHandle );
	}

	private void registerProjectionMode( BdvHandle bdvHandle )
	{
		final SourceAndConverterBdvDisplayService displayService = SourceAndConverterServices.getSourceAndConverterDisplayService();

		switch (projector) {
			case Projector.MIXED_PROJECTOR:
				displayService.setDisplayMetadata( bdvHandle, Projector.PROJECTOR, Projector.MIXED_PROJECTOR );
				break;
			case Projector.SUM_PROJECTOR:
				displayService.setDisplayMetadata( bdvHandle, Projector.PROJECTOR, Projector.SUM_PROJECTOR );
				break;
			case Projector.AVERAGE_PROJECTOR:
				displayService.setDisplayMetadata( bdvHandle, Projector.PROJECTOR, Projector.AVERAGE_PROJECTOR );
				break;
			default:
		}
	}

	private BdvOptions createBdvOptions()
	{
		BdvOptions bdvOptions = BdvOptions.options().frameTitle( windowTitle );
		if ( is2D ) bdvOptions = bdvOptions.is2D();
		bdvOptions = configureProjectorFactory( bdvOptions, projector );
		return bdvOptions;
	}

	private static BdvHandle createBdv( BdvOptions bdvOptions, boolean interpolate, int numTimePoints )
	{
		// create a dummy image to instantiate the BDV
		ArrayImg< ByteType, ByteArray > dummyImg = ArrayImgs.bytes(2, 2, 2);
		bdvOptions = bdvOptions.sourceTransform( new AffineTransform3D() );
		BdvStackSource<ByteType> bss = BdvFunctions.show( dummyImg, "dummy", bdvOptions );
		BdvHandle bdv = bss.getBdvHandle();

		if ( interpolate ) bdv.getViewerPanel().setInterpolation( Interpolation.NLINEAR );
		// remove dummy image
		bdv.getViewerPanel().state().removeSource(bdv.getViewerPanel().state().getCurrentSource());

		bdv.getViewerPanel().setNumTimepoints( numTimePoints );

		return bdv;
	}

	private static BdvOptions configureProjectorFactory( BdvOptions opts, String projector )
	{
		switch ( projector ) {
			case Projector.MIXED_PROJECTOR:
				return opts.accumulateProjectorFactory( new AccumulateMixedProjectorARGBFactory(  ) );
			case Projector.SUM_PROJECTOR:
				return opts; // default
			case Projector.AVERAGE_PROJECTOR:
				return opts.accumulateProjectorFactory( AccumulateAverageProjectorARGB.factory );
			default:
				return opts;
		}
	}
}
