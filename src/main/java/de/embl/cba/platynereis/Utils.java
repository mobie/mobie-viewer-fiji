package de.embl.cba.platynereis;

import bdv.BigDataViewer;
import bdv.util.Bdv;
import bdv.util.BdvHandle;
import net.imglib2.FinalInterval;
import net.imglib2.FinalRealInterval;
import net.imglib2.RealInterval;
import net.imglib2.realtransform.AffineTransform3D;

public class Utils
{

	public static double[] delimitedStringToDoubleArray(String s, String delimiter) {

		String[] sA = s.split(delimiter);
		double[] nums = new double[sA.length];
		for (int i = 0; i < nums.length; i++) {
			nums[i] = Double.parseDouble(sA[i].trim());
		}

		return nums;
	}


	public static void zoomToInterval( FinalRealInterval interval, Bdv bdv )
	{
		final AffineTransform3D affineTransform3D = getImageZoomTransform( interval, bdv );

		bdv.getBdvHandle().getViewerPanel().setCurrentViewerTransform( affineTransform3D );
	}

	public static AffineTransform3D getImageZoomTransform( FinalRealInterval interval, Bdv bdv )
	{

		final AffineTransform3D affineTransform3D = new AffineTransform3D();

		double[] centerPosition = new double[ 3 ];

		for( int d = 0; d < 3; ++d )
		{
			final double center = ( interval.realMin( d ) + interval.realMax( d ) ) / 2.0;
			centerPosition[ d ] = - center;
		}

		affineTransform3D.translate( centerPosition );

		int[] bdvWindowDimensions = new int[ 2 ];
		bdvWindowDimensions[ 0 ] = bdv.getBdvHandle().getViewerPanel().getWidth();
		bdvWindowDimensions[ 1 ] = bdv.getBdvHandle().getViewerPanel().getHeight();

		final double intervalSize = interval.realMax( 0 ) - interval.realMin( 0 );
		affineTransform3D.scale(  1.0 * bdvWindowDimensions[ 0 ] / intervalSize );

		double[] shiftToBdvWindowCenter = new double[ 3 ];

		for( int d = 0; d < 2; ++d )
		{
			shiftToBdvWindowCenter[ d ] += bdvWindowDimensions[ d ] / 2.0;
		}

		affineTransform3D.translate( shiftToBdvWindowCenter );

		return affineTransform3D;
	}

	public static void centerBdvViewToPosition( double[] position, Bdv bdv )
	{
		final AffineTransform3D viewerTransform = new AffineTransform3D();

		bdv.getBdvHandle().getViewerPanel().getState().getViewerTransform( viewerTransform );

		double[] centerPosition = new double[ 3 ];

//		for( int d = 0; d < 3; ++d )
//		{
//			final double center = ( interval.realMin( d ) + interval.realMax( d ) ) / 2.0;
//			centerPosition[ d ] = - center;
//		}

//		int[] bdvWindowDimensions = new int[ 2 ];
//		bdvWindowDimensions[ 0 ] = bdv.getBdvHandle().getViewerPanel().getWidth();
//		bdvWindowDimensions[ 1 ] = bdv.getBdvHandle().getViewerPanel().getHeight();

		final double[] translation = viewerTransform.getTranslation();

		viewerTransform.setTranslation( position );

		bdv.getBdvHandle().getViewerPanel().setCurrentViewerTransform( viewerTransform );

	}
}
