package mobie3.viewer.table;

import net.imglib2.FinalRealInterval;
import net.imglib2.RealInterval;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

public class TableSawAnnotatedSegment implements AnnotatedSegment
{
	private Row row;
	private final int numSegmentDimensions;
	private final Table table;
	private final int rowIndex;
	private RealInterval boundingBox;
	private float[] mesh;

	public TableSawAnnotatedSegment( Table table, int rowIndex )
	{
		this.table = table;
		this.rowIndex = rowIndex;

		this.row = table.row( rowIndex );
		this.numSegmentDimensions = row.columnNames().contains( ColumnNames.ANCHOR_Z ) ? 3 : 2;
		initBoundingBox( row, numSegmentDimensions );
	}

	private void initBoundingBox( tech.tablesaw.api.Row row, int numSegmentDimensions )
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
		return row.getString( ColumnNames.LABEL_IMAGE_ID );
	}

	@Override
	public int labelId()
	{
		return row.getInt( ColumnNames.LABEL_ID );
	}

	@Override
	public int timePoint()
	{
		return row.getInt( ColumnNames.TIMEPOINT );
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
	public Object getValue( String columnName )
	{
		return row.getObject( columnName );
	}

	@Override
	public void setString( String columnName, String value )
	{
		if ( ! table.containsColumn( columnName ) )
		{
			final StringColumn strings = StringColumn.create( columnName, table.rowCount() );
			table.addColumns( strings );
		}
		this.row = table.row( rowIndex ); // update with new column
		row.setText( columnName, value );
	}

}
