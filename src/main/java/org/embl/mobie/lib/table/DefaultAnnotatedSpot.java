package org.embl.mobie.lib.table;

import net.imglib2.realtransform.AffineTransform3D;
import org.embl.mobie.lib.annotation.AnnotatedSpot;

public class DefaultAnnotatedSpot implements AnnotatedSpot
{
	private static final String[] idColumns = new String[]{ ColumnNames.SPOT_ID };
	private final int rowIndex;

	private String uuid; // FIXME not initialised !
	private int label;
	private double[] position;
	private String source;

	// We use {@code Supplier< Table > tableSupplier}
	// because the table may change, e.g.
	// due to merging of additional columns.
	public DefaultAnnotatedSpot( final double[] position, final int rowIndex )
	{
		this.rowIndex = rowIndex;
		this.label = rowIndex;
		this.position = position;
	}

	@Override
	public int label()
	{
		return label;
	}

	@Override
	public Integer timePoint()
	{
		return 0;
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
		return null;
	}

	@Override
	public Double getNumber( String feature )
	{
		return null;
	}

	@Override
	public void setString( String columnName, String value )
	{

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
