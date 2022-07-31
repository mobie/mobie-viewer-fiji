package org.embl.mobie.viewer.table.saw;

import org.embl.mobie.viewer.table.ColumnNames;
import tech.tablesaw.api.Row;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public class TableSawImageAnnotationCreator implements TableSawAnnotationCreator< TableSawImageAnnotation >
{
	private final Map< String, List< String > > regionIdToImageNames;

	public TableSawImageAnnotationCreator( Map< String, List< String > > regionIdToImageNames )
	{
		this.regionIdToImageNames = regionIdToImageNames;
	}

	@Override
	public TableSawImageAnnotation create( Row row )
	{
		final String regionId = row.getString( ColumnNames.REGION_ID );
		return new TableSawImageAnnotation( row, regionIdToImageNames.get( regionId )  );
	}
}
