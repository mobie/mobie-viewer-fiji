package org.embl.mobie.viewer.bdv;

import bdv.util.BdvHandle;
import javafx.beans.value.ObservableDoubleValue;
import net.imglib2.FinalRealInterval;
import org.embl.mobie.viewer.display.SourceDisplay;
import sc.fiji.bdvpg.bdv.BdvHandleHelper;

import java.util.function.Function;

public class DisplayVisibilityChecker implements Function< SourceDisplay, Boolean >
{
	private final BdvHandle bdvHandle;

	public DisplayVisibilityChecker( BdvHandle bdvHandle )
	{
		this.bdvHandle = bdvHandle;
	}

	@Override
	public Boolean apply( SourceDisplay display )
	{
		final FinalRealInterval viewerInterval = BdvHandleHelper.getViewerGlobalBoundingInterval( bdvHandle );

		return null;
	}
}
