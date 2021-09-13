package org.embl.mobie.viewer.source;

import bdv.viewer.Source;

public interface SourceWrapper< T >
{
	Source< T > getWrappedSource();
}
