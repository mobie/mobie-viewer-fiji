package de.embl.cba.mobie.grid;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import de.embl.cba.bdv.utils.Logger;
import de.embl.cba.mobie.Utils;
import de.embl.cba.mobie.MoBIE;
import de.embl.cba.mobie.transform.PositionViewerTransform;
import de.embl.cba.mobie.transform.ViewerTransformChanger;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GridOverlaySourceDisplay extends SourceDisplay implements ColoringListener, SelectionListener< DefaultAnnotatedIntervalTableRow >
{
	private final MoBIEColoringModel< DefaultAnnotatedIntervalTableRow > coloringModel;
	private final DefaultSelectionModel< DefaultAnnotatedIntervalTableRow > selectionModel;
	private final TableViewer< DefaultAnnotatedIntervalTableRow > tableViewer;

	private final BdvHandle bdvHandle;

	public GridOverlaySourceDisplay( MoBIE moBIE, BdvHandle bdvHandle, String tableDataFolder, GridSourceTransformer sourceTransformer )
	{
		this.bdvHandle = bdvHandle;
		this.name = sourceTransformer.getName();

		// open tables
		final List< DefaultAnnotatedIntervalTableRow > tableRows = openGridTables( moBIE, tableDataFolder, sourceTransformer, sourceTransformer.tables );

		coloringModel = new MoBIEColoringModel< >( ColoringLuts.GLASBEY );
		selectionModel = new DefaultSelectionModel< >();
		coloringModel.setSelectionModel( selectionModel );

		HashMap<String, String> nameToTableDir = new HashMap<>();
		nameToTableDir.put( sourceTransformer.getName(), tableDataFolder );
		tableViewer = new TableViewer<>( moBIE, tableRows, selectionModel, coloringModel, name, nameToTableDir, true ).show();

		coloringModel.listeners().add( tableViewer );
		selectionModel.listeners().add( tableViewer );

		showGridImage( bdvHandle, name, tableRows );

		coloringModel.listeners().add( this );
		selectionModel.listeners().add( this );
	}

	// TODO: maybe replace the GridSourceTransform by a functional?
	private List< DefaultAnnotatedIntervalTableRow > openGridTables( MoBIE moBIE, String tableDataFolder, GridSourceTransformer sourceTransformer, String[] relativeTablePaths )
	{
		// open
		final List< Map< String, List< String > > > tables = new ArrayList<>();
		for ( String table : relativeTablePaths )
		{
			String tablePath = moBIE.getTablePath( tableDataFolder, table );
			tablePath = Utils.resolveTablePath( tablePath );
			Logger.log( "Opening table:\n" + tablePath );
			tables.add( TableColumns.stringColumnsFromTableFile( tablePath ) );
		}

		// create primary AnnotatedIntervalTableRow table
		final Map< String, List< String > > referenceTable = tables.get( 0 );
		final AnnotatedIntervalCreator annotatedIntervalCreator = new AnnotatedIntervalCreator( referenceTable, sourceTransformer );
		final List< DefaultAnnotatedIntervalTableRow > intervalTableRows = annotatedIntervalCreator.getTableRows();

		final List< Map< String, List< String > > > additionalTables = tables.subList( 1, tables.size() );

		for ( int i = 0; i < additionalTables.size(); i++ )
		{
			MoBIE.mergeImageTable( intervalTableRows, additionalTables.get( i ) );
		}

		return intervalTableRows;
	}

	private void showGridImage( BdvHandle bdvHandle, String name, List< DefaultAnnotatedIntervalTableRow > tableRows )
	{
		final TableRowsIntervalImage< DefaultAnnotatedIntervalTableRow > intervalImage = new TableRowsIntervalImage<>( tableRows, coloringModel, name );
		SourceAndConverter< IntType > sourceAndConverter = intervalImage.getSourceAndConverter();
		SourceAndConverterServices.getBdvDisplayService().show( bdvHandle, sourceAndConverter );
		sourceAndConverters = new ArrayList<>();
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
		bdvHandle.getViewerPanel().requestRepaint();
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

		ViewerTransformChanger.changeViewerTransform( bdvHandle, new PositionViewerTransform( center, bdvHandle.getViewerPanel().state().getCurrentTimepoint() ) );

	}

	public String getName()
	{
		return name;
	}

	public void close()
	{
		for ( SourceAndConverter< ? > sourceAndConverter : sourceAndConverters )
		{
			SourceAndConverterServices.getBdvDisplayService().removeFromAllBdvs( sourceAndConverter );
		}

		tableViewer.close();
	}
}
