package de.embl.cba.mobie.n5.zarr;

import bdv.util.BdvFunctions;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import de.embl.cba.mobie.n5.source.Sources;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.sequence.ViewSetup;
import net.imglib2.Dimensions;

import java.util.List;

public class OMEZarrViewer
{
	private final SpimData spimData;

	public OMEZarrViewer( SpimData spimData )
	{
		this.spimData = spimData;
	}

	public void show()
	{
		final List< ViewSetup > viewSetups = spimData.getSequenceDescription().getViewSetupsOrdered();
		// check whether all are 2D
		for ( ViewSetup viewSetup : viewSetups )
		{
			final Dimensions size = viewSetup.getSize();
			int a = 1;
		}

		List< BdvStackSource< ? > > sources = BdvFunctions.show( spimData );

		for ( int i = 0; i < viewSetups.size(); i++ )
		{
			final String name = viewSetups.get( i ).getChannel().getName();
			if ( name.contains( "labels" ) )
				Sources.showAsLabelMask( sources.get( i ) );
		}
	}
}
