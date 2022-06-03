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
package org.embl.mobie.viewer.bdv.view;

import bdv.viewer.SourceAndConverter;
import bdv.viewer.TimePointListener;
import de.embl.cba.tables.color.CategoryColoringModel;
import de.embl.cba.tables.color.ColoringListener;
import de.embl.cba.tables.color.ColoringModel;
import de.embl.cba.tables.tablerow.TableRow;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.display.AnnotationDisplay;
import org.embl.mobie.viewer.select.SelectionListener;
import org.embl.mobie.viewer.source.LabelSource;
import org.embl.mobie.viewer.source.SourceHelper;

import javax.swing.*;
import java.awt.*;

public abstract class AnnotationSliceView< T extends TableRow > extends AbstractSliceView implements ColoringListener, SelectionListener< T >
{
	protected final AnnotationDisplay< T > display;

	public AnnotationSliceView( MoBIE moBIE, AnnotationDisplay< T > display )
	{
		super( moBIE, display );
		this.display = display;
		display.selectionModel.listeners().add( this );
		display.selectionColoringModel.listeners().add( this );
	}

	protected void show( SourceAndConverter< ? > sourceAndConverter )
	{
		configureLabelRendering( sourceAndConverter );
		display.sliceViewer.show( sourceAndConverter, display );
		getSliceViewer().getBdvHandle().getViewerPanel().addTimePointListener( ( TimePointListener ) sourceAndConverter.getConverter() );
	}

	private void configureLabelRendering( SourceAndConverter< ? > sourceAndConverter )
	{
		final boolean showAsBoundaries = display.isShowAsBoundaries();
		final float boundaryThickness = display.getBoundaryThickness();
		final LabelSource< ? > labelSource = SourceHelper.getLabelSource( sourceAndConverter );
		labelSource.showAsBoundary( showAsBoundaries, boundaryThickness );
		if ( sourceAndConverter.asVolatile() != null )
		{
			final LabelSource< ? > vLabelSource = SourceHelper.getLabelSource( sourceAndConverter.asVolatile() );
			vLabelSource.showAsBoundary( showAsBoundaries, boundaryThickness );
		}

		final ColoringModel<T> coloringModel = display.selectionColoringModel.getWrappedColoringModel();
		if ( coloringModel instanceof CategoryColoringModel )
			( ( CategoryColoringModel<?> ) coloringModel ).setRandomSeed( display.getRandomColorSeed() );
	}

	@Override
	public synchronized void coloringChanged()
	{
		getSliceViewer().getBdvHandle().getViewerPanel().requestRepaint();
	}

	@Override
	public synchronized void selectionChanged()
	{
		getSliceViewer().getBdvHandle().getViewerPanel().requestRepaint();
	}

	@Override
	public synchronized void focusEvent( T selection, Object initiator )
	{
		// define in child classes
	}

	public Window getWindow()
	{
		return SwingUtilities.getWindowAncestor( getSliceViewer().getBdvHandle().getViewerPanel() );
	}
}
