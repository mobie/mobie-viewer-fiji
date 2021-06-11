package de.embl.cba.mobie.grid;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import de.embl.cba.bdv.utils.Logger;
import de.embl.cba.mobie.transform.BdvLocation;
import de.embl.cba.mobie.transform.BdvLocationType;
import de.embl.cba.mobie.Utils;
import de.embl.cba.mobie.MoBIE;
import de.embl.cba.mobie.transform.BdvLocationChanger;
import de.embl.cba.mobie.color.MoBIEColoringModel;
import de.embl.cba.mobie.display.SourceDisplay;
import de.embl.cba.mobie.transform.GridSourceTransformer;
import de.embl.cba.mobie.table.TableViewer;
import de.embl.cba.tables.TableColumns;
import de.embl.cba.tables.color.ColoringListener;
import de.embl.cba.tables.color.ColoringLuts;
import de.embl.cba.tables.select.DefaultSelectionModel;
import de.embl.cba.tables.select.SelectionListener;
import net.imglib2.type.numeric.integer.IntType;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GridOverlaySourceDisplay extends SourceDisplay implements ColoringListener, SelectionListener< DefaultAnnotatedIntervalTableRow >
{
	private final MoBIEColoringModel< DefaultAnnotatedIntervalTableRow > coloringModel;
	private final DefaultSelectionModel< DefaultAnnotatedIntervalTableRow > selectionModel;
	private final TableViewer< DefaultAnnotatedIntervalTableRow > tableViewer;
	private final BdvHandle bdvHandle;
	private final String name;

	// TODO: split in two classes: the GridOverlayDisplay and the GridOverlayView
	public GridOverlaySourceDisplay( MoBIE moBIE2, BdvHandle bdvHandle, String name, String tableDataFolder, GridSourceTransformer sourceTransformer )
	{
		this.bdvHandle = bdvHandle;
		this.name = name;

		// TODO: Deal with more than one table
		String tablePath = moBIE2.getTablePath( tableDataFolder, sourceTransformer.tables[ 0 ] );
		tablePath = Utils.resolveTablePath( tablePath );
		Logger.log( "Opening table:\n" + tablePath );

		Map< String, List< String > > columns = TableColumns.stringColumnsFromTableFile( tablePath );
		final AnnotatedIntervalCreator annotatedIntervalCreator = new AnnotatedIntervalCreator( columns, sourceTransformer );
		final List< DefaultAnnotatedIntervalTableRow > tableRows = annotatedIntervalCreator.getTableRows();

		coloringModel = new MoBIEColoringModel< DefaultAnnotatedIntervalTableRow >( ColoringLuts.GLASBEY );
		selectionModel = new DefaultSelectionModel< DefaultAnnotatedIntervalTableRow >();
		coloringModel.setSelectionModel( selectionModel );

		tableViewer = new TableViewer<>( tableRows, selectionModel, coloringModel, name ).show();
		coloringModel.listeners().add( tableViewer );
		selectionModel.listeners().add( tableViewer );

		sourceAndConverters = new ArrayList<>();
		showGridImage( bdvHandle, name, tableRows );

		coloringModel.listeners().add( this );
		selectionModel.listeners().add( this );
	}

	private void showGridImage( BdvHandle bdvHandle, String name, List< DefaultAnnotatedIntervalTableRow > tableRows )
	{
		final TableRowsIntervalImage< DefaultAnnotatedIntervalTableRow > intervalImage = new TableRowsIntervalImage<>( tableRows, coloringModel, name );
		SourceAndConverter< IntType > sourceAndConverter = intervalImage.getSourceAndConverter();
		SourceAndConverterServices.getSourceAndConverterDisplayService().show( bdvHandle, sourceAndConverter );
		sourceAndConverters.add( sourceAndConverter );
	}

	public TableViewer< DefaultAnnotatedIntervalTableRow > getTableViewer()
	{
		return tableViewer;
	}

	public BdvHandle getBdvHandle()
	{
		return bdvHandle;
	}

	@Override
	public void coloringChanged()
	{
		bdvHandle.getViewerPanel().requestRepaint();
	}

	@Override
	public void selectionChanged()
	{

	}

	@Override
	public void focusEvent( DefaultAnnotatedIntervalTableRow selection )
	{
		final double[] max = selection.interval.maxAsDoubleArray();
		final double[] min = selection.interval.minAsDoubleArray();
		final double[] center = new double[ min.length ];
		for ( int d = 0; d < 3; d++ )
		{
			center[ d ] = ( max[ d ] + min[ d ] ) / 2;
		}

		BdvLocationChanger.moveToLocation( bdvHandle, new BdvLocation( BdvLocationType.Position3d, center ) );

	}

	public String getName()
	{
		return name;
	}

	public void close()
	{
		for ( SourceAndConverter< ? > sourceAndConverter : sourceAndConverters )
		{
			SourceAndConverterServices.getSourceAndConverterDisplayService().removeFromAllBdvs( sourceAndConverter );
		}

		tableViewer.close();
	}
}
