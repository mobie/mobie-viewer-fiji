package org.embl.mobie.lib.table;

import net.imglib2.RealInterval;
import net.imglib2.realtransform.AffineTransform3D;
import org.embl.mobie.lib.annotation.AnnotatedSegment;

import java.util.HashMap;

public class DefaultAnnotatedSegment implements AnnotatedSegment
{
	private static final String[] idColumns = new String[]{ ColumnNames.LABEL_ID, ColumnNames.TIMEPOINT };
	public static final HashMap< String, Class > columnToClass = new HashMap<>();
	static {
		columnToClass.put( ColumnNames.LABEL_IMAGE_ID, String.class );
		columnToClass.put( ColumnNames.LABEL_ID, Integer.class );
		columnToClass.put( ColumnNames.TIMEPOINT, Integer.class );
	}

	private final String source;
	private final int timePoint;
	private final int labelId;
	private final double[] position;
	private RealInterval boundingBox;
	private float[] mesh;
	private String uuid;
	private HashMap< String, Object > columnToValue;

	public < A extends AnnotatedSegment > DefaultAnnotatedSegment( String source, int timePoint, int labelId )
	{
		this.source = source;
		this.timePoint = timePoint;
		this.labelId = labelId;
		this.position = null;

		columnToValue = new HashMap<>();
		columnToValue.put( ColumnNames.LABEL_IMAGE_ID, source );
		columnToValue.put( ColumnNames.TIMEPOINT, timePoint );
		columnToValue.put( ColumnNames.LABEL_ID, labelId );

		this.uuid = this.source + ";" + this.timePoint + ";" + this.labelId;
	}

	@Override
	public String imageId()
	{
		return source();
	}

	@Override
	public int label()
	{
		return labelId;
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
		return columnToValue.get( feature );
	}

	@Override
	public Double getNumber( String feature )
	{
		return new Double( String.valueOf( columnToValue.get( feature ) ) );
	}

	@Override
	public void setString( String columnName, String value )
	{
		// TODO current implementation would require making
		//  the static field mutable, which would not
		//  work for multiple tables using the DefaultAnnotatedSegment
//		columnToValue.put( columnName, value );
//		columnToClass.put( columnName, String.class );
		throw new UnsupportedOperationException( "Adding values to " + this.getClass() + " is not implemented." );
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
		//boundingBox = affineTransform3D.estimateBounds( boundingBox );
		//transform mesh
	}

	@Override
	public int numDimensions()
	{
		return positionAsDoubleArray().length;
	}
}
