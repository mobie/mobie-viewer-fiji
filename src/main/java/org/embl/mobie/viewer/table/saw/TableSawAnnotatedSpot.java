package org.embl.mobie.viewer.table.saw;

import net.imglib2.realtransform.AffineTransform3D;
import org.embl.mobie.viewer.annotation.AnnotatedSpot;
import org.embl.mobie.viewer.table.ColumnNames;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;

import java.util.List;
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

	// We use {@code Supplier< Table > tableSupplier}
	// because the table may change, e.g.
	// due to merging of additional columns.
	public TableSawAnnotatedSpot( final Supplier< Table > tableSupplier, final int rowIndex )
	{
		this.tableSupplier = tableSupplier;
		this.rowIndex = rowIndex;

		final Table table = tableSupplier.get();
		final Row row = table.row( rowIndex );
		final List< String > columnNames = row.columnNames();

		this.label = row.getInt( ColumnNames.SPOT_ID );

		if ( columnNames.contains( ColumnNames.SPOT_Z ) )
		{
			this.position = new double[]{
					row.getNumber( ColumnNames.SPOT_X ),
					row.getNumber( ColumnNames.SPOT_Y ),
					row.getNumber( ColumnNames.SPOT_Z )
			};
		}
		else // 2D
		{
			this.position = new double[]{
					row.getNumber( ColumnNames.SPOT_X ),
					row.getNumber( ColumnNames.SPOT_Y )
			};
		}

		this.timePoint = columnNames.contains( ColumnNames.TIMEPOINT ) ? row.getInt( ColumnNames.TIMEPOINT ) : 0;
		this.source = table.name();
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
		return position[ d ];
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
	public Double getNumber( String feature )
	{
		return tableSupplier.get().row( rowIndex ).getNumber( feature );
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
	public void transform( AffineTransform3D affineTransform3D )
	{
		affineTransform3D.apply( position, position );
	}

	@Override
	public int numDimensions()
	{
		return position.length;
	}
}
