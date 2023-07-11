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
package org.embl.mobie.lib.bdv;

import bdv.util.BdvHandle;
import bdv.util.PlaceHolderSource;
import bdv.viewer.SourceAndConverter;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class SourcesAtMousePositionSupplier implements Supplier< Collection< SourceAndConverter< ? > > >
{
	BdvHandle bdvHandle;
	boolean is2D;

	public SourcesAtMousePositionSupplier( BdvHandle bdvHandle, boolean is2D )
	{
		this.bdvHandle = bdvHandle;
		this.is2D = is2D;
	}

	@Override
	public Collection< SourceAndConverter< ? > > get()
	{
		final GlobalMousePositionProvider positionProvider = new GlobalMousePositionProvider( bdvHandle );

		final List< SourceAndConverter< ? > > sourceAndConverters = SourceAndConverterServices.getBdvDisplayService().getSourceAndConverterOf( bdvHandle )
				.stream()
				.filter( sac -> ! ( sac.getSpimSource() instanceof PlaceHolderSource ) )
				.filter( sac -> SourceAndConverterHelper.isPositionWithinSourceInterval( sac, positionProvider.getPositionAsRealPoint(), positionProvider.getTimePoint(), is2D ) )
				.filter( sac -> SourceAndConverterServices.getBdvDisplayService().isVisible( sac, bdvHandle ) )
				.collect( Collectors.toList() );

		return sourceAndConverters;
	}
}
