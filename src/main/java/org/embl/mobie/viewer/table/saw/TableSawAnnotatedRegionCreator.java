package org.embl.mobie.viewer.table.saw;

import org.embl.mobie.viewer.table.ColumnNames;
import tech.tablesaw.api.Row;

import java.util.List;
import java.util.Map;

public class TableSawAnnotatedRegionCreator implements TableSawAnnotationCreator< TableSawAnnotatedRegion >
{
	private final Map< String, List< String > > regionIdToImageNames;

	public TableSawAnnotatedRegionCreator( Map< String, List< String > > regionIdToImageNames )
	{
		this.regionIdToImageNames = regionIdToImageNames;
	}

	@Override
	public TableSawAnnotatedRegion create( Row row )
	{
		final String regionId = row.getObject( ColumnNames.SPOT_X ).toString();
		if ( ! regionIdToImageNames.containsKey( regionId ) )
			return null; // The regionDisplay may only use some table rows.

		return new TableSawAnnotatedRegion( row, regionIdToImageNames.get( regionId )  );
	}
}
