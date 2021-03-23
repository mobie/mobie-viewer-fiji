package de.embl.cba.mobie2;

import bdv.viewer.SourceAndConverter;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.bdv.utils.sources.LazySpimSource;
import de.embl.cba.mobie.ui.SourcesDisplayManager;
import de.embl.cba.tables.color.ColorUtils;
import mpicbg.spim.data.SpimData;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterBdvDisplayService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;
import sc.fiji.bdvpg.sourceandconverter.display.ColorChanger;

import java.util.Map;

public class Viewer
{
	private final MoBIE moBIE;

	public Viewer( MoBIE moBIE )
	{
		this.moBIE = moBIE;
	}

	public void show( View view )
	{
		// Apply all sourceTransforms
		// ...

		// Show the sources
		for ( SourceDisplaySupplier displaySupplier : view.sourceDisplays )
		{
			final SourceDisplays sourceDisplays = displaySupplier.get();
			if ( sourceDisplays instanceof ImageDisplays )
			{
				viewImageDisplay( ( ImageDisplays ) sourceDisplays );
			}
		}
	}

	public void viewImageDisplay( ImageDisplays imageDisplays )
	{
		for ( String sourceName : imageDisplays.sources )
		{
			final ImageSource source = ( ImageSource ) moBIE.getSource( sourceName );

			final String imageLocation = source.imageDataLocations.get( moBIE.getImageDataLocation() );
			final SpimData spimData = BdvUtils.openSpimData( imageLocation );
			final SourceAndConverter sourceAndConverter = SourceAndConverterHelper.createSourceAndConverters( spimData ).get( 0 );

			new ColorChanger( sourceAndConverter, ColorUtils.getARGBType(  imageDisplays.color ) ).run();

			SourceAndConverterServices.getSourceAndConverterDisplayService().show( sourceAndConverter );
		}
	}

}
