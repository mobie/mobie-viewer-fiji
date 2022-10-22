package org.embl.mobie.viewer.table.saw;

import org.embl.mobie.viewer.annotation.Annotation;
import tech.tablesaw.api.Table;

public abstract class AbstractTableSawAnnotation implements Annotation
{
	protected final Table table;
	protected final int rowIndex;

	public AbstractTableSawAnnotation( final Table table , int rowIndex )
	{
		this.table = table;
		this.rowIndex = rowIndex;
	}

	@Override
	public Object getValue( String feature )
	{
		return table.get( rowIndex, table.columnIndex( feature ) );
	}

	@Override
	public Double getNumber( String feature )
	{
		return table.numberColumn( feature ).getDouble( rowIndex );
	}

	@Override
	public void setString( String columnName, String value )
	{
		table.stringColumn( columnName ).set( rowIndex, value );
	}

}
