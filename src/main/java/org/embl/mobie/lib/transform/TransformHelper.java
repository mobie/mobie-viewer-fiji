/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2024 EMBL
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
package org.embl.mobie.lib.transform;

import bdv.AbstractSpimSource;
import bdv.tools.transformation.TransformedSource;
import bdv.util.BdvHandle;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerPanel;
import ij.IJ;
import net.imglib2.Volatile;
import net.imglib2.realtransform.*;
import net.imglib2.roi.RealMaskRealInterval;
import net.imglib2.roi.geom.GeomMasks;
import net.imglib2.type.numeric.IntegerType;
import org.embl.mobie.lib.image.*;
import org.embl.mobie.lib.playground.BdvPlaygroundHelper;
import org.embl.mobie.lib.serialize.transformation.AffineTransformation;
import org.embl.mobie.lib.serialize.transformation.InterpolatedAffineTransformation;
import org.embl.mobie.lib.serialize.transformation.Transformation;
import org.embl.mobie.lib.source.Masked;
import org.embl.mobie.lib.source.RealTransformedSource;
import org.embl.mobie.lib.source.SourceHelper;
import net.imglib2.RealInterval;
import net.imglib2.util.Intervals;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
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


	public static double[] getCenter( Image< ? > image, int t )
	{
		final RealInterval bounds = image.getMask();
        return getCenter( bounds );
	}

	public static double[] getCenter( RealInterval bounds )
	{
		final double[] center = bounds.minAsDoubleArray();
		final double[] max = bounds.maxAsDoubleArray();
		for ( int d = 0; d < max.length; d++ )
		{
			center[ d ] = ( center[ d ] + max[ d ] ) / 2;
		}
		return center;
	}

	public static double[] getCenter( SourceAndConverter< ? > sourceAndConverter )
	{
		final RealInterval bounds = SourceHelper.getMask( sourceAndConverter.getSpimSource(), 0 );
		final double[] center = getCenter( bounds );
		return center;
	}

	public static AffineTransform3D createTranslationTransform(
			final Image< ? > image,
			final boolean centerAtOrigin,
			final double[] translation )
	{
		AffineTransform3D translationTransform = new AffineTransform3D();
		if ( centerAtOrigin )
		{
			final double[] center = getCenter( image, 0 );
			translationTransform.translate( center );
			translationTransform = translationTransform.inverse();
		}
		// System.out.println( "Image: " + image.getName() );
		// System.out.println( "Translation: " + translationX + ", " + translationY );
		translationTransform.translate( translation );
		return translationTransform;
	}

	public static AffineTransform3D createTranslationTransform( double translationX, double translationY, SourceAndConverter< ? > sourceAndConverter, boolean centerAtOrigin )
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

	@Deprecated
	public static double[] computeSourceUnionRealDimensions( List< SourceAndConverter< ? > > sources, double relativeMargin, int t )
	{
		RealInterval bounds = createMask( sources.stream().map( sac -> sac.getSpimSource() ).collect( Collectors.toList() ), t );
		final double[] realDimensions = new double[ 2 ];
		for ( int d = 0; d < 2; d++ )
			realDimensions[ d ] = ( 1.0 + 2.0 * relativeMargin ) * ( bounds.realMax( d ) - bounds.realMin( d ) );
		return realDimensions;
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

	public static AffineTransform3D getScatterPlotViewerTransform( BdvHandle bdv, double[] min, double[] max, double aspectRatio, boolean invertY, double zoom )
	{
		final AffineTransform3D affineTransform3D = new AffineTransform3D();

		double[] centerPosition = new double[ 3 ];
		for( int d = 0; d < 2; ++d )
		{
			final double center = ( min[ d ] + max[ d ] ) / 2.0;
			centerPosition[ d ] = - center;
		}
		affineTransform3D.translate( centerPosition );

		int[] bdvWindowDimensions = getWindowDimensions( bdv );

		final int windowMinSize = Math.min( bdvWindowDimensions[ 0 ], bdvWindowDimensions[ 1 ] );
		final double[] scales = new double[ 2 ];
		scales[ 0 ] = zoom * windowMinSize / (max[ 0 ] - min[ 0 ]);
		scales[ 1 ] = scales[ 0 ] / aspectRatio;

		scales[ 1 ] = invertY ? -scales[ 1 ] : scales[ 1 ];
		affineTransform3D.scale( scales[ 0 ], scales[ 1 ], 1.0 );

		double[] shiftToBdvWindowCenter = new double[ 3 ];
		for( int d = 0; d < 2; ++d )
			shiftToBdvWindowCenter[ d ] += bdvWindowDimensions[ d ] / 2.0;
		affineTransform3D.translate( shiftToBdvWindowCenter );

		return affineTransform3D;
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
			scale = Math.min( scale, 1.0 * ( bdvWindowDimensions[ d ] - 40 ) / size );
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

	// The evaluation of the resulting masks is slower than in
	// create createUnionBox, but it takes rotations into account.
	// FIXME: This currently does not really work, because in {@code TableSawAnnotatedRegion}
	//   the dilation of the mask will create a rectangular shape
	//   see "if ( relativeDilation > 0 )"
	public static RealMaskRealInterval union( Collection< ? extends Masked > maskedCollection )
	{
		if ( maskedCollection.isEmpty() )
			throw new RuntimeException("Cannot create union of empty list of masks.");

		RealMaskRealInterval union = null;

		for ( Masked masked : maskedCollection )
		{
			final RealMaskRealInterval mask = masked.getMask();

			if ( union == null )
			{
				union = mask;
			}
			else
			{
				if ( Intervals.contains( union, mask ) )
					continue;

				union = union.or( mask );
			}
		}

		return union;
	}

	public static RealMaskRealInterval unionBox( Collection< ? extends Masked > maskedCollection )
	{
		if ( maskedCollection.isEmpty() )
			throw new RuntimeException("Cannot create union of empty list of masks.");

		RealInterval union = null;

		for ( Masked masked : maskedCollection )
		{
			final RealMaskRealInterval mask = masked.getMask();

			if ( union == null )
			{
				union = mask;
			}
			else
			{
				if ( Intervals.contains( union, mask ) )
					continue;

				union = Intervals.union( mask, union );
			}
		}

		// convert to a box
		final double[] min = union.minAsDoubleArray();
		final double[] max = union.maxAsDoubleArray();
		return GeomMasks.closedBox( min, max );
	}

	@Nullable
	public static double[] getRealDimensions( RealMaskRealInterval unionMask )
	{
		final int numDimensions = unionMask.numDimensions();
		final double[] realDimensions = new double[ numDimensions ];
		final double[] min = unionMask.minAsDoubleArray();
		final double[] max = unionMask.maxAsDoubleArray();
		for ( int d = 0; d < numDimensions; d++ )
			realDimensions[ d ] = max[ d ] - min [ d ];
		return realDimensions;
	}

	public static String maskToString( RealMaskRealInterval mask )
	{
		return Arrays.toString( mask.minAsDoubleArray() ) + " - " + Arrays.toString( mask.maxAsDoubleArray() );
	}

	@NotNull
	public static AffineGet getEnlargementTransform( RealMaskRealInterval realMaskRealInterval, double scale )
	{
		int numDimensions = realMaskRealInterval.numDimensions();

		if ( numDimensions == 2 )
		{
			AffineTransform2D transform2D = new AffineTransform2D();
			double[] center = getCenter( realMaskRealInterval );
			transform2D.translate( Arrays.stream( center ).map( x -> -x ).toArray() );
			transform2D.scale( 1.0 / scale );
			transform2D.translate( center );
			return transform2D;
		}
		else if ( numDimensions == 3 )
		{
			AffineTransform3D transform3D = new AffineTransform3D();
			double[] center = getCenter( realMaskRealInterval );
			transform3D.translate( Arrays.stream( center ).map( x -> -x ).toArray() );
			transform3D.scale( 1.0 / scale );
			transform3D.translate( center );
			return transform3D;
		}
		else
		{
			throw new RuntimeException( "Unsupported number of dimensions " + numDimensions + ".");
		}
	}

	public static ArrayList< Transformation > fetchAllImageTransformations( Image< ? > image )
	{
		ArrayList< Transformation > transformations = new ArrayList<>();
		collectTransformations( image, transformations );
		Collections.reverse( transformations ); // first transformation first
		return transformations;
	}

	private static void collectTransformations( Image< ? > image, Collection< Transformation > transformations )
	{
		if ( image instanceof TransformedImage )
		{
			TransformedImage transformedImage = ( TransformedImage ) image;
			transformations.add( transformedImage.getTransformation() );
			collectTransformations( transformedImage.getWrappedImage(), transformations );
		}
		else if ( image instanceof AnnotationLabelImage )
		{
			Image< ? extends IntegerType< ? > > labelImage = ( ( AnnotationLabelImage< ? > ) image ).getLabelImage();
			collectTransformations( labelImage, transformations );
		}
		else
		{
			AffineTransform3D affineTransform3D = new AffineTransform3D();
			image.getSourcePair().getSource().getSourceTransform( 0, 0, affineTransform3D  );
			AffineTransformation affineTransformation = new AffineTransformation(
					"original image transform",
					affineTransform3D,
					Collections.singletonList( image.getName() ) );
			transformations.add( affineTransformation );
		}
	}


//	@Deprecated // use fetchAllTransformations( Image< ? > image ) instead
//	public static ArrayList< Transformation > fetchAllSourceTransformations( Source< ? > source )
//	{
//		ArrayList< Transformation > transformations = new ArrayList<>();
//		collectTransformations( source, transformations );
//		Collections.reverse( transformations ); // first transformation first
//		return transformations;
//	}

	private static void collectTransformations( Source< ? > source, Collection< Transformation > transformations )
	{
		if ( source instanceof AbstractSpimSource )
		{
			AffineTransform3D affineTransform3D = new AffineTransform3D();
			source.getSourceTransform( 0, 0, affineTransform3D );
			AffineTransformation affineTransformation = new AffineTransformation(
					"Input transformation", // FIXME: Those are not the names in the JSON
					affineTransform3D,
					Collections.singletonList( source.getName() ) );
			transformations.add( affineTransformation );
		}
		else if ( source instanceof TransformedSource )
		{
			TransformedSource< ? > transformedSource = ( TransformedSource< ? > ) source;
			final Source< ? > wrappedSource = transformedSource.getWrappedSource();
			AffineTransform3D fixedTransform = new AffineTransform3D();
			transformedSource.getFixedTransform( fixedTransform );
			// FIXME How to get the names?
			//  We could extend TransformedSource and add a field for the name of the transformation
			if ( ! fixedTransform.isIdentity() )
			{
				AffineTransformation affineTransformation = new AffineTransformation(
						"Additional transformation", // FIXME: Those are not the names in the JSON
						fixedTransform,
						Collections.singletonList( wrappedSource.getName() ) );
				transformations.add( affineTransformation );
			}
			collectTransformations( wrappedSource, transformations );
		}
		else if ( source instanceof RealTransformedSource )
		{
			RealTransformedSource< ? > realTransformedSource = ( RealTransformedSource< ? > ) source;
			RealTransform realTransform = realTransformedSource.getRealTransform();
			if ( realTransform instanceof InterpolatedAffineRealTransform )
			{
				Source< ? > wrappedSource = realTransformedSource.getWrappedSource();
				InterpolatedAffineRealTransform interpolatedAffineRealTransform = ( InterpolatedAffineRealTransform ) realTransform;
				InterpolatedAffineTransformation interpolatedAffineTransformation =
						new InterpolatedAffineTransformation(
								interpolatedAffineRealTransform.getName(),
								interpolatedAffineRealTransform.getTransforms(),
								wrappedSource.getName(),
								source.getName()
						);
				transformations.add( interpolatedAffineTransformation );
				collectTransformations( wrappedSource, transformations );
			}
			else
			{
				IJ.log( "Fetching transformations from " + source.getClass().getName() + " is not implemented." );
			}
		}
		else
		{
			IJ.log("Fetching transformations from " + source.getClass().getName() + " is not implemented.");
		}
	}

	public static ArrayList< Transformation > fetchAddedImageTransformations( Image< ? > image )
	{
		ArrayList< Transformation > allTransformations = fetchAllImageTransformations( image );
		allTransformations.remove( 0 ); // in MoBIE this is part of the raw image itself
		return allTransformations;
	}

//	@Deprecated
//	public static ArrayList< Transformation > fetchAddedSourceTransformations( Source< ? > source )
//	{
//		ArrayList< Transformation > allTransformations = fetchAllSourceTransformations( source );
//		if ( ! allTransformations.isEmpty() )
//			allTransformations.remove( 0 ); // in MoBIE this is part of the raw image itself
//		return allTransformations;
//	}

	// Wrap the input sourcePair into new TransformedSources,
	// because otherwise, if the incremental transformations of the input TransformedSources
	// are changed, e.g. by the current logic of how the ManualTransformEditor works,
	// this would create a mess.
	public static < T > SourcePair< T > getSourcePairWithNewTransformedSources( SourcePair< T > sourcePair )
	{
		TransformedSource< T > inputTransformedSource = ( TransformedSource< T > ) sourcePair.getSource();
		Source< T > inputSource = inputTransformedSource.getWrappedSource();
		TransformedSource< ? > wrappedTransformedSource = new TransformedSource<>( inputSource, inputSource.getName() );
		AffineTransform3D transform3D = new AffineTransform3D();
		inputTransformedSource.getFixedTransform( transform3D );
		wrappedTransformedSource.setFixedTransform( transform3D );
		Source< ? extends Volatile< T > > inputVolatileSource = ( ( TransformedSource< ? extends Volatile< T > > ) sourcePair.getVolatileSource() ).getWrappedSource();
		TransformedSource wrappedTransformedVolatileSource = new TransformedSource<>( inputVolatileSource, wrappedTransformedSource );
		return new DefaultSourcePair<>( wrappedTransformedSource, wrappedTransformedVolatileSource );
	}
}
