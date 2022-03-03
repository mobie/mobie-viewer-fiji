package org.embl.mobie.viewer.transform;

import bdv.util.Affine3DHelpers;
import bdv.util.Bdv;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import ij.IJ;
import net.imglib2.util.LinAlgHelpers;
import org.embl.mobie.viewer.playground.SourceAffineTransformer;
import net.imglib2.FinalRealInterval;
import net.imglib2.RealInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Intervals;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TransformHelpers
{
	public static RealInterval estimateBounds( List< ? extends Source< ? > > sources, int t )
	{
		RealInterval union = estimateBounds( sources.get( 0 ), t );

		for ( int i = 1; i < sources.size(); i++ )
			union = Intervals.union( estimateBounds( sources.get( i ), t ), union );

		return union;
	}

	public static SourceAndConverter< ? > centerAtPhysicalOrigin( SourceAndConverter< ? > sourceAndConverter )
	{
		final double[] center = getPhysicalCenter( sourceAndConverter.getSpimSource() );
		final AffineTransform3D translate = new AffineTransform3D();
		translate.translate( center );
		final SourceAffineTransformer transformer = new SourceAffineTransformer( sourceAndConverter, translate.inverse() );
		return transformer.getSourceOut();
	}

	public static double[] getPhysicalCenter( Source< ? > spimSource )
	{
		final FinalRealInterval bounds = estimateBounds( spimSource, 0 );
		final double[] center = getCenter( bounds );
		return center;
	}

	public static double[] getCenter( FinalRealInterval realInterval )
	{
		final double[] center = realInterval.minAsDoubleArray();
		final double[] max = realInterval.maxAsDoubleArray();
		for ( int d = 0; d < max.length; d++ )
		{
			center[ d ] = ( center[ d ] + max[ d ] ) / 2;
		}
		return center;
	}

	public static AffineTransform3D createTranslationTransform3D( double translationX, double translationY, boolean centerAtOrigin, Source< ? > source )
	{
		AffineTransform3D translationTransform = new AffineTransform3D();
		if ( centerAtOrigin )
		{
			final double[] center = getPhysicalCenter( source );
			translationTransform.translate( center );
			translationTransform = translationTransform.inverse();
		}
		translationTransform.translate( translationX, translationY, 0 );
		return translationTransform;
	}

	public static double[] computeSourceUnionRealDimensions( List< SourceAndConverter< ? > > sources, double relativeMargin )
	{
		RealInterval bounds = estimateBounds( sources.stream().map( sac -> sac.getSpimSource() ).collect( Collectors.toList() ), 0 );
		final double[] realDimensions = new double[ 2 ];
		for ( int d = 0; d < 2; d++ )
			realDimensions[ d ] = ( 1.0 + 2.0 * relativeMargin ) * ( bounds.realMax( d ) - bounds.realMin( d ) );
		return realDimensions;
	}

	public static void focusInterval( Bdv bdv, RealInterval interval )
	{
		final AffineTransform3D affineTransform3D = getIntervalViewerTransorm( bdv, interval );

		bdv.getBdvHandle().getViewerPanel().state().setViewerTransform( affineTransform3D );
	}

	public static AffineTransform3D getIntervalViewerTransorm( Bdv bdv, RealInterval interval  )
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

	public static double[] getMaximalSourceUnionRealDimensions( Map< String, SourceAndConverter< ? > > sourceNameToSourceAndConverter, Collection< List< String > > sourceNamesList )
	{
		double[] maximalDimensions = new double[ 2 ];
		for ( List< String > sourceNames : sourceNamesList )
		{
			final List< SourceAndConverter< ? > > sourceAndConverters = sourceNames.stream().map( name -> sourceNameToSourceAndConverter.get( name ) ).collect( Collectors.toList() );
			final double[] realDimensions = computeSourceUnionRealDimensions( sourceAndConverters, TransformedGridSourceTransformer.RELATIVE_CELL_MARGIN );
			for ( int d = 0; d < 2; d++ )
				maximalDimensions[ d ] = realDimensions[ d ] > maximalDimensions[ d ] ? realDimensions[ d ] : maximalDimensions[ d ];
		}

		return maximalDimensions;
	}

	// Note that these bounds are aligned with the global coordinate system
	// which may not always make sense
	public static FinalRealInterval estimateBounds( Source< ? > source, int t )
	{
		final AffineTransform3D affineTransform3D = new AffineTransform3D();
		source.getSourceTransform( t, 0, affineTransform3D );
		final FinalRealInterval bounds = affineTransform3D.estimateBounds( source.getSource( t, 0 ) );
		return bounds;
	}

	/*
		Returns a transformation that reverses the rotation that may
		be included in the transform.
		The rotation is applied around the center of the given interval.
	 */
	public static AffineTransform3D getRectifyAffineTransform3D( RealInterval interval, AffineTransform3D transform )
	{
		final AffineTransform3D rectifyTransform = extractRectifyAffineTransform3D( transform );

		final double[] maskPhysicalCenter = getCenter( transform.estimateBounds( interval ) );

		final AffineTransform3D rotateAroundCenter = new AffineTransform3D();
		rotateAroundCenter.translate(  Arrays.stream( maskPhysicalCenter ).map( x -> -x ).toArray() );
		rotateAroundCenter.preConcatenate( rectifyTransform );
		rotateAroundCenter.translate( maskPhysicalCenter );
		return rotateAroundCenter;
	}

	public static AffineTransform3D extractRectifyAffineTransform3D( AffineTransform3D transform )
	{
		final double[] q = new double[ 4 ];
		Affine3DHelpers.extractRotationAnisotropic( transform.inverse(), q );
		final double[][] affine = new double[ 3 ][ 4 ];
		LinAlgHelpers.quaternionToR( q, affine );
		final AffineTransform3D rectifyTransform = new AffineTransform3D();
		rectifyTransform.set( affine );
		return rectifyTransform;
	}

	public static AffineTransform3D toAffineTransform3D( String affine )
	{
		if ( isValidAffine( affine ) ) {
			AffineTransform3D sourceTransform = new AffineTransform3D();
			// remove spaces
			affine = affine.replaceAll("\\s","");
			String[] splitAffineTransform = affine.split(",");
			double[] doubleAffineTransform = new double[splitAffineTransform.length];
			for (int i = 0; i < splitAffineTransform.length; i++) {
				doubleAffineTransform[i] = Double.parseDouble(splitAffineTransform[i]);
			}
			sourceTransform.set(doubleAffineTransform);
			return sourceTransform;
		} else {
			return null;
		}
	}

	public static boolean isValidAffine( String affine ) {
		if (!affine.matches("^[0-9., ]+$")) {
			IJ.log("Invalid affine transform - must contain only numbers, commas and spaces");
			return false;
		}

		String[] splitAffine = affine.split(",");
		if (splitAffine.length != 12) {
			IJ.log("Invalid affine transform - must be of length 12");
			return false;
		}

		return true;
	}
}
