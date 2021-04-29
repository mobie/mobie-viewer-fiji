package de.embl.cba.mobie2.open;

import bdv.viewer.SourceAndConverter;

import java.util.List;

public interface SourceAndConverterSupplier
{
	List< SourceAndConverter< ? > > get( List< String > sourceNames );
}
