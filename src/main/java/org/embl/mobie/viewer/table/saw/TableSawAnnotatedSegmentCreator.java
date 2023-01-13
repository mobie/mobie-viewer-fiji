package org.embl.mobie.viewer.table.saw;

import net.imglib2.FinalRealInterval;
import org.embl.mobie.viewer.table.TableDataFormat;
import org.embl.mobie.viewer.table.columns.SegmentColumnNames;
import tech.tablesaw.api.Table;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class TableSawAnnotatedSegmentCreator implements TableSawAnnotationCreator< TableSawAnnotatedSegment >
{
	private SegmentColumnNames segmentColumnNames;
	private int labelImageColumnIndex;
	private int labelIdColumnIndex;
	private int timePointColumnIndex;
	private int[] anchorColumnIndices;
	private int[] bbMinColumnIndices;
	private int[] bbMaxColumnIndices;

	private AtomicBoolean columnsInitialised = new AtomicBoolean( false );
	private boolean is3D;
	private boolean hasBoundingBox;

	public TableSawAnnotatedSegmentCreator(
			@Nullable SegmentColumnNames segmentColumnNames,
			@Nullable Table table )
	{
		this.segmentColumnNames = segmentColumnNames;
		if ( table != null )
			initColumns( table );
	}

	private synchronized void initColumns( Table table )
	{
		columnsInitialised.set( true );

		final List< String > columnNames = table.columnNames();

		if ( segmentColumnNames == null )
			segmentColumnNames = TableDataFormat.getSegmentColumnNames( columnNames );

		labelIdColumnIndex = columnNames.indexOf( segmentColumnNames.labelIdColumn() );
		if ( labelIdColumnIndex == -1 )
			throw new RuntimeException("The label id column \"" + segmentColumnNames.labelIdColumn() + "\" could not be found in table " + table.name() );

		timePointColumnIndex = columnNames.indexOf( segmentColumnNames.timePointColumn() );

		anchorColumnIndices = Arrays.stream( segmentColumnNames.anchorColumns() )
				.mapToInt( name -> columnNames.indexOf( name ) )
				.toArray();

		bbMinColumnIndices = Arrays.stream( segmentColumnNames.bbMinColumns() )
				.mapToInt( name -> columnNames.indexOf( name ) )
				.toArray();

		bbMaxColumnIndices = Arrays.stream( segmentColumnNames.bbMaxColumns() )
				.mapToInt( name -> columnNames.indexOf( name ) )
				.toArray();

		labelImageColumnIndex = columnNames.indexOf( segmentColumnNames.labelImageColumn() );

		is3D = anchorColumnIndices.length == 3 && anchorColumnIndices[ 2 ] > -1;

		hasBoundingBox = bbMinColumnIndices[ 0 ] > -1;

	}

	@Override
	public TableSawAnnotatedSegment create( TableSawAnnotationTableModel< TableSawAnnotatedSegment > model, int rowIndex )
	{
		final Table table = model.getTable();

		if ( ! columnsInitialised.get() )
			initColumns( table );

		Integer labelId = table.intColumn( labelIdColumnIndex ).get( rowIndex );

		String source = labelImageColumnIndex > -1 ? table.stringColumn( labelImageColumnIndex ).get( rowIndex ) : table.name();

		int timePoint = timePointColumnIndex > -1 ? table.intColumn( timePointColumnIndex ).get( rowIndex ) : 0;


		final FinalRealInterval boundingBox = boundingBox( table, rowIndex );

		double[] position = new double[]{
				table.numberColumn( anchorColumnIndices[ 0 ] ).getDouble( rowIndex ),
				table.numberColumn( anchorColumnIndices[ 1 ] ).getDouble( rowIndex ),
				is3D ? table.numberColumn( anchorColumnIndices[ 2 ] ).getDouble( rowIndex ) : 0
		};

		final String uuid = source + ";" + timePoint + ";" + labelId;

		return new TableSawAnnotatedSegment( model, rowIndex, source, uuid, labelId, timePoint, position, boundingBox );
	}

	@Override
	public int[] removeColumns()
	{
		return new int[ 0 ];
	}

	private FinalRealInterval boundingBox( Table table, int rowIndex )
	{
		if ( ! hasBoundingBox ) return null;

		final double[] min = {
				table.numberColumn( bbMinColumnIndices[ 0 ] ).getDouble( rowIndex ),
				table.numberColumn( bbMinColumnIndices[ 1 ] ).getDouble( rowIndex ),
				is3D ? table.numberColumn( bbMinColumnIndices[ 2 ] ).getDouble( rowIndex ) : 0
		};

		final double[] max = {
				table.numberColumn( bbMaxColumnIndices[ 0 ] ).getDouble( rowIndex ),
				table.numberColumn( bbMaxColumnIndices[ 1 ] ).getDouble( rowIndex ),
				is3D ? table.numberColumn( bbMaxColumnIndices[ 2 ] ).getDouble( rowIndex ) : 0
		};

		final FinalRealInterval boundingBox = new FinalRealInterval( min, max );

		return boundingBox;
	}
}
