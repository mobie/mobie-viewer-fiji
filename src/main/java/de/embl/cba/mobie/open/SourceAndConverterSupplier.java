package de.embl.cba.mobie.open;

import bdv.viewer.SourceAndConverter;

import java.util.List;

public interface SourceAndConverterSupplier
{
	List< SourceAndConverter< ? > > get( List< String > sourceNames );
}
