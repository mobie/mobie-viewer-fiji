package mobie3.viewer.transform;

import mobie3.viewer.annotation.AnnotatedSegment;
import mobie3.viewer.transform.image.AffineTransformation;
import net.imglib2.RealInterval;

public class TransformedAnnotatedSegment implements AnnotatedSegment
{
	private final AnnotatedSegment annotatedSegment;
	private final Transformation transformation;

	public TransformedAnnotatedSegment( AnnotatedSegment annotatedSegment, Transformation transformation )
	{
		this.annotatedSegment = annotatedSegment;
		this.transformation = transformation;
	}

	@Override
	public String imageId()
	{
		return annotatedSegment.imageId();
	}

	@Override
	public int labelId()
	{
		return annotatedSegment.labelId();
	}

	@Override
	public int timePoint()
	{
		// could be transformed
		return annotatedSegment.timePoint();
	}

	@Override
	public double[] anchor()
	{
		final double[] anchor = annotatedSegment.anchor();

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
			return ( ( AffineTransformation ) transformation ).getAffineTransform3D().estimateBounds( annotatedSegment.boundingBox() );
		}
		else
		{
			return annotatedSegment.boundingBox();
		}
	}

	@Override
	public void setBoundingBox( RealInterval boundingBox )
	{
		if ( transformation instanceof AffineTransformation )
		{
			annotatedSegment.setBoundingBox( ( ( AffineTransformation ) transformation ).getAffineTransform3D().inverse().estimateBounds( annotatedSegment.boundingBox() ) );
		}
		else
		{
			annotatedSegment.setBoundingBox( boundingBox );
		}

	}

	@Override
	public float[] mesh()
	{
		// TODO transform
		return annotatedSegment.mesh();
	}

	@Override
	public void setMesh( float[] mesh )
	{
		// TODO inverse transform
		annotatedSegment.setMesh( mesh );
	}

	@Override
	public String getId()
	{
		return annotatedSegment.getId();
	}

	@Override
	public Object getValue( String columnName )
	{
		return annotatedSegment.getValue( columnName );
	}

	@Override
	public void setString( String columnName, String value )
	{
		annotatedSegment.setString( columnName, value );
	}
}
