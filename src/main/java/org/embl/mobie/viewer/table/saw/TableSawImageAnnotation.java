package org.embl.mobie.viewer.table.saw;

import net.imglib2.roi.RealMaskRealInterval;
import net.imglib2.util.Intervals;
import org.embl.mobie.viewer.ImageStore;
import org.embl.mobie.viewer.annotation.ImageAnnotation;
import org.embl.mobie.viewer.table.ColumnNames;
import org.embl.mobie.viewer.transform.TransformHelper;
import tech.tablesaw.api.Row;

import java.util.List;

public class TableSawImageAnnotation implements ImageAnnotation
{
	private Row row;
	private final List< String > annotatedImageNames;
	private RealMaskRealInterval realMaskRealInterval;
	private String regionId;
	private int label;
	private final int timePoint;

	public TableSawImageAnnotation(
			Row row,
			List< String > annotatedImageNames )
	{
		this.row = row;
		this.annotatedImageNames = annotatedImageNames;

		// ImageAnnotation properties
		this.regionId = row.getObject( ColumnNames.REGION_ID ).toString();
		this.label = regionId.hashCode();
		this.timePoint = row.columnNames().contains( ColumnNames.TIMEPOINT ) ? this.row.getInt( ColumnNames.TIMEPOINT ) : 0;
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
	public double[] anchor()
	{
		final double[] min = Intervals.minAsDoubleArray( realMaskRealInterval );
		final double[] max = Intervals.maxAsDoubleArray( realMaskRealInterval );
		final double[] center = new double[ min.length ];
		for ( int d = 0; d < min.length; d++ )
			center[ d ] = ( max[ d ] + min[ d ] ) / 2.0;
		return center;
	}

	@Override
	public String id()
	{
		return regionId;
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

	@Override
	public RealMaskRealInterval getMask()
	{
		if ( realMaskRealInterval == null )
			realMaskRealInterval = TransformHelper.getUnionMask( ImageStore.getImages( annotatedImageNames ), timePoint() );

		return realMaskRealInterval;
	}

	@Override
	public String regionId ( )
	{
		return regionId;
	}
}
