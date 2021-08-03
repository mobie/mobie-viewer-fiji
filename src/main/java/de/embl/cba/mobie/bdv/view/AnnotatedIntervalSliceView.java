package de.embl.cba.mobie.bdv.view;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.TimePointListener;
import de.embl.cba.mobie.MoBIE;
import de.embl.cba.mobie.annotate.AnnotatedInterval;
import de.embl.cba.mobie.annotate.AnnotatedIntervalTableRow;
import de.embl.cba.mobie.annotate.TableRowsIntervalImage;
import de.embl.cba.mobie.color.ListItemsARGBConverter;
import de.embl.cba.mobie.color.OpacityAdjuster;
import de.embl.cba.mobie.display.AnnotatedIntervalDisplay;
import de.embl.cba.mobie.transform.PositionViewerTransform;
import de.embl.cba.mobie.transform.MoBIEViewerTransformChanger;
import de.embl.cba.tables.color.ColoringListener;
import de.embl.cba.tables.select.SelectionListener;
import net.imglib2.type.numeric.integer.IntType;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterBdvDisplayService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;


// TODO: code duplication with SegmentationSourceDisplay => derive from a parent class
public class AnnotatedIntervalSliceView< S extends AnnotatedInterval > implements ColoringListener, SelectionListener< S >
{
	private final SourceAndConverterBdvDisplayService displayService;
	private final MoBIE moBIE;
	private final AnnotatedIntervalDisplay display;
	private BdvHandle bdvHandle;

	public AnnotatedIntervalSliceView( MoBIE moBIE, AnnotatedIntervalDisplay display, BdvHandle bdvHandle  )
	{
		this.moBIE = moBIE;
		this.display = display;
		this.bdvHandle = bdvHandle;
		this.displayService = SourceAndConverterServices.getBdvDisplayService();
		show();
	}

	private void show( )
	{
		display.selectionModel.listeners().add( this );
		display.coloringModel.listeners().add( this );

		// TODO: Make a SourceAnnotationSliceView with the listeners for the focussing.
		final TableRowsIntervalImage< AnnotatedIntervalTableRow > intervalImage = new TableRowsIntervalImage<>( display.tableRows, display.coloringModel, display.getName() );
		SourceAndConverter< IntType > sourceAndConverter = intervalImage.getSourceAndConverter();
		display.sourceAndConverters = new ArrayList<>();
		display.sourceAndConverters.add( sourceAndConverter );

		// set opacity
		OpacityAdjuster.adjustOpacity( sourceAndConverter, display.getOpacity() );

		// show
		final boolean visible = display.isVisible();
		displayService.show( bdvHandle, visible, sourceAndConverter );

		// add listener
		ListItemsARGBConverter converter = ( ListItemsARGBConverter ) sourceAndConverter.getConverter();
		bdvHandle.getViewerPanel().addTimePointListener(  converter );
	}

	public void close()
	{
		for ( SourceAndConverter< ? > sourceAndConverter : display.sourceAndConverters )
		{
			SourceAndConverterServices.getBdvDisplayService().removeFromAllBdvs( sourceAndConverter );
		}

		for ( SourceAndConverter< ? > sourceAndConverter : display.sourceAndConverters )
		{
			moBIE.closeSourceAndConverter( sourceAndConverter );
		}
		display.sourceAndConverters.clear();
	};

	@Override
	public synchronized void coloringChanged()
	{
		bdvHandle.getViewerPanel().requestRepaint();
	}

	@Override
	public synchronized void selectionChanged()
	{
		bdvHandle.getViewerPanel().requestRepaint();
	}

	@Override
	public synchronized void focusEvent( S selection )
	{
		if ( selection.getTimepoint() != getBdvHandle().getViewerPanel().state().getCurrentTimepoint() )
		{
			getBdvHandle().getViewerPanel().state().setCurrentTimepoint( selection.getTimepoint() );
		}

		final double[] max = selection.getInterval().maxAsDoubleArray();
		final double[] min = selection.getInterval().minAsDoubleArray();
		final double[] center = new double[ min.length ];
		for ( int d = 0; d < 3; d++ )
		{
			center[ d ] = ( max[ d ] + min[ d ] ) / 2;
		}

		MoBIEViewerTransformChanger.changeViewerTransform( bdvHandle, new PositionViewerTransform( center, bdvHandle.getViewerPanel().state().getCurrentTimepoint() ) );

	}

	public BdvHandle getBdvHandle()
	{
		return bdvHandle;
	}

	public Window getWindow()
	{
		return SwingUtilities.getWindowAncestor( bdvHandle.getViewerPanel() );
	}
}
