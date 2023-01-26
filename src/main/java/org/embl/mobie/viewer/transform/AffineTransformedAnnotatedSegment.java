package org.embl.mobie.viewer.transform;

import ij.IJ;
import net.imglib2.realtransform.AffineTransform3D;
import org.embl.mobie.viewer.annotation.AnnotatedSegment;
import net.imglib2.RealInterval;
import org.embl.mobie.viewer.volume.MeshTransformer;

public class AffineTransformedAnnotatedSegment< AS extends AnnotatedSegment > implements AnnotatedSegment
{
	private final AS annotatedSegment;
	private final AffineTransform3D affineTransform3D;
	private float[] mesh; // the transformed mesh
	private RealInterval boundingBox; // the transformed bb
	private double[] position; // the transformed position

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
		// TODO could be transformed
		return annotatedSegment.timePoint();
	}

	@Override
	public double[] positionAsDoubleArray()
	{
		if ( position == null )
		{
			position = new double[ 3 ];
			affineTransform3D.apply( annotatedSegment.positionAsDoubleArray(), position );
		}

		return position;
	}

	@Override
	public double getDoublePosition( int d )
	{
		return positionAsDoubleArray()[ d ];
	}

	@Override
	public RealInterval boundingBox()
	{
		if ( boundingBox == null )
			boundingBox = affineTransform3D.estimateBounds( annotatedSegment.boundingBox() );

		return boundingBox;
	}

	@Override
	public void setBoundingBox( RealInterval boundingBox )
	{
		this.boundingBox = boundingBox;
	}

	@Override
	public float[] mesh()
	{
		if ( mesh == null )
			mesh = MeshTransformer.transform( annotatedSegment.mesh(), affineTransform3D );

		return mesh;
	}

	@Override
	public void setMesh( float[] mesh )
	{
		this.mesh = mesh;
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
		if ( position != null )
			affineTransform3D.apply( position, position );

		if ( boundingBox != null )
			boundingBox = affineTransform3D.estimateBounds( boundingBox );

		if ( mesh != null )
			mesh = MeshTransformer.transform( mesh, affineTransform3D );

		this.affineTransform3D.preConcatenate( affineTransform3D );
	}

	@Override
	public int numDimensions()
	{
		return positionAsDoubleArray().length;
	}
}
