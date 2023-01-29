package org.embl.mobie.lib.table.saw;

import org.embl.mobie.lib.table.ColumnNames;
import tech.tablesaw.api.Table;

import java.util.List;
import java.util.Map;

public class TableSawAnnotatedRegionCreator implements TableSawAnnotationCreator< TableSawAnnotatedRegion >
{
	private final Map< String, List< String > > regionIdToImageNames;
	private final int timePointColumnIndex;
	private int regionIdColumnIndex;

	public TableSawAnnotatedRegionCreator( Table table, Map< String, List< String > > regionIdToImageNames )
	{
		final List< String > columnNames = table.columnNames();
		this.regionIdToImageNames = regionIdToImageNames;
		regionIdColumnIndex = columnNames.indexOf( ColumnNames.REGION_ID );
		timePointColumnIndex = columnNames.indexOf( ColumnNames.TIMEPOINT );
	}

	@Override
	public TableSawAnnotatedRegion create( TableSawAnnotationTableModel< TableSawAnnotatedRegion > model, int rowIndex )
	{
		final Table table = model.getTable();

		final String regionId = table.getString( rowIndex, regionIdColumnIndex );

		final int labelId = 1 + rowIndex; // 0 is the background label, thus we add 1

		int timePoint = 0;
		if ( timePointColumnIndex > -1 )
			timePoint = (int) table.get( rowIndex, timePointColumnIndex );

		final String uuid = timePoint + ";" + regionId;

		return new TableSawAnnotatedRegion( model, rowIndex, regionIdToImageNames.get( regionId ), timePoint, regionId, labelId, uuid );
	}

	@Override
	public int[] removeColumns()
	{
		return new int[ 0 ];
	}
}
