package org.embl.mobie.viewer.table.saw;

import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.roi.RealMaskRealInterval;
import net.imglib2.util.Intervals;
import org.embl.mobie.viewer.DataStore;
import org.embl.mobie.viewer.annotation.AnnotatedRegion;
import org.embl.mobie.viewer.image.Image;
import org.embl.mobie.viewer.image.ImageListener;
import org.embl.mobie.viewer.table.ColumnNames;
import org.embl.mobie.viewer.transform.TransformHelper;

import java.util.List;
import java.util.Set;

public class TableSawAnnotatedRegion extends AbstractTableSawAnnotation implements AnnotatedRegion, ImageListener
{
	private static final String[] idColumns = new String[]{ ColumnNames.REGION_ID };

	private final List< String > imageNames;
	private final String uuid;
	private String regionId;
	private final int labelId;
	private final int timePoint;
	private double[] position;
	private String source;
	private RealMaskRealInterval mask;

	public TableSawAnnotatedRegion(
			TableSawAnnotationTableModel< TableSawAnnotatedRegion > model,
			int rowIndex,
			List< String > imageNames,
			int timePoint,
			String regionId,
			int labelId,
			String uuid )
	{
		super( model, rowIndex );
		this.source = model.getDataSourceName();
		this.imageNames = imageNames;
		this.regionId = regionId;
		this.timePoint = timePoint;
		this.labelId = labelId;
		this.uuid = uuid;
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
	public String[] idColumns()
	{
		return idColumns;
	}

	@Override
	public void transform( AffineTransform3D affineTransform3D )
	{
		// We don't do anything here, because the annotated regions
		// provide all the spatial coordinates.
	}

	@Override
	public RealMaskRealInterval getMask()
	{
		if ( mask == null )
		{
			final Set< Image< ? > > regionImages = DataStore.getImageSet( imageNames );

			for ( Image< ? > regionImage : regionImages )
				regionImage.listeners().add( this );

			mask = TransformHelper.getUnionMask( regionImages, timePoint() );
		}

		return mask;
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

	@Override
	public void imageChanged()
	{
		mask = null; // force to compute the mask again
	}
}
