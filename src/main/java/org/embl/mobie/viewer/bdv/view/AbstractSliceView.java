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
package org.embl.mobie.viewer.bdv.view;

import bdv.viewer.SourceAndConverter;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.display.AbstractSourceDisplay;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import java.util.Collection;
import java.util.HashMap;

public abstract class AbstractSliceView implements SliceView
{
	protected final MoBIE moBIE;
	protected final AbstractSourceDisplay display;
	protected final SliceViewer sliceViewer;

	public AbstractSliceView( MoBIE moBIE, AbstractSourceDisplay display )
	{
		this.moBIE = moBIE;
		this.display = display;
		sliceViewer = display.sliceViewer;
		display.sourceNameToSourceAndConverter = new HashMap<>();
	}

	@Override
	public void close( boolean closeImgLoader )
	{
		for ( SourceAndConverter< ? > sourceAndConverter : display.sourceNameToSourceAndConverter.values() )
		{
			moBIE.closeSourceAndConverter( sourceAndConverter, closeImgLoader );
		}
		display.sourceNameToSourceAndConverter.clear();

		sliceViewer.updateTimepointSlider();
	}

	@Override
	public SliceViewer getSliceViewer()
	{
		return sliceViewer;
	}

	@Override
	public boolean isVisible() {
		Collection<SourceAndConverter<?>> sourceAndConverters = display.sourceNameToSourceAndConverter.values();
		// check if first source is visible
		return SourceAndConverterServices.getBdvDisplayService().isVisible( sourceAndConverters.iterator().next(), display.sliceViewer.getBdvHandle() );
	}
}
