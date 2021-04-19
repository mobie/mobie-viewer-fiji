package de.embl.cba.mobie2.grid;

import bdv.viewer.SourceAndConverter;
import de.embl.cba.bdv.utils.Logger;
import de.embl.cba.mobie.utils.Utils;
import de.embl.cba.mobie2.MoBIE2;
import de.embl.cba.mobie2.color.MoBIEColoringModel;
import de.embl.cba.mobie2.transform.GridSourceTransformer;
import de.embl.cba.mobie2.transform.SourceTransformer;
import de.embl.cba.mobie2.view.TableViewer;
import de.embl.cba.tables.TableColumns;
import de.embl.cba.tables.color.ColoringLuts;
import de.embl.cba.tables.select.DefaultSelectionModel;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import net.imglib2.type.numeric.integer.IntType;

import java.util.List;
import java.util.Map;

public class GridView
{
	private final MoBIEColoringModel< DefaultAnnotatedIntervalTableRow > coloringModel;
	private final DefaultSelectionModel< DefaultAnnotatedIntervalTableRow > selectionModel;
	private final SourceAndConverter< IntType > sourceAndConverter;
	private final TableViewer< DefaultAnnotatedIntervalTableRow > tableViewer;

	public GridView( MoBIE2 moBIE2, String name, String tableDataFolder, GridSourceTransformer sourceTransformer )
	{
		String tablePath = moBIE2.getDefaultTableLocation( tableDataFolder );
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

		final TableRowsIntervalImage< DefaultAnnotatedIntervalTableRow > intervalImage = new TableRowsIntervalImage<>( tableRows, coloringModel, name );

		sourceAndConverter = intervalImage.getSourceAndConverter();
	}

	public SourceAndConverter< IntType > getSourceAndConverter()
	{
		return sourceAndConverter;
	}

	public TableViewer< DefaultAnnotatedIntervalTableRow > getTableViewer()
	{
		return tableViewer;
	}
}
