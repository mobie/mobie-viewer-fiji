package org.embl.mobie.viewer.table.saw;

import net.imglib2.FinalRealInterval;
import org.embl.mobie.viewer.table.ColumnNames;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;

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
	}

	@Override
	public TableSawAnnotatedSegment create( Table table, int rowIndex )
	{
		final boolean is3D = zColumnIndex > -1;

		String source = labelImageColumnIndex > -1 ? table.stringColumn( labelIdColumnIndex ).get( rowIndex ) : table.name();

		int timePoint = timePointColumnIndex > -1 ? table.intColumn( timePointColumnIndex ).get( rowIndex ) : 0;

		this.labelId = row.getInt( ColumnNames.LABEL_ID );

		initBoundingBox( row, is3D );

		this.position = new double[]{
				row.getNumber( ColumnNames.ANCHOR_X ),
				row.getNumber( ColumnNames.ANCHOR_Y ),
				is3D ? row.getNumber( ColumnNames.ANCHOR_Z ) : 0
		};

		this.uuid = source + ";" + timePoint + ";" + labelId;

		return new TableSawAnnotatedSegment( table, rowIndex );
	}

	@Override
	public int[] removeColumns()
	{
		return new int[ 0 ];
	}

	private void initBoundingBox( Row row, boolean is3D )
	{
		final boolean rowContainsBoundingBox = row.columnNames().contains( ColumnNames.BB_MIN_X );

		if ( ! rowContainsBoundingBox ) return;

		final double[] min = {
				row.getNumber( ColumnNames.BB_MIN_X ),
				row.getNumber( ColumnNames.BB_MIN_Y ),
				is3D ? row.getNumber( ColumnNames.BB_MIN_Z ) : 0
		};

		final double[] max = {
				row.getNumber( ColumnNames.BB_MAX_X ),
				row.getNumber( ColumnNames.BB_MAX_Y ),
				is3D ? row.getNumber( ColumnNames.BB_MAX_Z ) : 0
		};

		boundingBox = new FinalRealInterval( min, max );
	}
}
