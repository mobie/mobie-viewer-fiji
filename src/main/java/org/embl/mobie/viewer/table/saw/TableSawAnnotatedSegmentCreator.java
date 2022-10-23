package org.embl.mobie.viewer.table.saw;

import net.imglib2.FinalRealInterval;
import org.embl.mobie.viewer.table.ColumnNames;
import tech.tablesaw.api.Table;

import java.util.List;

public class TableSawAnnotatedSegmentCreator implements TableSawAnnotationCreator< TableSawAnnotatedSegment >
{
	private int labelImageColumnIndex;
	private int labelIdColumnIndex;
	private int timePointColumnIndex;
	private int xColumnIndex;
	private int yColumnIndex;
	private int zColumnIndex;

	public TableSawAnnotatedSegmentCreator( )
	{
	}

	public TableSawAnnotatedSegmentCreator( Table table )
	{
		final List< String > columnNames = table.columnNames();
		labelIdColumnIndex =  columnNames.indexOf( ColumnNames.LABEL_ID );
		timePointColumnIndex = columnNames.indexOf( ColumnNames.TIMEPOINT );
		xColumnIndex = columnNames.indexOf( ColumnNames.ANCHOR_X );
		yColumnIndex = columnNames.indexOf( ColumnNames.ANCHOR_Y );
		zColumnIndex = columnNames.indexOf( ColumnNames.ANCHOR_Z );
		labelImageColumnIndex = columnNames.indexOf( ColumnNames.LABEL_IMAGE_ID );
		columnNames.indexOf( ColumnNames.BB_MIN_X );
	}

	@Override
	public TableSawAnnotatedSegment create( TableSawAnnotationTableModel< TableSawAnnotatedSegment > model, int rowIndex )
	{
		final Table table = model.getTable();

		final boolean is3D = zColumnIndex > -1;

		String source = labelImageColumnIndex > -1 ? table.stringColumn( labelIdColumnIndex ).get( rowIndex ) : table.name();

		int timePoint = timePointColumnIndex > -1 ? table.intColumn( timePointColumnIndex ).get( rowIndex ) : 0;

		Integer labelId = table.intColumn( labelIdColumnIndex ).get( rowIndex );

		final FinalRealInterval boundingBox = boundingBox( table, rowIndex, is3D );

		double [] position = new double[]{
				table.numberColumn( xColumnIndex ).getDouble( rowIndex ),
				table.numberColumn( yColumnIndex ).getDouble( rowIndex ),
				is3D ? table.numberColumn( zColumnIndex ).getDouble( rowIndex ) : 0
		};

		String uuid = source + ";" + timePoint + ";" + labelId;

		return new TableSawAnnotatedSegment( model, rowIndex, source, uuid, labelId, timePoint, position, boundingBox );
	}

	@Override
	public int[] removeColumns()
	{
		return new int[ 0 ];
	}

	private FinalRealInterval boundingBox( Table table, int rowIndex, boolean is3D )
	{
		// TODO add the column names as indices to the constructor
		final boolean rowContainsBoundingBox = table.columnNames().contains( ColumnNames.BB_MIN_X );

		if ( ! rowContainsBoundingBox ) return null;

		final double[] min = {
				table.numberColumn( ColumnNames.BB_MIN_X ).getDouble( rowIndex ),
				table.numberColumn( ColumnNames.BB_MIN_Y ).getDouble( rowIndex ),
				is3D ? table.numberColumn( ColumnNames.BB_MIN_Z ).getDouble( rowIndex ) : 0
		};

		final double[] max = {
				table.numberColumn( ColumnNames.BB_MAX_X ).getDouble( rowIndex ),
				table.numberColumn( ColumnNames.BB_MAX_Y ).getDouble( rowIndex ),
				is3D ? table.numberColumn( ColumnNames.BB_MAX_Z ).getDouble( rowIndex ) : 0
		};

		FinalRealInterval boundingBox = new FinalRealInterval( min, max );
		return boundingBox;
	}
}
