package org.embl.mobie.viewer.transform;

import net.imglib2.realtransform.AffineTransform3D;
import org.embl.mobie.viewer.annotation.AnnotatedSegment;
import net.imglib2.RealInterval;

public class AffineTransformedAnnotatedSegment< AS extends AnnotatedSegment > implements AnnotatedSegment
{
	private final AS annotatedSegment;
	private final AffineTransform3D affineTransform3D;

	public AffineTransformedAnnotatedSegment( AS annotatedSegment, AffineTransform3D affineTransform3D )
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
		// TODO: cache this?! this would assume the that wrapped
		//   segment is not changing its position.
		final double[] position = annotatedSegment.positionAsDoubleArray();
		final double[] transformedPosition = new double[ 3 ];
		affineTransform3D.apply( position, transformedPosition );
		return transformedPosition;
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
		// FIXME: this should probably just set the bounding box!
		throw new RuntimeException();

		//annotatedSegment.setBoundingBox( affineTransform3D.inverse().estimateBounds( annotatedSegment.boundingBox() ) );
	}

	@Override
	public float[] mesh()
	{
		// FIXME transform
		return annotatedSegment.mesh();
	}

	@Override
	public void setMesh( float[] mesh )
	{
		// FIXME
		//   store the mesh locally, don't
		//
		//
		//   modify the annotatedSegment!

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
	public Double getNumber( String feature )
	{
		return annotatedSegment.getNumber( feature );
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
	public void transform( AffineTransform3D affineTransform3D )
	{
		this.affineTransform3D.preConcatenate( affineTransform3D );
	}

	@Override
	public int numDimensions()
	{
		return positionAsDoubleArray().length;
	}
}
