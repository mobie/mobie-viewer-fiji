package org.embl.mobie.viewer.transform;

import net.imglib2.realtransform.AffineTransform3D;
import org.embl.mobie.viewer.annotation.SegmentAnnotation;
import org.embl.mobie.viewer.transform.image.AffineTransformation;
import net.imglib2.RealInterval;

public class AffineTransformedSegmentAnnotation implements SegmentAnnotation
{
	private final SegmentAnnotation segmentAnnotation;
	private final AffineTransform3D affineTransform3D;

	public AffineTransformedSegmentAnnotation( SegmentAnnotation segmentAnnotation, AffineTransform3D affineTransform3D )
	{
		this.segmentAnnotation = segmentAnnotation;
		this.affineTransform3D = affineTransform3D;
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
		final double[] transformedAnchor = new double[ anchor.length ];
		affineTransform3D.apply( anchor, transformedAnchor );
		return transformedAnchor;
	}

	@Override
	public RealInterval boundingBox()
	{
		return affineTransform3D.estimateBounds( segmentAnnotation.boundingBox() );
	}

	@Override
	public void setBoundingBox( RealInterval boundingBox )
	{
		segmentAnnotation.setBoundingBox( affineTransform3D.inverse().estimateBounds( segmentAnnotation.boundingBox() ) );
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
