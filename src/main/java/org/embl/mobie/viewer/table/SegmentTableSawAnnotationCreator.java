package org.embl.mobie.viewer.table;

import tech.tablesaw.api.Table;

import javax.annotation.Nullable;

public class SegmentTableSawAnnotationCreator implements TableSawAnnotationCreator< TableSawAnnotatedSegment >
{
	@Nullable
	private final String imageId;

	public SegmentTableSawAnnotationCreator( @Nullable String imageId )
	{
		this.imageId = imageId;
	}

	@Override
	public TableSawAnnotatedSegment create( Table table, int rowIndex )
	{
		return new TableSawAnnotatedSegment( table, rowIndex, imageId );
	}
}
