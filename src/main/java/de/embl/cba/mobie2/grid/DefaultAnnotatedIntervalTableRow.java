package de.embl.cba.mobie2.grid;

import de.embl.cba.tables.tablerow.AbstractTableRow;
import net.imglib2.Interval;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class DefaultAnnotatedIntervalTableRow extends AbstractTableRow implements AnnotatedIntervalTableRow
{
	protected final Interval interval;
	protected final Map< String, List< String > > columns;
	protected final String siteName;
	protected final int rowIndex;

	public DefaultAnnotatedIntervalTableRow(
			String siteName,
			Interval interval,
			Map< String, List< String > > columns,
			int rowIndex )
	{
		this.siteName = siteName;
		this.interval = interval;
		this.columns = columns;
		this.rowIndex = rowIndex;
	}

	@Override
	public Interval getInterval()
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
		return columns.get( columnName ).get( rowIndex );
	}

	@Override
	public void setCell( String columnName, String value )
	{
		columns.get( columnName ).set( rowIndex, value );
		notifyCellChangedListeners( columnName, value );
	}

	@Override
	public Set< String > getColumnNames()
	{
		return columns.keySet();
	}

	@Override
	public int rowIndex()
	{
		return rowIndex;
	}
}
