package org.embl.mobie.viewer.bdv.view;

import bdv.viewer.SourceAndConverter;
import bdv.viewer.TimePointListener;
import de.embl.cba.tables.color.CategoryColoringModel;
import de.embl.cba.tables.color.ColoringListener;
import de.embl.cba.tables.color.ColoringModel;
import de.embl.cba.tables.color.ColumnColoringModel;
import de.embl.cba.tables.color.NumericColoringModel;
import de.embl.cba.tables.tablerow.TableRow;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.display.AnnotationDisplay;
import org.embl.mobie.viewer.select.SelectionListener;
import org.embl.mobie.viewer.source.LabelSource;

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
		display.coloringModel.listeners().add( this );
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
		( (LabelSource) sourceAndConverter.getSpimSource() ).showAsBoundary( showAsBoundaries, boundaryThickness );
		if ( sourceAndConverter.asVolatile() != null )
			( (LabelSource) sourceAndConverter.asVolatile().getSpimSource() ).showAsBoundary( showAsBoundaries, boundaryThickness );

		final ColoringModel<T> coloringModel = display.coloringModel.getWrappedColoringModel();
		if ( coloringModel instanceof CategoryColoringModel )
			( ( CategoryColoringModel<?> ) coloringModel ).setRandomSeed( display.getRandomColorSeed() );
	}

	public void close( boolean closeImgLoader )
	{
		for ( SourceAndConverter< ? > sourceAndConverter : display.sourceNameToSourceAndConverter.values() )
		{
			moBIE.closeSourceAndConverter( sourceAndConverter, closeImgLoader );
		}
		display.sourceNameToSourceAndConverter.clear();
	};

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
