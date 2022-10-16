package org.embl.mobie.viewer.table;

import net.imglib2.RealInterval;
import net.imglib2.realtransform.AffineTransform3D;
import org.embl.mobie.viewer.annotation.AnnotatedSegment;

public class DefaultAnnotatedSegment implements AnnotatedSegment
{
	private static final String[] idColumns = new String[]{ ColumnNames.LABEL_ID, ColumnNames.TIMEPOINT };

	private final String source;
	private final int timePoint;
	private final int label;
	private final AnnotationTableModel< ? extends AnnotatedSegment > tableModel;
	private final int rowIndex;
	private final double[] position;
	private RealInterval boundingBox;
	private float[] mesh;
	private String uuid;

	public < A extends AnnotatedSegment > DefaultAnnotatedSegment( String source, int timePoint, int label )
	{
		this.source = source;
		this.timePoint = timePoint;
		this.label = label;
		this.tableModel = tableModel;
		this.rowIndex = rowIndex;
		this.position = new double[]{0,0,0};

		this.uuid = this.source + ";" + this.timePoint + ";" + this.label;
	}

	@Override
	public String imageId()
	{
		return source();
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
	public RealInterval boundingBox()
	{
		return boundingBox;
	}

	@Override
	public void setBoundingBox( RealInterval boundingBox )
	{
		this.boundingBox = boundingBox;
	}

	@Override
	public float[] mesh()
	{
		return mesh;
	}

	@Override
	public void setMesh( float[] mesh )
	{
		this.mesh = mesh;
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
		return tableModel..row( rowIndex ).getObject( feature );
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
		// update fields
		affineTransform3D.apply( position, position );
		boundingBox = affineTransform3D.estimateBounds( boundingBox );
		// FIXME transform mesh
	}

	@Override
	public int numDimensions()
	{
		return positionAsDoubleArray().length;
	}
}
