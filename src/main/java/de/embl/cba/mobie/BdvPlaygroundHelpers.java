package de.embl.cba.mobie;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import java.util.List;

public class BdvPlaygroundHelpers
{
	public static BdvHandle getBdvHandle( List< SourceAndConverter< ? > > sourceAndConverters )
	{
		return SourceAndConverterServices.getSourceAndConverterDisplayService().getDisplaysOf( sourceAndConverters.get( 0 ) ).iterator().next();
	}
}
