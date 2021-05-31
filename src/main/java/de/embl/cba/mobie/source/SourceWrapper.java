package de.embl.cba.mobie.source;

import bdv.viewer.Source;

public interface SourceWrapper< T >
{
	Source< T > getWrappedSource();
}
