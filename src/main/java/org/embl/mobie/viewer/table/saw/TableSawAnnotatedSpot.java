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

	private String spotId;
	private int label;
	private final int timePoint;
	private double[] position;

	public TableSawAnnotatedSpot( Supplier< Table > tableSupplier, int rowIndex )
	{
		this.tableSupplier = tableSupplier;
		this.rowIndex = rowIndex;
		final Row row = tableSupplier.get().row( rowIndex );

		// fetch spot properties from table row
		this.spotId = null; // FIXME

		this.label = (int) row.getObject( ColumnNames.SPOT_ID );

		if ( row.columnNames().contains( ColumnNames.SPOT_Z ) )
		{
			this.position = new double[]{
					Double.parseDouble( row.getObject( ColumnNames.SPOT_X ).toString() ),
					Double.parseDouble( row.getObject( ColumnNames.SPOT_Y ).toString() ),
					Double.parseDouble( row.getObject( ColumnNames.SPOT_Z ).toString() )};
		}
		else // 2D
		{
			this.position = new double[]{
					(double) row.getObject( ColumnNames.SPOT_X ),
					(double) row.getObject( ColumnNames.SPOT_Y ) };
		}

		this.timePoint = row.columnNames().contains( ColumnNames.TIMEPOINT ) ? row.getInt( ColumnNames.TIMEPOINT ) : 0;
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
	public String id()
	{
		return spotId;
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
	public String spotId ( )
	{
		return spotId;
	}

	@Override
	public int numDimensions()
	{
		return position.length;
	}
}
