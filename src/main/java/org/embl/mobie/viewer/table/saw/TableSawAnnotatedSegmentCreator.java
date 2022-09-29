package org.embl.mobie.viewer.table.saw;

import tech.tablesaw.api.Table;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public class TableSawAnnotatedSegmentCreator implements TableSawAnnotationCreator< TableSawAnnotatedSegment >
{
	public TableSawAnnotatedSegmentCreator(  )
	{

	}

	@Override
	public TableSawAnnotatedSegment create( Supplier< Table > tableSupplier, int rowIndex )
	{
		return new TableSawAnnotatedSegment( tableSupplier, rowIndex );
	}
}
