/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2022 EMBL
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package mobie3.viewer.transform;

import bdv.util.BdvHandle;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerPanel;
import mobie3.viewer.playground.BdvPlaygroundHelper;
import mobie3.viewer.playground.SourceAffineTransformer;
import mobie3.viewer.source.SourceHelper;
import net.imglib2.RealInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.Scale3D;
import net.imglib2.util.Intervals;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TransformHelper
{
	public static RealInterval createMask( List< ? extends Source< ? > > sources, int t )
	{
		RealInterval union = null;

		for ( Source< ? > source : sources )
		{
			final RealInterval bounds = SourceHelper.getMask( source, t );

			if ( union == null )
				union = bounds;
			else
				union = Intervals.union( bounds, union );
		}

		return union;
	}


	public static SourceAndConverter< ? > centerAtOrigin( SourceAndConverter< ? > sourceAndConverter )
	{
		final AffineTransform3D translate = new AffineTransform3D();
		final AffineTransform3D sourceTransform = new AffineTransform3D();
		sourceAndConverter.getSpimSource().getSourceTransform( 0,0, sourceTransform );
		final double[] center = getCenter( sourceAndConverter );
		translate.translate( center );
		final SourceAffineTransformer transformer = new SourceAffineTransformer( translate.inverse(), false );
		sourceAndConverter = transformer.apply( sourceAndConverter );
		return sourceAndConverter;
	}

	public static double[] getCenter( SourceAndConverter< ? > sourceAndConverter )
	{
		final RealInterval bounds = SourceHelper.getMask( sourceAndConverter.getSpimSource(), 0 );
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

	public static double[] computeSourceUnionRealDimensions( List< SourceAndConverter< ? > > sources, double relativeMargin, int t )
	{
		RealInterval bounds = createMask( sources.stream().map( sac -> sac.getSpimSource() ).collect( Collectors.toList() ), t );
		final double[] realDimensions = new double[ 2 ];
		for ( int d = 0; d < 2; d++ )
			realDimensions[ d ] = ( 1.0 + 2.0 * relativeMargin ) * ( bounds.realMax( d ) - bounds.realMin( d ) );
		return realDimensions;
	}

	public static double[] getMaximalSourceUnionRealDimensions( Map< String, SourceAndConverter< ? > > sourceNameToSourceAndConverter, Collection< List< String > > sourceNamesList )
	{
		double[] maximalDimensions = new double[ 2 ];
		for ( List< String > sourceNames : sourceNamesList )
		{
			final List< SourceAndConverter< ? > > sourceAndConverters = sourceNames.stream().map( name -> sourceNameToSourceAndConverter.get( name ) ).collect( Collectors.toList() );
			final double[] realDimensions = computeSourceUnionRealDimensions( sourceAndConverters, TransformedGridImageTransformer.RELATIVE_CELL_MARGIN, 0 );
			for ( int d = 0; d < 2; d++ )
				maximalDimensions[ d ] = realDimensions[ d ] > maximalDimensions[ d ] ? realDimensions[ d ] : maximalDimensions[ d ];
		}

		return maximalDimensions;
	}

	public static AffineTransform3D createNormalisedViewerTransform( ViewerPanel viewerPanel )
	{
		return createNormalisedViewerTransform( viewerPanel, BdvPlaygroundHelper.getWindowCentreInPixelUnits( viewerPanel ) );
	}

	public static AffineTransform3D createNormalisedViewerTransform( ViewerPanel viewerPanel, double[] position )
	{
		final AffineTransform3D view = new AffineTransform3D();
		viewerPanel.state().getViewerTransform( view );

		// translate position to upper left corner of the Window (0,0)
		final AffineTransform3D translate = new AffineTransform3D();
		translate.translate( position );
		view.preConcatenate( translate.inverse() );

		// divide by window width
		final int bdvWindowWidth = viewerPanel.getDisplay().getWidth();;
		final Scale3D scale = new Scale3D( 1.0 / bdvWindowWidth, 1.0 / bdvWindowWidth, 1.0 / bdvWindowWidth );
		view.preConcatenate( scale );

		return view;
	}

	public static AffineTransform3D createUnnormalizedViewerTransform( AffineTransform3D normalisedTransform, ViewerPanel viewerPanel )
	{
		final AffineTransform3D transform = normalisedTransform.copy();

		final int bdvWindowWidth = viewerPanel.getDisplay().getWidth();
		final Scale3D scale = new Scale3D( 1.0 / bdvWindowWidth, 1.0 / bdvWindowWidth, 1.0 / bdvWindowWidth );
		transform.preConcatenate( scale.inverse() );

		AffineTransform3D translate = new AffineTransform3D();
		translate.translate( BdvPlaygroundHelper.getWindowCentreInPixelUnits( viewerPanel ) );

		transform.preConcatenate( translate );

		return transform;
	}

	public static AffineTransform3D getIntervalViewerTransform( BdvHandle bdv, RealInterval interval  )
	{
		final AffineTransform3D affineTransform3D = new AffineTransform3D();

		double[] centerPosition = new double[ 3 ];
		for( int d = 0; d < 3; ++d )
		{
			final double center = ( interval.realMin( d ) + interval.realMax( d ) ) / 2.0;
			centerPosition[ d ] = - center;
		}
		affineTransform3D.translate( centerPosition );

		int[] bdvWindowDimensions = getWindowDimensions( bdv );

		double scale = Double.MAX_VALUE;
		for ( int d = 0; d < 2; d++ )
		{
			final double size = interval.realMax( d ) - interval.realMin( d );
			scale = Math.min( scale, 1.0 * bdvWindowDimensions[ d ] / size );
		}
		affineTransform3D.scale( scale );

		double[] shiftToBdvWindowCenter = new double[ 3 ];
		for( int d = 0; d < 2; ++d )
		{
			shiftToBdvWindowCenter[ d ] += bdvWindowDimensions[ d ] / 2.0;
		}
		affineTransform3D.translate( shiftToBdvWindowCenter );

		return affineTransform3D;
	}

	private static int[] getWindowDimensions( BdvHandle bdv )
	{
		int[] bdvWindowDimensions = new int[ 2 ];
		bdvWindowDimensions[ 0 ] = bdv.getBdvHandle().getViewerPanel().getWidth();
		bdvWindowDimensions[ 1 ] = bdv.getBdvHandle().getViewerPanel().getHeight();
		return bdvWindowDimensions;
	}

	public static AffineTransform3D asAffineTransform3D( double[] doubles )
	{
		final AffineTransform3D view = new AffineTransform3D( );
		view.set( doubles );
		return view;
	}

	public static String createNormalisedViewerTransformString( BdvHandle bdv, double[] position )
	{
		final AffineTransform3D view = createNormalisedViewerTransform( bdv.getViewerPanel(), position );
		final String replace = view.toString().replace( "3d-affine: (", "" ).replace( ")", "" );
		final String collect = Arrays.stream( replace.split( "," ) ).map( x -> "n" + x.trim() ).collect( Collectors.joining( "," ) );
		return collect;
	}
}
