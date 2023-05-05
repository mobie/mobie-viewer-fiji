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
package org.embl.mobie.lib.source;

import bdv.util.RealRandomAccessibleSource;
import mpicbg.spim.data.sequence.DefaultVoxelDimensions;
import mpicbg.spim.data.sequence.FinalVoxelDimensions;
import net.imglib2.Interval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.Type;

import java.util.Set;

public class RealRandomAccessibleIntervalTimelapseSource< T extends Type< T > > extends RealRandomAccessibleSource< T >
{
	private final Interval interval;

	private final AffineTransform3D sourceTransform;
	private final Set< Integer > timePoints;

	public RealRandomAccessibleIntervalTimelapseSource(
			final RealRandomAccessible< T > accessible,
			final Interval interval,
			final T type,
			final AffineTransform3D sourceTransform,
			final String name,
			final boolean doBoundingBoxIntersectionCheck,
			final Set< Integer > timePoints )
	{
		super( accessible, type, name, new FinalVoxelDimensions( "", 1, 1, 1 ), doBoundingBoxIntersectionCheck );
		this.interval = interval;
		this.sourceTransform = sourceTransform;
		this.timePoints = timePoints;
	}

	@Override
	public synchronized void getSourceTransform( final int t, final int level, final AffineTransform3D transform )
	{
		transform.set( sourceTransform );
	}

	@Override
	public Interval getInterval( final int t, final int level )
	{
		return interval;
	}

	@Override
	public boolean isPresent( final int t )
	{
		if ( timePoints == null )
			return t == 0;

		return timePoints.contains( t );
	}
}
