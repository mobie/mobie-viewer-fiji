package org.embl.mobie.viewer.table.saw;

import org.embl.mobie.viewer.annotation.AnnotatedSpot;
import org.embl.mobie.viewer.table.ColumnNames;
import tech.tablesaw.api.Row;

public class TableSawAnnotatedSpot implements AnnotatedSpot
{
	private Row row;
	private String spotId;
	private int label;
	private final int timePoint;
	private double[] position;

	public TableSawAnnotatedSpot( Row row )
	{
		this.row = row;

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
		return row.getObject( feature );
	}

	@Override
	public void setString( String columnName, String value )
	{
		row.setText( columnName, value );
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
