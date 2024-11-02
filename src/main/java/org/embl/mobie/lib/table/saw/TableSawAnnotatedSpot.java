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
package org.embl.mobie.lib.table.saw;

import net.imglib2.realtransform.AffineTransform3D;
import org.embl.mobie.lib.annotation.AnnotatedSpot;
import org.embl.mobie.lib.table.columns.ColumnNames;

public class TableSawAnnotatedSpot extends AbstractTableSawAnnotation implements AnnotatedSpot
{
	private static final String[] idColumns = new String[]{ ColumnNames.SPOT_ID };
	private final int labelId;
	private final int timePoint;
	private final String source;
	private float[] position; // may change due to transformations

	public TableSawAnnotatedSpot(
			final TableSawAnnotationTableModel< TableSawAnnotatedSpot > model,
			final int rowIndex,
			final int labelId,
			final float[] position,
			final int timePoint,
			final String source )
	{
		super( model, rowIndex );
		this.labelId = labelId;
		this.position = position;
		this.timePoint = timePoint;
		this.source = source;
	}

	@Override
	public int label()
	{
		return labelId;
	}

	@Override
	public Integer timePoint()
	{
		return timePoint;
	}

	@Override
	public double[] positionAsDoubleArray()
	{
		// Create new array (don't cache), because the {@code position}
		// is subject to change by means of a transformation
		final double[] doublePosition = new double[ position.length ];
		for ( int d = 0; d < position.length; d++ )
			doublePosition[ d ] = position[ d ];
		return doublePosition;
	}

	@Override
	public double getDoublePosition( int d )
	{
		return position[ d ];
	}

	@Override
	public float getFloatPosition( int d )
	{
		return position[ d ];
	}

	@Override
	public String uuid()
	{
		return source + ";" + timePoint + ";" + labelId;
	}

	@Override
	public String source()
	{
		return source;
	}

	@Override
	public void transform( AffineTransform3D affineTransform3D )
	{
		affineTransform3D.apply( position, position );
	}

	@Override
	public int numDimensions()
	{
		return position.length;
	}
}
