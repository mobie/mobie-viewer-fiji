package de.embl.cba.mobie2.transform;

import bdv.viewer.SourceAndConverter;

import javax.xml.transform.Source;
import java.util.List;

public class AutoGridSourceTransformer implements SourceTransformer
{
	private String[][] sources;
	private String gridType;

	@Override
	public List< SourceAndConverter< ? > > transform( List< SourceAndConverter< ? > > sourceAndConverters )
	{
		return null;
	}
}
