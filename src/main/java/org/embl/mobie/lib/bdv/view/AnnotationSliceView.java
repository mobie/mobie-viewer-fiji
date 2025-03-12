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
package org.embl.mobie.lib.bdv.view;

import bdv.tools.transformation.TransformedSource;
import bdv.util.BdvHandle;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.SynchronizedViewerState;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Volatile;
import net.imglib2.converter.Converter;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.roi.RealMaskRealInterval;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.util.LinAlgHelpers;
import org.embl.mobie.lib.data.DataStore;
import org.embl.mobie.MoBIE;
import org.embl.mobie.lib.annotation.AnnotatedRegion;
import org.embl.mobie.lib.annotation.Annotation;
import org.embl.mobie.lib.annotation.SliceViewAnnotationSelector;
import org.embl.mobie.lib.color.AnnotationARGBConverter;
import org.embl.mobie.lib.color.ColoringListener;
import org.embl.mobie.lib.color.VolatileAnnotationARGBConverter;
import org.embl.mobie.lib.image.*;
import org.embl.mobie.lib.image.Image;
import org.embl.mobie.lib.select.SelectionListener;
import org.embl.mobie.lib.serialize.display.AbstractAnnotationDisplay;
import org.embl.mobie.lib.serialize.display.RegionDisplay;
import org.embl.mobie.lib.serialize.display.SegmentationDisplay;
import org.embl.mobie.lib.serialize.display.SpotDisplay;
import org.embl.mobie.lib.source.AnnotationType;
import org.embl.mobie.lib.source.BoundarySource;
import org.embl.mobie.lib.source.SourceHelper;
import org.embl.mobie.lib.source.VolatileBoundarySource;
import org.embl.mobie.lib.transform.viewer.MoBIEViewerTransformAdjuster;
import org.embl.mobie.lib.transform.viewer.ViewerTransformChanger;
import org.embl.mobie.lib.util.MoBIEHelper;
import org.embl.mobie.lib.volume.SegmentVolumeViewer;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import javax.swing.*;
import java.awt.*;

public class AnnotationSliceView< A extends Annotation > extends AbstractSliceView implements ColoringListener, SelectionListener< A >
{
	protected final AbstractAnnotationDisplay< A > display;

	public AnnotationSliceView( MoBIE moBIE, AbstractAnnotationDisplay< A > display )
	{
		super( moBIE, display );
		this.display = display;
		display.selectionModel.listeners().add( this );
		display.coloringModel.listeners().add( this );

		for ( Image< AnnotationType< A > > image : display.images() )
		{
			SourceAndConverter< ? > sac = createSourceAndConverter( display, image );

			System.out.println("AnnotationSliceView: " + image.getName() + ": " + sac );

			configureRendering( sac );

			display.sliceViewer.show( image, sac, display );

			if ( display instanceof SegmentationDisplay )
			{
				final SegmentationDisplay segmentationDisplay = ( SegmentationDisplay ) display;
				SourceAndConverterServices.getSourceAndConverterService().setMetadata( sac, SegmentVolumeViewer.class.getName(), segmentationDisplay.segmentVolumeViewer );
			}

			if ( display instanceof SpotDisplay )
			{
				final SpotDisplay spotDisplay = ( SpotDisplay ) display;
				// TODO: Improve this by implementing an unwrapImage function
				//       similar to the existing unwrapSource function
				if ( image instanceof AffineTransformedImage )
				{
					image = ( Image ) ( ( ImageWrapper ) image ).getWrappedImage();
				}

				Image labelImage = ( ( AnnotatedLabelImage ) image ).getLabelImage();

				if ( labelImage instanceof AffineTransformedImage )
				{
					labelImage = ( Image ) ( ( ImageWrapper ) labelImage ).getWrappedImage();
				}

				( ( SpotLabelImage ) labelImage ).setSpotRadius( spotDisplay.spotRadius );
				( ( SpotLabelImage ) labelImage ).setSpotRadiusZ( spotDisplay.spotRadiusZ );


			}
		}
	}

	private SourceAndConverter createSourceAndConverter( AbstractAnnotationDisplay< A > display, Image< AnnotationType< A > > image )
	{
		// create non-volatile sac
		final Source< AnnotationType< A > > source = image.getSourcePair().getSource();
		final BoundarySource boundarySource = new BoundarySource( source, false, 0.0F, image.getMask() );
		final Converter< AnnotationType< A >, ARGBType > annotationARGBConverter = new AnnotationARGBConverter<>( display.coloringModel );
		final TransformedSource transformedBoundarySource = new TransformedSource( boundarySource );
		// FIXME: If those sources are transformed the underlying image will not know about it

		if ( image.getSourcePair().getVolatileSource() != null )
		{
			// create volatile sac
			final Source< ? extends Volatile< ? extends AnnotationType< ? > > > volatileSource = image.getSourcePair().getVolatileSource();
			final VolatileBoundarySource volatileBoundarySource = new VolatileBoundarySource( volatileSource, false, 1.0F, image.getMask() );
			final VolatileAnnotationARGBConverter volatileAnnotationConverter = new VolatileAnnotationARGBConverter( display.coloringModel );
			final TransformedSource volatileTransformedSource = new TransformedSource( volatileBoundarySource, transformedBoundarySource );
			SourceAndConverter volatileSourceAndConverter = new SourceAndConverter( volatileTransformedSource, volatileAnnotationConverter );

			// return combined non-volatile and volatile sac
			final SourceAndConverter combinedSAC = new SourceAndConverter( transformedBoundarySource, annotationARGBConverter, volatileSourceAndConverter );
			return combinedSAC;
		}

		// return non-volatile sac
		return new SourceAndConverter( transformedBoundarySource, annotationARGBConverter );
	}

	private void configureRendering( SourceAndConverter< ? > sourceAndConverter )
	{
		final boolean showAsBoundaries = display.showAsBoundaries();

		double boundaryThickness = display.getBoundaryThickness();

		if ( showAsBoundaries )
		{
			if ( display instanceof RegionDisplay )
			{
				final RegionDisplay< ? > display = ( RegionDisplay ) this.display;
				if ( display.boundaryThicknessIsRelative() )
				{
					final String someRegion = display.sources.keySet().iterator().next();
					final String someSource = display.sources.get( someRegion ).get( 0 );
					Image< ? > image = DataStore.getImage( someSource );
					final RealMaskRealInterval mask = image.getMask();
					// FIXME Do we really need the source here? Can't we just do it with the mask?
					RandomAccessibleInterval< ? > source = image.getSourcePair().getSource().getSource( 0, 0 );
					long[] dimensions = source.dimensionsAsLongArray();
					double[] realDimensions = new double[3];
					LinAlgHelpers.subtract( mask.maxAsDoubleArray(), mask.minAsDoubleArray(), realDimensions );
					double minimalExtent = Double.MAX_VALUE;
					for ( int d = 0; d < 3; d++ )
						if ( dimensions[d] > 1 )
							minimalExtent = Math.min( minimalExtent, realDimensions[d ] );
					boundaryThickness = minimalExtent * boundaryThickness;
				}
			}
		}

		final BoundarySource< ? > boundarySource = SourceHelper.unwrapSource( sourceAndConverter.getSpimSource(), BoundarySource.class );
		boundarySource.showAsBoundary( showAsBoundaries, boundaryThickness );
		if ( sourceAndConverter.asVolatile() != null )
		{
			final VolatileBoundarySource volatileBoundarySource = SourceHelper.unwrapSource( sourceAndConverter.asVolatile().getSpimSource(), VolatileBoundarySource.class );
			//System.out.println( "AnnotationSliceView: Boundary rendering for volatile " + sourceAndConverter.getSpimSource().getName() + "; showAsBoundaries = " + showAsBoundaries + "; boundaryThickness = " + boundaryThickness );
			volatileBoundarySource.showAsBoundary( showAsBoundaries, boundaryThickness );
		}
	}

	@Override
	public synchronized void selectionChanged()
	{
		getSliceViewer().getBdvHandle().getViewerPanel().requestRepaint();
	}

	@Override
	public void coloringChanged()
	{
		getSliceViewer().getBdvHandle().getViewerPanel().requestRepaint();
	}

	public synchronized void focusEvent( A selection, Object initiator )
	{
		if ( initiator instanceof SliceViewAnnotationSelector )
			return; // such that it is not focussed twice

		final BdvHandle bdvHandle = getSliceViewer().getBdvHandle();
		final SynchronizedViewerState state = bdvHandle.getViewerPanel().state();

		final Integer timepoint = selection.timePoint();
		if ( timepoint != null )
			state.setCurrentTimepoint( timepoint );

		AffineTransform3D viewerTransform;
		if ( selection instanceof AnnotatedRegion )
		{
			RealMaskRealInterval mask = ( ( AnnotatedRegion ) selection ).getMask();
			viewerTransform = MoBIEViewerTransformAdjuster.getViewerTransform( bdvHandle, mask );
		}
		else
		{
			final double[] position = selection.positionAsDoubleArray();
			if ( position == null ) return;
			viewerTransform = MoBIEHelper.getViewerTransformWithNewCenter( bdvHandle, position );

		}

		new sc.fiji.bdvpg.bdv.navigate.ViewerTransformChanger(
				bdvHandle,
				viewerTransform,
				false,
				ViewerTransformChanger.animationDurationMillis ).run();
	}

	public Window getWindow()
	{
		return SwingUtilities.getWindowAncestor( getSliceViewer().getBdvHandle().getViewerPanel() );
	}
}
