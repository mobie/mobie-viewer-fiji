package org.embl.mobie.viewer.table.saw;

import tech.tablesaw.api.Row;

import javax.annotation.Nullable;

public class TableSawSegmentAnnotationCreator implements TableSawAnnotationCreator< TableSawAnnotatedSegment >
{
	@Nullable
	private final String imageId;

	public TableSawSegmentAnnotationCreator( @Nullable String imageId )
	{
		this.imageId = imageId;
	}

	@Override
	public TableSawAnnotatedSegment create( Row row )
	{
		return new TableSawAnnotatedSegment( row, imageId );
	}
}
