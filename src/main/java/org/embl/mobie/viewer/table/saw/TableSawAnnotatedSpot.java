package org.embl.mobie.viewer.table.saw;

import org.embl.mobie.viewer.annotation.AnnotatedSpot;
import org.embl.mobie.viewer.table.ColumnNames;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;

import java.util.function.Supplier;

public class TableSawAnnotatedSpot implements AnnotatedSpot
{
	private static final String[] idColumns = new String[]{ ColumnNames.SPOT_ID };
	private final Supplier< Table > tableSupplier;
	private final int rowIndex;

	private String uuid;
	private int label;
	private final int timePoint;
	private double[] position;
	private String source;

	public TableSawAnnotatedSpot( Supplier< Table > tableSupplier, int rowIndex )
	{
		this.tableSupplier = tableSupplier;
		this.rowIndex = rowIndex;

		final Table rows = tableSupplier.get();
		final Row row = rows.row( rowIndex );

		this.label = row.getInt( ColumnNames.SPOT_ID );

		if ( row.columnNames().contains( ColumnNames.SPOT_Z ) )
		{
			this.position = new double[]{
					row.getNumber( ColumnNames.SPOT_X ),
					row.getNumber( ColumnNames.SPOT_Y ),
					row.getNumber( ColumnNames.SPOT_Z )};
		}
		else // 2D
		{
			this.position = new double[]{
					row.getNumber( ColumnNames.SPOT_X ),
					row.getNumber( ColumnNames.SPOT_Y ) };
		}

		this.timePoint = row.columnNames().contains( ColumnNames.TIMEPOINT ) ? row.getInt( ColumnNames.TIMEPOINT ) : 0;
		this.source = tableSupplier.get().name();
		this.uuid = source + ";" + timePoint + ";" + label;
	}

	@Override
	public int label()
	{
		return label;
	}

	@Override
	public int timePoint()
	{
		return timePoint;
	}

	@Override
	public double[] positionAsDoubleArray()
	{
		return position;
	}

	@Override
	public double getDoublePosition( int d )
	{
		return positionAsDoubleArray()[ d ];
	}

	@Override
	public String uuid()
	{
		return uuid;
	}

	@Override
	public String source()
	{
		return source;
	}

	@Override
	public Object getValue( String feature )
	{
		return tableSupplier.get().row( rowIndex ).getObject( feature );
	}

	@Override
	public void setString( String columnName, String value )
	{
		tableSupplier.get().row( rowIndex ).setText( columnName, value );
	}

	@Override
	public String[] idColumns()
	{
		return idColumns;
	}

	@Override
	public int numDimensions()
	{
		return position.length;
	}
}
