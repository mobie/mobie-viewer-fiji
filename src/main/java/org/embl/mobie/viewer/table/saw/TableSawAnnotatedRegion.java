package org.embl.mobie.viewer.table.saw;

import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.roi.RealMaskRealInterval;
import net.imglib2.util.Intervals;
import org.embl.mobie.viewer.DataStore;
import org.embl.mobie.viewer.annotation.AnnotatedRegion;
import org.embl.mobie.viewer.image.Image;
import org.embl.mobie.viewer.table.ColumnNames;
import org.embl.mobie.viewer.transform.TransformHelper;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;

import java.util.List;
import java.util.Set;
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
	private double[] position;
	private String source;
	private AffineTransform3D affineTransform3D;

	public TableSawAnnotatedRegion(
			Supplier< Table > tableSupplier,
			int rowIndex,
			List< String > imageNames )
	{
		this.tableSupplier = tableSupplier;
		this.rowIndex = rowIndex;
		this.imageNames = imageNames;
		this.affineTransform3D = new AffineTransform3D();

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
	public synchronized double[] positionAsDoubleArray()
	{
		//if ( position == null )
		//{
		// Update the position every time, because the underlying
		// images that are annotated by this region may have changed
		// their position
		final RealMaskRealInterval mask = getMask();
		final double[] min = Intervals.minAsDoubleArray( mask );
		final double[] max = Intervals.maxAsDoubleArray( mask );
		position = new double[ min.length ];
		for ( int d = 0; d < min.length; d++ )
			position[ d ] = ( max[ d ] + min[ d ] ) / 2.0;
		//	affineTransform3D.apply( position, position );
		//}

		return position;
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
	public Double getNumber( String feature )
	{
		return tableSupplier.get().row( rowIndex ).getNumber( feature );
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
	public void transform( AffineTransform3D affineTransform3D )
	{
		// We don't do anything here, because the annotated regions
		// provide all the spatial coordinates

		//this.affineTransform3D.preConcatenate( affineTransform3D );
		///this.affineTransform3D.apply( position, position );
		//realMaskRealInterval = realMaskRealInterval.transform( this.affineTransform3D );
	}

	@Override
	public RealMaskRealInterval getMask()
	{
		// Update every time, because the position of the images
		// maybe have changed.
		final Set< Image< ? > > regionImages = DataStore.getViewImageSet( imageNames );
		final RealMaskRealInterval unionMask = TransformHelper.getUnionMask( regionImages, timePoint() );
		return unionMask;
	}

	@Override
	public void setMask( RealMaskRealInterval mask )
	{
		throw new RuntimeException();
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

	public List< String > getRegionImageNames()
	{
		return imageNames;
	}
}
