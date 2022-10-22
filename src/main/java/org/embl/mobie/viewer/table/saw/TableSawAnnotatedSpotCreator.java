package org.embl.mobie.viewer.table.saw;

import org.embl.mobie.viewer.table.ColumnNames;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class TableSawAnnotatedSpotCreator implements TableSawAnnotationCreator< TableSawAnnotatedSpot >
{
	private final int spotIDColumnIndex;
	private final int xColumnIndex;
	private final int yColumnIndex;
	private final int zColumnIndex;
	private int timePointColumnIndex;

	public TableSawAnnotatedSpotCreator( Table table )
	{
		final List< String > columnNames = table.columnNames();
		spotIDColumnIndex = columnNames.indexOf( ColumnNames.SPOT_ID );
		xColumnIndex = columnNames.indexOf( ColumnNames.SPOT_X );
		yColumnIndex = columnNames.indexOf( ColumnNames.SPOT_Y );
		// FIXME figure out the column types here and
		//  declare final varibles for how to parse
		zColumnIndex = columnNames.indexOf( ColumnNames.SPOT_Z );
		timePointColumnIndex = columnNames.indexOf( ColumnNames.TIMEPOINT );
	}

	@Override
	public TableSawAnnotatedSpot create( Table table, int rowIndex )
	{
		final float[] position = new float[ 3 ];
		position[ 0 ] = (float) table.get( rowIndex, xColumnIndex );
		position[ 1 ] = (float) table.get( rowIndex, yColumnIndex );
		if ( zColumnIndex > -1 )
			position[ 2 ] =  (float) table.get( rowIndex, zColumnIndex ) + (float) ( 1e-3 * Math.random() ); // kdTree issue: https://imagesc.zulipchat.com/#narrow/stream/327240-ImgLib2/topic/kdTree.20issue

		int label = ( int ) table.get( rowIndex, spotIDColumnIndex );

		int timePoint = 0;
		if ( timePointColumnIndex > -1 )
			timePoint = ( int ) table.get( rowIndex, timePointColumnIndex );

		String source = table.name();

		// FIXME: the uuid occupies too much RAM for many spots
		// String uuid = source + ";" + timePoint + ";" + label;

		return new TableSawAnnotatedSpot( table, rowIndex, label, position, timePoint, source, null );
	}

	@Override
	public int[] removeColumns()
	{
		return new int[]{ xColumnIndex, yColumnIndex, zColumnIndex };
	}
}
