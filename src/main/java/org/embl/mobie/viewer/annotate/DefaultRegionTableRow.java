package org.embl.mobie.viewer.annotate;

import de.embl.cba.tables.tablerow.AbstractTableRow;
import net.imglib2.roi.RealMaskRealInterval;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DefaultRegionTableRow extends AbstractTableRow implements RegionTableRow
{
	protected final RealMaskRealInterval mask;
	protected final Map< String, String > cells;
	protected final String name;

	// TODO: rename to annotated region?
	public DefaultRegionTableRow(
			String name,
			RealMaskRealInterval mask,
			Map< String, List< String > > columns,
			int rowIndex )
	{
		this.name = name;
		this.mask = mask;

		// set cells
		this.cells = new LinkedHashMap<>();
		for ( String column : columns.keySet() )
			cells.put( column, columns.get( column ).get( rowIndex ) );

	}

	@Override
	public RealMaskRealInterval mask()
	{
		return mask;
	}

	@Override
	public Integer timePoint()
	{
		if ( cells.containsKey( "timepoint" ) )
			return Integer.parseInt( cells.get( "timepoint" ) );
		else
			return 0;
	}

	@Override
	public String name()
	{
		return name;
	}

	@Override
	public String getCell( String columnName )
	{
		return cells.get( columnName );
	}

	@Override
	public void setCell( String columnName, String value )
	{
		cells.put( columnName, value );
		notifyCellChangedListeners( columnName, value );
	}

	@Override
	public Set< String > getColumnNames()
	{
		return cells.keySet();
	}

	@Override
	@Deprecated
	public int rowIndex()
	{
		return -1;
	}

}
