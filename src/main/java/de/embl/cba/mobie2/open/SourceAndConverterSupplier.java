package de.embl.cba.mobie2.open;

import bdv.viewer.SourceAndConverter;

public interface SourceAndConverterSupplier
{
	SourceAndConverter< ? > get( String sourceName );
}
