package org.embl.mobie.lib.table.saw;

import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.roi.RealMaskRealInterval;
import net.imglib2.util.Intervals;
import org.embl.mobie.lib.DataStore;
import org.embl.mobie.lib.annotation.AnnotatedRegion;
import org.embl.mobie.lib.image.Image;
import org.embl.mobie.lib.image.ImageListener;
import org.embl.mobie.lib.table.ColumnNames;
import org.embl.mobie.lib.transform.TransformHelper;

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
	private Set< Image< ? > > images;

	// TODO also here it is misleading that the model is
	//  is given, one only needs something that can read the values from the correct table
	//  Maybe a new class is needed?
	//  Maybe simply the Annotation itself is given in the constructor?
	//  Rather than extending it?
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

		images = DataStore.getImageSet( imageNames );
		for ( Image< ? > image : images )
			image.listeners().add( this );
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
		if ( position == null )
		{
			final RealMaskRealInterval mask = getMask();
			final double[] min = Intervals.minAsDoubleArray( mask );
			final double[] max = Intervals.maxAsDoubleArray( mask );
			position = new double[ min.length ];
			for ( int d = 0; d < min.length; d++ )
				position[ d ] = ( max[ d ] + min[ d ] ) / 2.0;
		}

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
		// don't do anything here, because the annotated regions
		// provide all the spatial coordinates.
	}

	@Override
	public RealMaskRealInterval getMask()
	{
		if ( mask == null )
		{
			// Compute the mask of the images
			// that are annotated by this region
			mask = TransformHelper.getUnionMask( images, timePoint() );
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
		//System.out.println("Image changed: " + imageNames.get( 0 ) );
		mask = null; // force to compute the mask again
		position = null; // force to compute the position again
	}
}
