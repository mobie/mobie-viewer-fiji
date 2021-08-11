package de.embl.cba.mobie.transform;

import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import de.embl.cba.mobie.Utils;
import de.embl.cba.mobie.playground.SourceAffineTransformer;
import net.imglib2.FinalRealInterval;
import net.imglib2.RealInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.util.Intervals;

import java.util.List;

public class TransformHelper
{
	public static List< SourceAndConverter< ? > > transformSourceAndConverters( List< SourceAndConverter< ? > > sourceAndConverters, List< SourceTransformer > sourceTransformers )
	{
		if ( sourceTransformers != null )
		{
			for ( SourceTransformer sourceTransformer : sourceTransformers )
			{
				sourceAndConverters = sourceTransformer.transform( sourceAndConverters );
				//break;
			}
		}

		return sourceAndConverters;
	}

	public static RealInterval unionRealInterval( List< ? extends Source< ? > > sources )
	{
		RealInterval union = null;

		for ( Source< ? > source : sources )
		{
			final FinalRealInterval bounds = Utils.estimateBounds( source );

			if ( union == null )
			{
				union = bounds;
			}
			else
			{
				union = Intervals.union( bounds, union );
			}
		}

		return union;
	}

	public static < T extends NumericType< T > > SourceAndConverter< T > centerAtOrigin( SourceAndConverter< T > sourceAndConverter )
	{
		final AffineTransform3D translate = new AffineTransform3D();
		final AffineTransform3D sourceTransform = new AffineTransform3D();
		sourceAndConverter.getSpimSource().getSourceTransform( 0,0, sourceTransform );
		final double[] center = getCenter( sourceAndConverter );
		translate.translate( center );
		final SourceAffineTransformer transformer = new SourceAffineTransformer( translate.inverse() );
		sourceAndConverter = transformer.apply( sourceAndConverter );
		return sourceAndConverter;
	}

	public static double[] getCenter( SourceAndConverter< ? > sourceAndConverter )
	{
		final FinalRealInterval bounds = Utils.estimateBounds( sourceAndConverter.getSpimSource() );
		final double[] center = bounds.minAsDoubleArray();
		final double[] max = bounds.maxAsDoubleArray();
		for ( int d = 0; d < max.length; d++ )
		{
			center[ d ] = ( center[ d ] + max[ d ] ) / 2;
		}
		return center;
	}

	public static AffineTransform3D createTranslationTransform3D( double translationX, double translationY, SourceAndConverter< ? > sourceAndConverter, boolean centerAtOrigin )
	{
		AffineTransform3D translationTransform = new AffineTransform3D();
		if ( centerAtOrigin )
		{
			final double[] center = getCenter( sourceAndConverter );
			translationTransform.translate( center );
			translationTransform = translationTransform.inverse();
		}
		translationTransform.translate( translationX, translationY, 0 );
		return translationTransform;
	}
}
