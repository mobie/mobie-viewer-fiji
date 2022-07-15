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
package mobie3.viewer.bdv.view;

import bdv.viewer.SourceAndConverter;
import mobie3.viewer.MoBIE;
import mobie3.viewer.display.AbstractDisplay;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import java.util.HashMap;

public abstract class AbstractSliceView implements SliceView
{
	protected final MoBIE moBIE;
	protected final AbstractDisplay< ? > display;
	protected final SliceViewer sliceViewer;

	// TODO get rid of MoBIE here, which is only needed to close the sacs...
	public AbstractSliceView( MoBIE moBIE, AbstractDisplay< ? > display )
	{
		this.moBIE = moBIE;
		this.display = display;
		sliceViewer = display.sliceViewer;
		display.nameToSourceAndConverter = new HashMap<>();
	}

	@Override
	public void close( boolean closeImgLoader )
	{
		for ( SourceAndConverter< ? > sourceAndConverter : display.nameToSourceAndConverter.values() )
		{
			moBIE.closeSourceAndConverter( sourceAndConverter, closeImgLoader );
		}
		display.nameToSourceAndConverter.clear();

		sliceViewer.updateTimepointSlider();
	}

	@Override
	public SliceViewer getSliceViewer()
	{
		return sliceViewer;
	}

	@Override
	public boolean isVisible() {
		// check if first source is visible
		return SourceAndConverterServices.getBdvDisplayService().isVisible( display.nameToSourceAndConverter.values().iterator().next(), display.sliceViewer.getBdvHandle() );
	}
}
