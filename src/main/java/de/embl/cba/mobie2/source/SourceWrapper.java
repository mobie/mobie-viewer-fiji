package de.embl.cba.mobie2.source;

import bdv.viewer.Source;

public interface SourceWrapper< T >
{
	Source< T > getWrappedSource();
}
