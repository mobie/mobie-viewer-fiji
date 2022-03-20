package org.embl.mobie.viewer.bdv;

import bdv.util.BdvHandle;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import de.embl.cba.bdv.utils.BdvUtils;
import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.converter.Converters;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.RandomAccessibleOnRealRandomAccessible;
import net.imglib2.view.Views;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;

import java.util.ArrayList;
import java.util.List;

import static de.embl.cba.bdv.utils.BdvUtils.getLevel;
import static sc.fiji.bdvpg.bdv.BdvHandleHelper.getViewerVoxelSpacing;

public class SourceViewRasterizer
{
	private final BdvHandle bdvHandle;
	private final List< Source< ? > > sources;

	private int currentTimepoint;
	private double rasterVoxelSize;
	private ArrayList< RandomAccessibleInterval< FloatType > > rasterRais;
	private long[] rasterDimensions;
	private AffineTransform3D rasterTransform;

	public SourceViewRasterizer( BdvHandle bdvHandle, List< Source< ? > > sources )
	{
		this.bdvHandle = bdvHandle;
		this.sources = sources;
		currentTimepoint = bdvHandle.getViewerPanel().state().getCurrentTimepoint();
	}

	public List< RandomAccessibleInterval< FloatType > > getRasterRais()
	{
		if ( rasterRais == null )
			raster();

		return rasterRais;
	}
	
	public double getRasterVoxelSize()
	{
		return rasterVoxelSize;
	}

	public AffineTransform3D getRasterTransform()
	{
		return rasterTransform;
	}

	private void raster()
	{
		rasterTransform = bdvHandle.getViewerPanel().state().getViewerTransform();
		rasterVoxelSize = determineRasterVoxelSize();
		rasterDimensions = ScreenShotMaker.captureImageSizeInPixels( bdvHandle, rasterVoxelSize );
		rasterRais = new ArrayList<>();

		for ( Source< ? > source : sources )
		{
			final RandomAccessibleInterval< FloatType > rai = raster( ( Source< ? extends RealType< ? > > ) source );
			rasterRais.add( rai );
		}
	}

	private RandomAccessibleInterval< FloatType > raster( Source< ? extends RealType< ? > > source )
	{
		final int level = getLevel( source, rasterVoxelSize );

		AffineTransform3D rasterToSourceTransform = getAffineTransform3D( source, level );

		final RandomAccessibleInterval< FloatType > rai = rasterViaView( source, level, rasterToSourceTransform );

		//final RandomAccessibleInterval< FloatType > rai = rasterViaCursor( rasterToSourceTransform, source, level );

		return rai;
	}

	private RandomAccessibleInterval< FloatType > rasterViaView( Source< ? extends RealType<?> > source, int level, AffineTransform3D rasterToSourceTransform )
	{
		final RandomAccessibleOnRealRandomAccessible< ? extends RealType< ? > > raster = Views.raster( RealViews.affine( ( source.getInterpolatedSource( currentTimepoint, level, Interpolation.NLINEAR ) ), rasterToSourceTransform.inverse() ) );

		final RandomAccessibleInterval< ? extends RealType< ? > > interval = Views.interval(
				raster,
				new FinalInterval( bdvHandle.getViewerPanel().getWidth(), bdvHandle.getViewerPanel().getHeight(), 1 ) );

		ImageJFunctions.show( (RandomAccessibleInterval) interval, "aaa" );

		final RandomAccessibleInterval< FloatType > convert = Converters.convert( interval, ( i, o ) -> o.setReal( i.getRealFloat() ), new FloatType() );

		return Views.dropSingletonDimensions( convert );
	}

	private AffineTransform3D getAffineTransform3D( Source< ? > source, int level )
	{
		AffineTransform3D sourceTransform = new AffineTransform3D();
		source.getSourceTransform( currentTimepoint, level, sourceTransform );
		AffineTransform3D viewerToSourceTransform = new AffineTransform3D();
		viewerToSourceTransform.preConcatenate( rasterTransform.inverse() );
		viewerToSourceTransform.preConcatenate( sourceTransform.inverse() );
		return viewerToSourceTransform;
	}

	private RandomAccessibleInterval< FloatType > rasterViaCursor( AffineTransform3D viewerToSourceTransform, Source< ? > source, int level )
	{
		final RandomAccessibleInterval< FloatType > rai = ArrayImgs.floats( rasterDimensions[ 0 ], rasterDimensions[ 1 ] );

		final double canvasStepSize = rasterVoxelSize / getViewerVoxelSpacing( bdvHandle );

		RealRandomAccess< ? extends RealType< ? > > realTypeAccess = (RealRandomAccess<? extends RealType<?>>) source.getInterpolatedSource( currentTimepoint, level, Interpolation.NLINEAR).realRandomAccess();


		final Cursor< FloatType > cursor = Views.iterable( rai ).localizingCursor();
		final RandomAccess< FloatType > access = rai.randomAccess();

		final double[] canvasPosition = new double[ 3 ];
		final double[] sourcePixelPosition = new double[ 3 ];

		// iterate through the target image in pixel units
		while ( cursor.hasNext() )
		{
			cursor.fwd();
			cursor.localize( canvasPosition );
			access.setPosition( cursor );

			// canvasPosition is the position on the canvas
			// in physical units
			// dxy is the step size that is needed to get
			// the desired resolution in the output image
			canvasPosition[ 0 ] *= canvasStepSize;
			canvasPosition[ 1 ] *= canvasStepSize;

			viewerToSourceTransform.apply( canvasPosition, sourcePixelPosition );
			access.get().setReal( realTypeAccess.setPositionAndGet( sourcePixelPosition ).getRealFloat() );
		}

		return rai;
	}

	private double determineRasterVoxelSize()
	{
		double rasterVoxelSize = BdvUtils.getViewerVoxelSpacing( bdvHandle );
		for ( Source< ? > source : sources )
		{
			final int level = getLevel( source, rasterVoxelSize );
			final double sourceVoxelSize = SourceAndConverterHelper.getCharacteristicVoxelSize( source, currentTimepoint, level );
			rasterVoxelSize = Math.max( rasterVoxelSize, sourceVoxelSize );
		}
		return BdvUtils.getViewerVoxelSpacing( bdvHandle );
	}
}
