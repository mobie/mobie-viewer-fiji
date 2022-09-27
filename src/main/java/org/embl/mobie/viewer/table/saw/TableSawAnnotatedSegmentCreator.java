package org.embl.mobie.viewer.table.saw;

import tech.tablesaw.api.Table;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public class TableSawAnnotatedSegmentCreator implements TableSawAnnotationCreator< TableSawAnnotatedSegment >
{
	@Nullable
	private final String imageId;

	public TableSawAnnotatedSegmentCreator( @Nullable String imageId )
	{
		this.imageId = imageId;
	}

	@Override
	public TableSawAnnotatedSegment create( Supplier< Table > tableSupplier, int rowIndex )
	{
		return new TableSawAnnotatedSegment( tableSupplier, rowIndex, imageId );
	}
}
