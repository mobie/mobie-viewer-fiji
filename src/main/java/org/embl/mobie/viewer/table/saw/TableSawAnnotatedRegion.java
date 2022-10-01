package org.embl.mobie.viewer.table.saw;

import net.imglib2.roi.RealMaskRealInterval;
import net.imglib2.util.Intervals;
import org.embl.mobie.viewer.DataStore;
import org.embl.mobie.viewer.annotation.AnnotatedRegion;
import org.embl.mobie.viewer.table.ColumnNames;
import org.embl.mobie.viewer.transform.TransformHelper;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;

import java.util.List;
import java.util.function.Supplier;

public class TableSawAnnotatedRegion implements AnnotatedRegion
{
	private static final String[] idColumns = new String[]{ ColumnNames.REGION_ID };
	private final Supplier< Table > tableSupplier;
	private final int rowIndex;

	private final List< String > imageNames;
	private final String uuid;
	private RealMaskRealInterval realMaskRealInterval;
	private String regionId;
	private int label;
	private final int timePoint;
	private String source;

	public TableSawAnnotatedRegion(
			Supplier< Table > tableSupplier,
			int rowIndex,
			List< String > imageNames )
	{
		this.tableSupplier = tableSupplier;
		this.rowIndex = rowIndex;
		this.imageNames = imageNames;

		final Row row = tableSupplier.get().row( rowIndex );

		this.regionId = row.getObject( ColumnNames.REGION_ID ).toString();
		// 0 is the background label, thus we add 1
		this.label = 1 + rowIndex; //regionId.hashCode();
		this.timePoint = row.columnNames().contains( ColumnNames.TIMEPOINT ) ? row.getInt( ColumnNames.TIMEPOINT ) : 0;
		this.source = tableSupplier.get().name();
		this.uuid = timePoint + ";" + regionId;
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
		final double[] min = Intervals.minAsDoubleArray( getMask() );
		final double[] max = Intervals.maxAsDoubleArray( getMask() );
		final double[] center = new double[ min.length ];
		for ( int d = 0; d < min.length; d++ )
			center[ d ] = ( max[ d ] + min[ d ] ) / 2.0;
		return center;
	}

	@Override
	public double getDoublePosition( int d )
	{
		return positionAsDoubleArray()[ d ];
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
	public RealMaskRealInterval getMask()
	{
		if ( realMaskRealInterval == null )
			realMaskRealInterval = TransformHelper.getUnionMask( DataStore.getViewImageSet( imageNames ), timePoint() );

		//System.out.println( regionId + ": " + realMaskRealInterval.toString() );
		return realMaskRealInterval;
	}

	@Override
	public String regionId()
	{
		return regionId;
	}

	@Override
	public int numDimensions()
	{
		return positionAsDoubleArray().length;
	}
}
