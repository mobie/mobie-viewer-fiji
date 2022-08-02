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
package org.embl.mobie.viewer.transform.image;

import bdv.tools.transformation.TransformedSource;
import bdv.util.VolatileSource;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import org.embl.mobie.viewer.MultiThreading;
import org.embl.mobie.viewer.source.StitchedImage;
import org.embl.mobie.viewer.source.Image;
import org.embl.mobie.viewer.source.MergedGridSource;
import org.embl.mobie.viewer.source.SourceHelper;
import org.embl.mobie.viewer.transform.AbstractGridTransformation;
import org.embl.mobie.viewer.transform.TransformedGridTransformation;
import net.imglib2.RealInterval;
import net.imglib2.converter.Converter;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class MergedGridTransformation< T extends NumericType< T > > extends AbstractGridTransformation< T >
{
	// Serialization
	protected List< String > sources;
	protected String mergedGridSourceName;
	protected boolean centerAtOrigin = false; // TODO: should actually be true, but: https://github.com/mobie/mobie-viewer-fiji/issues/685#issuecomment-1108179599
	protected boolean encodeSource = false; // true for the first time label images are encoded // TODO: (we can remove this now).

	// Runtime
	private transient Image< T > stitchedImage;

	@Override
	public Image< T > apply( List< Image< T > > images )
	{
		if ( stitchedImage == null )
		{
			if ( positions == null )
				autoSetPositions();

			stitchedImage = new StitchedImage<>( images, positions, mergedGridSourceName, TransformedGridTransformation.RELATIVE_CELL_MARGIN );
		}

		return stitchedImage;
	}

	@Override
	public List< String > getTargetImages()
	{
		return sources;
	}
}
