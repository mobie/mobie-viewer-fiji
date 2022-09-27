package org.embl.mobie.viewer.table.saw;

import org.embl.mobie.viewer.table.ColumnNames;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class TableSawAnnotatedSpotCreator implements TableSawAnnotationCreator< TableSawAnnotatedSpot >
{
	public TableSawAnnotatedSpotCreator()
	{
	}

	@Override
	public TableSawAnnotatedSpot create( Supplier< Table > tableSupplier, int rowIndex )
	{
		return new TableSawAnnotatedSpot( tableSupplier, rowIndex );
	}
}
