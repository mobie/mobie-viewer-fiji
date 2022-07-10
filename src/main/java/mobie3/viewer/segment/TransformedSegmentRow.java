package mobie3.viewer.segment;

import mobie3.viewer.table.SegmentRow;
import net.imglib2.RealInterval;
import net.imglib2.realtransform.AffineTransform3D;

import java.util.List;

public class TransformedSegmentRow implements SegmentRow
{
	private final SegmentRow segmentRow;
	private final AffineTransform3D transform;

	public TransformedSegmentRow( SegmentRow segmentRow, AffineTransform3D transform )
	{
		this.segmentRow = segmentRow;
		this.transform = transform;
	}

	@Override
	public String imageId()
	{
		return segmentRow.imageId();
	}

	@Override
	public int labelId()
	{
		return segmentRow.labelId();
	}

	@Override
	public int timePoint()
	{
		// could be transformed
		return segmentRow.timePoint();
	}

	@Override
	public double[] getAnchor()
	{
		final double[] anchor = segmentRow.getAnchor();
		final double[] transformedAnchor = new double[ anchor.length ];
		transform.apply( anchor, transformedAnchor );
		return transformedAnchor;
	}

	@Override
	public RealInterval boundingBox()
	{
		return transform.estimateBounds( segmentRow.boundingBox() );
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
		return segmentRow.getColumnNames();
	}

	@Override
	public Class< ? > getColumnClass( String columnName )
	{
		return segmentRow.getColumnClass( columnName );
	}

	@Override
	public Object getValue( String columnName )
	{
		return segmentRow.getValue( columnName );
	}
}
