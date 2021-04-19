package de.embl.cba.mobie2.grid;

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

import java.util.List;
import java.util.Map;

public class GridView
{
	private final MoBIEColoringModel< DefaultAnnotatedIntervalTableRow > coloringModel;
	private final DefaultSelectionModel< DefaultAnnotatedIntervalTableRow > selectionModel;

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

		final TableViewer< DefaultAnnotatedIntervalTableRow > tableViewer = new TableViewer<>( tableRows, selectionModel, coloringModel, getImageName( name ) ).show();
	}

	private String getImageName( String name )
	{
		return name + "-grid-features";
	}
}
