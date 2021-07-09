package de.embl.cba.mobie.annotate;

import de.embl.cba.tables.tablerow.AbstractTableRow;
import net.imglib2.RealInterval;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DefaultAnnotatedIntervalTableRow extends AbstractTableRow implements AnnotatedIntervalTableRow
{
	protected final RealInterval interval;
	protected final Map< String, String > cells;
	protected final String siteName;

	public DefaultAnnotatedIntervalTableRow(
			String siteName,
			RealInterval interval,
			Map< String, List< String > > columns,
			int rowIndex )
	{
		this.siteName = siteName;
		this.interval = interval;

		// set cells
		this.cells = new LinkedHashMap<>();
		final List< String > columnNames = new ArrayList<>( columns.keySet() );
		Collections.sort( columnNames );
		for ( String column : columnNames )
		{
			cells.put( column, columns.get( column ).get( rowIndex ) );
		}
	}

	@Override
	public RealInterval getInterval()
	{
		return interval;
	}

	@Override
	public String getName()
	{
		return siteName;
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
