package org.embl.mobie.lib.bvv;

import bdv.viewer.SourceAndConverter;
import mpicbg.spim.data.generic.AbstractSpimData;

public class BvvSourceStateMobie
{
	public final SourceAndConverter<?> sac;
	public final AbstractSpimData<?> spimData;
	public final boolean bVisible;

	public BvvSourceStateMobie (final SourceAndConverter<?> sac_,
						final AbstractSpimData<?> spimData_, final boolean bVisible_)
	{
		sac = sac_;
		spimData = spimData_;
		bVisible = bVisible_;
	}
}
