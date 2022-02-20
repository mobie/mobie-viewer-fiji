package org.embl.mobie.viewer.view;

import bdv.viewer.SourceAndConverter;

import java.util.Arrays;
import java.util.Collection;

public class ViewFromSourceAndConverterCreator
{
	private Collection< SourceAndConverter > sourceAndConverters;

	public ViewFromSourceAndConverterCreator( SourceAndConverter sourceAndConverter )
	{
		this( Arrays.asList( sourceAndConverter ) );
	}

	public ViewFromSourceAndConverterCreator( Collection< SourceAndConverter > sourceAndConverters )
	{
		this.sourceAndConverters = sourceAndConverters;
	}

	public View getView()
	{
		// TODO
		return null;
	}

}
