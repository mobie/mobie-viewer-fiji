package org.embl.mobie.viewer.table.saw;

import org.embl.mobie.viewer.annotation.AnnotatedSegment;
import net.imglib2.FinalRealInterval;
import net.imglib2.RealInterval;
import org.embl.mobie.viewer.table.ColumnNames;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;

import java.util.function.Supplier;

public class TableSawAnnotatedSegment implements AnnotatedSegment
{
	private static final String[] idColumns = new String[]{ ColumnNames.LABEL_ID, ColumnNames.TIMEPOINT };

	private final int numSegmentDimensions;
	private final int timePoint;
	private final int labelId;
	private final double[] position;
	private final Supplier< Table > tableSupplier;
	private final int rowIndex;
	private RealInterval boundingBox;
	private float[] mesh;
	private String sourceName;
	private String uuid;

	public TableSawAnnotatedSegment(
			Supplier< Table > tableSupplier,
			int rowIndex )
	{
		this.tableSupplier = tableSupplier;
		this.rowIndex = rowIndex;
		final Table rows = tableSupplier.get();
		Row row = rows.row( rowIndex );

		// segment properties
		this.numSegmentDimensions = row.columnNames().contains( ColumnNames.ANCHOR_Z ) ? 3 : 2;
		this.sourceName = row.columnNames().contains( ColumnNames.LABEL_IMAGE_ID ) ? row.getString( ColumnNames.LABEL_IMAGE_ID ) : rows.name();
		this.timePoint = row.columnNames().contains( ColumnNames.TIMEPOINT ) ? row.getInt( ColumnNames.TIMEPOINT ) : 0;
		this.labelId = row.getInt( ColumnNames.LABEL_ID );

		initBoundingBox( row, numSegmentDimensions );

		this.position = new double[]{
				row.getDouble( ColumnNames.ANCHOR_X ),
				row.getDouble( ColumnNames.ANCHOR_Y ),
				row.getDouble( ColumnNames.ANCHOR_Z )
		};

		this.uuid = ""+ sourceName +";"+timePoint+";"+labelId;
	}

	private void initBoundingBox( Row row, int numSegmentDimensions )
	{
		if ( row.columnNames().contains( ColumnNames.BB_MIN_X ) )
		{
			if ( numSegmentDimensions == 2 )
			{
				final double[] min = {
						row.getDouble( ColumnNames.BB_MIN_X ),
						row.getDouble( ColumnNames.BB_MIN_Y )
				};
				final double[] max = {
						row.getDouble( ColumnNames.BB_MAX_X ),
						row.getDouble( ColumnNames.BB_MAX_Y )
				};
				boundingBox = new FinalRealInterval( min, max );
			}
			else if ( numSegmentDimensions == 3 )
			{
				final double[] min = {
						row.getDouble( ColumnNames.BB_MIN_X ),
						row.getDouble( ColumnNames.BB_MIN_Y ),
						row.getDouble( ColumnNames.BB_MIN_Z )
				};
				final double[] max = {
						row.getDouble( ColumnNames.BB_MAX_X ),
						row.getDouble( ColumnNames.BB_MAX_Y ),
						row.getDouble( ColumnNames.BB_MAX_Z )
				};
				boundingBox = new FinalRealInterval( min, max );
			}
		}
	}

	@Override
	public String imageId()
	{
		return dataSource();
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
	public String dataSource()
	{
		return sourceName;
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
