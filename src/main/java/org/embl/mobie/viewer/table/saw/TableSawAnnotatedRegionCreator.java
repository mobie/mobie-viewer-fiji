package org.embl.mobie.viewer.table.saw;

import org.embl.mobie.viewer.table.ColumnNames;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class TableSawAnnotatedRegionCreator implements TableSawAnnotationCreator< TableSawAnnotatedRegion >
{
	private final Map< String, List< String > > regionIdToImageNames;

	public TableSawAnnotatedRegionCreator( Map< String, List< String > > regionIdToImageNames )
	{
		this.regionIdToImageNames = regionIdToImageNames;
	}

	@Override
	public TableSawAnnotatedRegion create( Supplier< Table > tableSupplier , int rowIndex )
	{
		final Row row = tableSupplier.get().row( rowIndex );
		final String regionId = row.getObject( ColumnNames.SPOT_X ).toString();
		if ( ! regionIdToImageNames.containsKey( regionId ) )
			return null; // The regionDisplay may only use some table rows.

		return new TableSawAnnotatedRegion( tableSupplier, rowIndex, regionIdToImageNames.get( regionId )  );
	}
}
