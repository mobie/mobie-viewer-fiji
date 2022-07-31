package org.embl.mobie.viewer.table.saw;

import org.embl.mobie.viewer.annotation.SegmentAnnotation;
import net.imglib2.FinalRealInterval;
import net.imglib2.RealInterval;
import org.embl.mobie.viewer.table.ColumnNames;
import tech.tablesaw.api.Row;

import javax.annotation.Nullable;

public class TableSawSegmentAnnotation implements SegmentAnnotation
{
	private Row row;
	private final int numSegmentDimensions;
	private final int timePoint;
	private final int labelId;
	private RealInterval boundingBox;
	private float[] mesh;
	private String imageId;

	public TableSawSegmentAnnotation(
			Row row,
			@Nullable String imageId  // may be present in table
	)
	{
		this.row = row;

		// segment properties
		this.numSegmentDimensions = this.row.columnNames().contains( ColumnNames.ANCHOR_Z ) ? 3 : 2;
		this.imageId = row.columnNames().contains( ColumnNames.LABEL_IMAGE_ID ) ? this.row.getString( ColumnNames.LABEL_IMAGE_ID ) : imageId;
		this.timePoint = row.columnNames().contains( ColumnNames.TIMEPOINT ) ? this.row.getInt( ColumnNames.TIMEPOINT ) : 0;
		this.labelId = this.row.getInt( ColumnNames.LABEL_ID );
		initBoundingBox( this.row, numSegmentDimensions );
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
		return imageId;
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
	public double[] anchor()
	{
		return new double[]{
				row.getDouble( ColumnNames.ANCHOR_X ),
				row.getDouble( ColumnNames.ANCHOR_Y ),
				row.getDouble( ColumnNames.ANCHOR_Z )
		};
	}

	@Override
	public RealInterval boundingBox()
	{
		return null;
	}

	@Override
	public void setBoundingBox( RealInterval boundingBox )
	{

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
	public String id()
	{
		return null; // MUST
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

}
