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
