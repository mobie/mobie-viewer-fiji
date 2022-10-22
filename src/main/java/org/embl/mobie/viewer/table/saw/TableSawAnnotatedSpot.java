package org.embl.mobie.viewer.table.saw;

import net.imglib2.realtransform.AffineTransform3D;
import org.embl.mobie.viewer.annotation.AnnotatedSpot;
import org.embl.mobie.viewer.table.ColumnNames;
import tech.tablesaw.api.Table;

import java.util.List;
import java.util.function.Supplier;

public class TableSawAnnotatedSpot implements AnnotatedSpot
{
	private static final String[] idColumns = new String[]{ ColumnNames.SPOT_ID };
	private final Supplier< Table > tableSupplier;
	private final int rowIndex;
	private final String uuid;
	private final int label;
	private final int timePoint;
	private final String source;

	private double[] position; // may change due to transformations

	// We use {@code Supplier< Table > tableSupplier}
	// because the table object may change, e.g.
	// due to merging of additional columns.
	public TableSawAnnotatedSpot( final Supplier< Table > tableSupplier, int rowIndex, int label, double[] position, final int timePoint, String source, String uuid )
	{
		this.tableSupplier = tableSupplier;
		this.rowIndex = rowIndex;
		this.label = label;
		this.position = position;
		this.timePoint = timePoint;

		this.source = source;
		this.uuid = uuid;
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
