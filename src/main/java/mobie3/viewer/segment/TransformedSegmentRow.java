package mobie3.viewer.segment;

import mobie3.viewer.table.SegmentRow;
import net.imglib2.RealInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.RealViews;

import java.util.List;

public class TransformedSegmentRow implements SegmentRow
{
	private final SegmentRow segment;
	private final AffineTransform3D transform;

	public < S extends SegmentRow > TransformedSegmentRow( SegmentRow segmentRow, AffineTransform3D transform )
	{
		this.segment = segmentRow;
		this.transform = transform;
	}

	@Override
	public String imageId()
	{
		return segment.imageId();
	}

	@Override
	public int labelId()
	{
		return segment.labelId();
	}

	@Override
	public int timePoint()
	{
		return segment.timePoint();
	}

	@Override
	public double[] getAnchor()
	{
		final double[] anchor = segment.getAnchor();
		final double[] transformedAnchor = new double[ anchor.length ];
		transform.apply( anchor, transformedAnchor );
		return transformedAnchor;
	}

	@Override
	public RealInterval boundingBox()
	{
		return transform.estimateBounds( segment.boundingBox() );
	}

	@Override
	public void setBoundingBox( RealInterval boundingBox )
	{

	}

	@Override
	public float[] mesh()
	{
		// transform
		return new float[ 0 ];
	}

	@Override
	public void setMesh( float[] mesh )
	{

	}

	@Override
	public List< String > getColumnNames()
	{
		return null;
	}

	@Override
	public Class< ? > getColumnClass( String columnName )
	{
		return null;
	}

	@Override
	public Object getValue( String columnName )
	{
		return null;
	}
}
