package org.embl.mobie.viewer.table.saw;

import org.embl.mobie.viewer.table.ColumnNames;
import tech.tablesaw.api.Row;

import java.util.List;
import java.util.Map;

public class TableSawAnnotatedSpotCreator implements TableSawAnnotationCreator< TableSawAnnotatedSpot >
{
	public TableSawAnnotatedSpotCreator()
	{
	}

	@Override
	public TableSawAnnotatedSpot create( Row row )
	{
		return new TableSawAnnotatedSpot( row );
	}
}
