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
package mobie3.viewer.bdv.view;

import bdv.util.BdvHandle;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.SynchronizedViewerState;
import de.embl.cba.tables.color.CategoryColoringModel;
import de.embl.cba.tables.color.ColoringListener;
import de.embl.cba.tables.color.ColoringModel;
import mobie3.viewer.MoBIE;
import mobie3.viewer.annotation.SliceViewAnnotationSelector;
import mobie3.viewer.color.AnnotationConverter;
import mobie3.viewer.color.VolatileAnnotationConverter;
import mobie3.viewer.display.AnnotationDisplay;
import mobie3.viewer.select.SelectionListener;
import mobie3.viewer.source.AnnotatedImage;
import mobie3.viewer.source.AnnotatedLabelMask;
import mobie3.viewer.source.AnnotationType;
import mobie3.viewer.source.BoundarySource;
import mobie3.viewer.source.SourceHelper;
import mobie3.viewer.source.VolatileBoundarySource;
import mobie3.viewer.annotation.Annotation;
import mobie3.viewer.transform.SliceViewLocationChanger;
import net.imglib2.Volatile;
import sc.fiji.bdvpg.bdv.BdvHandleHelper;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformChanger;

import javax.swing.*;
import java.awt.*;

public class AnnotationSliceView< A extends Annotation > extends AbstractSliceView implements ColoringListener, SelectionListener< A >
{
	protected final AnnotationDisplay< A > display;

	public AnnotationSliceView( MoBIE moBIE, AnnotationDisplay< A > display )
	{
		super( moBIE, display );
		this.display = display;
		display.selectionModel.listeners().add( this );
		display.coloringModel.listeners().add( this );

		for ( AnnotatedImage< A > image : display.images )
		{
			// create volatile sac
			//
			final Source< ? extends Volatile< ? extends AnnotationType< ? > > > volatileSource = image.getSourcePair().getVolatileSource();
			final VolatileBoundarySource volatileBoundarySource = new VolatileBoundarySource( volatileSource );
			final VolatileAnnotationConverter volatileAnnotationConverter = new VolatileAnnotationConverter( display.coloringModel );
			SourceAndConverter volatileSourceAndConverter = new SourceAndConverter( volatileBoundarySource, volatileAnnotationConverter );

			// create non-volatile sac
			//
			final Source< ? extends  AnnotationType< ? > > source = image.getSourcePair().getSource();
			final BoundarySource boundarySource = new BoundarySource( source );
			final AnnotationConverter< ? > annotationConverter = new AnnotationConverter<>( display.coloringModel );

			// combine volatile and non-volatile sac
			final SourceAndConverter sourceAndConverter = new SourceAndConverter( boundarySource, annotationConverter, volatileSourceAndConverter );

			show( sourceAndConverter );
		}
	}

	private void show( SourceAndConverter< ? > sourceAndConverter )
	{
		configureAnnotationRendering( sourceAndConverter );
		display.sliceViewer.show( sourceAndConverter, display );
	}

	private void configureAnnotationRendering( SourceAndConverter< ? > sourceAndConverter )
	{
		final boolean showAsBoundaries = display.isShowAsBoundaries();
		final float boundaryThickness = display.getBoundaryThickness();
		final BoundarySource boundarySource = SourceHelper.unwrapSource( sourceAndConverter.getSpimSource(), BoundarySource.class );
		boundarySource.showAsBoundary( showAsBoundaries, boundaryThickness );
		if ( sourceAndConverter.asVolatile() != null )
		{
			final VolatileBoundarySource volatileBoundarySource = SourceHelper.unwrapSource( sourceAndConverter.asVolatile().getSpimSource(), VolatileBoundarySource.class );
			volatileBoundarySource.showAsBoundary( showAsBoundaries, boundaryThickness );
		}
		final ColoringModel< A > coloringModel = display.coloringModel.getWrappedColoringModel();
		if ( coloringModel instanceof CategoryColoringModel )
			( ( CategoryColoringModel<?> ) coloringModel ).setRandomSeed( display.getRandomColorSeed() );
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
			return;

		final BdvHandle bdvHandle = getSliceViewer().getBdvHandle();
		final SynchronizedViewerState state = bdvHandle.getViewerPanel().state();
		state.setCurrentTimepoint( selection.timePoint() );
		final double[] position = selection.anchor();

		new ViewerTransformChanger(
				bdvHandle,
				BdvHandleHelper.getViewerTransformWithNewCenter( bdvHandle, position ),
				false,
				SliceViewLocationChanger.animationDurationMillis ).run();
	}

	public Window getWindow()
	{
		return SwingUtilities.getWindowAncestor( getSliceViewer().getBdvHandle().getViewerPanel() );
	}
}
