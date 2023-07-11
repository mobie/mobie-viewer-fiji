/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2023 EMBL
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.embl.mobie.lib.transform;

import net.imglib2.realtransform.AffineTransform3D;
import org.embl.mobie.lib.annotation.AnnotatedSegment;
import net.imglib2.RealInterval;
import org.embl.mobie.lib.volume.MeshTransformer;

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
	public Integer timePoint()
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
