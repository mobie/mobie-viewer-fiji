package org.embl.mobie.viewer.transform;

import org.embl.mobie.viewer.annotation.SegmentAnnotation;
import org.embl.mobie.viewer.transform.image.AffineTransformation;
import net.imglib2.RealInterval;
import org.embl.mobie.viewer.transform.image.Transformation;

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
	public int label()
	{
		return segmentAnnotation.label();
	}

	@Override
	public int timePoint()
	{
		// could be transformed
		return segmentAnnotation.timePoint();
	}

	@Override
	public double[] anchor()
	{
		final double[] anchor = segmentAnnotation.anchor();

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
		if ( transformation instanceof AffineTransformation )
		{
			segmentAnnotation.setBoundingBox( ( ( AffineTransformation ) transformation ).getAffineTransform3D().inverse().estimateBounds( segmentAnnotation.boundingBox() ) );
		}
		else
		{
			segmentAnnotation.setBoundingBox( boundingBox );
		}

	}

	@Override
	public float[] mesh()
	{
		// TODO transform
		return segmentAnnotation.mesh();
	}

	@Override
	public void setMesh( float[] mesh )
	{
		// TODO inverse transform
		segmentAnnotation.setMesh( mesh );
	}

	@Override
	public String id()
	{
		return segmentAnnotation.id();
	}

	@Override
	public Object getValue( String feature )
	{
		return segmentAnnotation.getValue( feature );
	}

	@Override
	public void setString( String columnName, String value )
	{
		segmentAnnotation.setString( columnName, value );
	}
}
