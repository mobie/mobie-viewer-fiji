package org.embl.mobie.viewer.transform;

import net.imglib2.realtransform.AffineTransform3D;
import org.embl.mobie.viewer.annotation.AnnotatedSegment;
import net.imglib2.RealInterval;

public class AffineTransformedAnnotatedSegment implements AnnotatedSegment
{
	private final AnnotatedSegment annotatedSegment;
	private final AffineTransform3D affineTransform3D;

	public AffineTransformedAnnotatedSegment( AnnotatedSegment annotatedSegment, AffineTransform3D affineTransform3D )
	{
		this.annotatedSegment = annotatedSegment;
		this.affineTransform3D = affineTransform3D;
	}

	@Override
	public String imageId()
	{
		return annotatedSegment.imageId();
	}

	@Override
	public int label()
	{
		return annotatedSegment.label();
	}

	@Override
	public int timePoint()
	{
		// could be transformed
		return annotatedSegment.timePoint();
	}

	@Override
	public double[] positionAsDoubleArray()
	{
		final double[] anchor = annotatedSegment.positionAsDoubleArray();
		final double[] transformedAnchor = new double[ anchor.length ];
		affineTransform3D.apply( anchor, transformedAnchor );
		return transformedAnchor;
	}

	@Override
	public double getDoublePosition( int d )
	{
		return positionAsDoubleArray()[ d ];
	}

	@Override
	public RealInterval boundingBox()
	{
		return affineTransform3D.estimateBounds( annotatedSegment.boundingBox() );
	}

	@Override
	public void setBoundingBox( RealInterval boundingBox )
	{
		annotatedSegment.setBoundingBox( affineTransform3D.inverse().estimateBounds( annotatedSegment.boundingBox() ) );
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
	public String uuid()
	{
		return annotatedSegment.uuid();
	}

	@Override
	public String source()
	{
		return annotatedSegment.source();
	}

	@Override
	public Object getValue( String feature )
	{
		return annotatedSegment.getValue( feature );
	}

	@Override
	public void setString( String columnName, String value )
	{
		annotatedSegment.setString( columnName, value );
	}

	@Override
	public String[] idColumns()
	{
		return annotatedSegment.idColumns();
	}

	@Override
	public int numDimensions()
	{
		return positionAsDoubleArray().length;
	}
}
