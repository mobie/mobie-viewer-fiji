package org.embl.mobie.viewer.table.saw;

import net.imglib2.realtransform.AffineTransform3D;
import org.apache.kerby.cms.type.OtherRecipientInfo;
import org.embl.mobie.viewer.annotation.AnnotatedSpot;
import org.embl.mobie.viewer.table.ColumnNames;
import tech.tablesaw.api.Table;

public class TableSawAnnotatedSpot extends AbstractTableSawAnnotation implements AnnotatedSpot
{
	private static final String[] idColumns = new String[]{ ColumnNames.SPOT_ID };
	private final int label;
	private final int timePoint;
	private final String source;
	private float[] position; // may change due to transformations

	// We use {@code Supplier< Table > tableSupplier}
	// because the table object may change, e.g.
	// due to merging of additional columns.
	public TableSawAnnotatedSpot(
			final TableSawAnnotationTableModel< TableSawAnnotatedSpot > model,
			int rowIndex,
			int label,
			float[] position,
			final int timePoint,
			String source )
	{
		super( model, rowIndex );
		this.label = label;
		this.position = position;
		this.timePoint = timePoint;

		this.source = source;
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
		// Create new array (don't cache), because the {@code position}
		// is subject to change by means of a transformation
		final double[] doublePosition = new double[ position.length ];
		for ( int d = 0; d < position.length; d++ )
			doublePosition[ d ] = position[ d ];
		return doublePosition;
	}

	@Override
	public double getDoublePosition( int d )
	{
		return position[ d ];
	}

	@Override
	public float getFloatPosition( int d )
	{
		return position[ d ];
	}

	@Override
	public String uuid()
	{
		return source + ";" + timePoint + ";" + label;
	}

	@Override
	public String source()
	{
		return source;
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
