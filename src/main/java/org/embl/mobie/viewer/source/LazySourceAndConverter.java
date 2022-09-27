/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2022 EMBL
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
package org.embl.mobie.viewer.source;

import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.Volatile;
import net.imglib2.converter.Converter;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;
import org.embl.mobie.viewer.MoBIE;


/**
 * Needed functions by MergedGridSource without loading data:
 *
 * for reference source:
 * - getSourceTransform()
 * - rai.dimensionsAsLongArray() // could be provided by dedicated method in LazySource.
 *
 * in fact the other sources are not needed it seems...
 *
 * However, for transforming all the individual ones,
 * one needs the SpimSource and the Converters, which is crazy.
 * Can one do this another way? E.g. could the MergedGridSource
 * provide the positions of those sources?
 *
 * @param <T>
 */
public class LazySourceAndConverter< T extends NumericType< T > > extends SourceAndConverter< T >
{
	private final MoBIE moBIE;
	private String name;
	private final T type;
	private final double[] min;
	private final double[] max;
	private SourceAndConverter< T > sourceAndConverter;
	private LazySpimSource< T > lazySpimSource;
	private boolean isOpen = false;

	public LazySourceAndConverter( MoBIE moBIE, String name, AffineTransform3D sourceTransform, VoxelDimensions voxelDimensions, T type, double[] min, double[] max )
	{
		super( null, null );
		this.moBIE = moBIE;
		this.name = name;
		this.type = type;
		this.min = min;
		this.max = max;
		this.lazySpimSource = new LazySpimSource( this, name, sourceTransform, voxelDimensions, min, max );
	}

	@Override
	public Source< T > getSpimSource()
	{
		return lazySpimSource;
	}

	@Override
	public Converter< T, ARGBType > getConverter()
	{
		return getSourceAndConverter().getConverter();
	}

	@Override
	public SourceAndConverter< ? extends Volatile< T > > asVolatile()
	{
		return getSourceAndConverter().asVolatile();
	}

	public SourceAndConverter< T > getSourceAndConverter()
	{
		if ( sourceAndConverter == null )
		{
			sourceAndConverter = ( SourceAndConverter< T > ) moBIE.openSourceAndConverter( name, null );
			isOpen = true;
		}

		return sourceAndConverter;
	}

	public boolean isOpen()
	{
		return isOpen;
	}

	public void setName( String name )
	{
		this.name = name;
		lazySpimSource.setName( name );
	}

	public void setSourceTransform( AffineTransform3D sourceTransform )
	{
		lazySpimSource.setSourceTransform( sourceTransform );
	}
}
