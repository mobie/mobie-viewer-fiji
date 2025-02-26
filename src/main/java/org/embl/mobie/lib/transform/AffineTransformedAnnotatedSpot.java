/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2024 EMBL
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

import net.imglib2.RealInterval;
import net.imglib2.realtransform.AffineTransform3D;
import org.embl.mobie.lib.annotation.AnnotatedSpot;
import org.embl.mobie.lib.volume.MeshTransformer;

// TODO: code duplication with AffineTransformedAnnotatedSegment
public class AffineTransformedAnnotatedSpot< AS extends AnnotatedSpot > implements AnnotatedSpot
{
	private final AS annotatedSpot;
	private final AffineTransform3D affineTransform3D;
	private double[] position; // the transformed position

	public AffineTransformedAnnotatedSpot( AS annotatedSpot, AffineTransform3D affineTransform3D )
	{
		this.annotatedSpot = annotatedSpot;
		this.affineTransform3D = affineTransform3D;
	}

	@Override
	public int label()
	{
		return annotatedSpot.label();
	}

	@Override
	public Integer timePoint()
	{
		// TODO could be transformed
		return annotatedSpot.timePoint();
	}

	@Override
	public double[] positionAsDoubleArray()
	{
		if ( position == null )
		{
			position = new double[ 3 ];
			affineTransform3D.apply( annotatedSpot.positionAsDoubleArray(), position );
		}

		return position;
	}

	@Override
	public double getDoublePosition( int d )
	{
		return positionAsDoubleArray()[ d ];
	}

	@Override
	public String uuid()
	{
		return annotatedSpot.uuid();
	}

	@Override
	public String source()
	{
		return annotatedSpot.source();
	}

	@Override
	public Object getValue( String feature )
	{
		return annotatedSpot.getValue( feature );
	}

	@Override
	public Double getNumber( String feature )
	{
		return annotatedSpot.getNumber( feature );
	}

	@Override
	public void setString( String columnName, String value )
	{
		annotatedSpot.setString( columnName, value );
	}

	@Override
	public void setNumber( String columnName, double value )
	{
		annotatedSpot.setNumber( columnName, value );
	}

	@Override
	public void transform( AffineTransform3D affineTransform3D )
	{
		if ( position != null )
			affineTransform3D.apply( position, position );

		this.affineTransform3D.preConcatenate( affineTransform3D );
	}

	@Override
	public int numDimensions()
	{
		return positionAsDoubleArray().length;
	}
}
