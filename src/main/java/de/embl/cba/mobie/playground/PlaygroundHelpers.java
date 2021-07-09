package de.embl.cba.mobie.playground;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import java.util.List;

public class PlaygroundHelpers
{
	public static BdvHandle getBdvHandle( List< SourceAndConverter< ? > > sourceAndConverters )
	{
		return SourceAndConverterServices.getBdvDisplayService().getDisplaysOf( sourceAndConverters.get( 0 ) ).iterator().next();
	}
}
