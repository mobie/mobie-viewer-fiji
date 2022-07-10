package mobie3.viewer.segment;

import mobie3.viewer.table.SegmentAnnotation;
import mobie3.viewer.transform.AffineTransformation;
import mobie3.viewer.transform.Transformation;
import net.imglib2.RealInterval;

public class TransformedSegmentAnnotation implements SegmentAnnotation
{
	private final SegmentAnnotation segmentAnnotation;
	private final Transformation transformation;

	public TransformedSegmentAnnotation( SegmentAnnotation segmentAnnotation, Transformation transformation )
	{
		this.segmentAnnotation = segmentAnnotation;
		this.transformation = transformation;
	}

	@Override
	public String imageId()
	{
		return segmentAnnotation.imageId();
	}

	@Override
	public int labelId()
	{
		return segmentAnnotation.labelId();
	}

	@Override
	public int timePoint()
	{
		// could be transformed
		return segmentAnnotation.timePoint();
	}

	@Override
	public double[] getAnchor()
	{
		final double[] anchor = segmentAnnotation.getAnchor();

		if ( transformation instanceof AffineTransformation )
		{
			final double[] transformedAnchor = new double[ anchor.length ];
			( ( AffineTransformation ) transformation ).getAffineTransform3D().apply( anchor, transformedAnchor );
			return transformedAnchor;
		}
		else
		{
			return anchor;
		}
	}

	@Override
	public RealInterval boundingBox()
	{
		if ( transformation instanceof AffineTransformation )
		{
			return ( ( AffineTransformation ) transformation ).getAffineTransform3D().estimateBounds( segmentAnnotation.boundingBox() );
		}
		else
		{
			return segmentAnnotation.boundingBox();
		}
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
	public Object getValue( String columnName )
	{
		return segmentAnnotation.getValue( columnName );
	}
}
