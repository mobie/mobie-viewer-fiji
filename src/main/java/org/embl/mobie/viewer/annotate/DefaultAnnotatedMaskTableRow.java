package org.embl.mobie.viewer.annotate;

import de.embl.cba.tables.tablerow.AbstractTableRow;
import net.imglib2.RealInterval;
import net.imglib2.roi.RealMask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DefaultAnnotatedMaskTableRow extends AbstractTableRow implements AnnotatedMaskTableRow
{
	protected final RealMask mask;
	protected final Map< String, String > cells;
	protected final String siteName;

	public DefaultAnnotatedMaskTableRow(
			String siteName,
			RealMask mask,
			Map< String, List< String > > columns,
			int rowIndex )
	{
		this.siteName = siteName;
		this.mask = mask;

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
	public RealMask getMask()
	{
		return mask;
	}

	@Override
	public Integer getTimepoint()
	{
		if ( cells.containsKey( "timepoint" ) )
			return Integer.parseInt( cells.get( "timepoint" ) );
		else
			return 0;
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
