package org.embl.mobie.viewer.table.saw;

import tech.tablesaw.api.Row;

import javax.annotation.Nullable;

public class TableSawSegmentAnnotationCreator implements TableSawAnnotationCreator< TableSawSegmentAnnotation >
{
	@Nullable
	private final String imageId;

	public TableSawSegmentAnnotationCreator( @Nullable String imageId )
	{
		this.imageId = imageId;
	}

	@Override
	public TableSawSegmentAnnotation create( Row row )
	{
		return new TableSawSegmentAnnotation( row, imageId );
	}
}
