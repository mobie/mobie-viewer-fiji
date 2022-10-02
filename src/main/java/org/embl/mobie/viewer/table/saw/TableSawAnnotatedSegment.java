package org.embl.mobie.viewer.table.saw;

import org.embl.mobie.viewer.annotation.AnnotatedSegment;
import net.imglib2.FinalRealInterval;
import net.imglib2.RealInterval;
import org.embl.mobie.viewer.table.ColumnNames;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;

import java.util.List;
import java.util.function.Supplier;

public class TableSawAnnotatedSegment implements AnnotatedSegment
{
	private static final String[] idColumns = new String[]{ ColumnNames.LABEL_ID, ColumnNames.TIMEPOINT };

	private final int timePoint;
	private final int labelId;
	private final double[] position;
	private final Supplier< Table > tableSupplier;
	private final int rowIndex;
	private RealInterval boundingBox;
	private float[] mesh;
	private String source;
	private String uuid;

	public TableSawAnnotatedSegment(
			Supplier< Table > tableSupplier,
			int rowIndex )
	{
		this.tableSupplier = tableSupplier;
		this.rowIndex = rowIndex;
		final Table rows = tableSupplier.get();
		Row row = rows.row( rowIndex );

		final List< String > columnNames = row.columnNames();

		final boolean is3D = columnNames.contains( ColumnNames.ANCHOR_Z );

		this.source = columnNames.contains( ColumnNames.LABEL_IMAGE_ID ) ? row.getString( ColumnNames.LABEL_IMAGE_ID ) : rows.name();

		this.timePoint = columnNames.contains( ColumnNames.TIMEPOINT ) ? row.getInt( ColumnNames.TIMEPOINT ) : 0;

		this.labelId = row.getInt( ColumnNames.LABEL_ID );

		initBoundingBox( row, is3D );

		this.position = new double[]{
				row.getDouble( ColumnNames.ANCHOR_X ),
				row.getDouble( ColumnNames.ANCHOR_Y ),
				is3D ? row.getDouble( ColumnNames.ANCHOR_Z ) : 0
		};

		this.uuid = source + ";" + timePoint + ";" + labelId;
	}

	private void initBoundingBox( Row row, boolean is3D )
	{
		final boolean rowContainsBoundingBox = row.columnNames().contains( ColumnNames.BB_MIN_X );

		if ( ! rowContainsBoundingBox ) return;

		final double[] min = {
				row.getDouble( ColumnNames.BB_MIN_X ),
				row.getDouble( ColumnNames.BB_MIN_Y ),
				is3D ? row.getDouble( ColumnNames.BB_MIN_Z ) : 0
			};

		final double[] max = {
				row.getDouble( ColumnNames.BB_MAX_X ),
				row.getDouble( ColumnNames.BB_MAX_Y ),
				is3D ? row.getDouble( ColumnNames.BB_MAX_Z ) : 0
		};

		boundingBox = new FinalRealInterval( min, max );
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
		return positionAsDoubleArray()[ d ];
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
		return positionAsDoubleArray().length;
	}
}
