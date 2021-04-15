package de.embl.cba.mobie2.transform;

import bdv.viewer.SourceAndConverter;

import java.util.ArrayList;
import java.util.List;

public class TransformerHelper
{
	public static List< SourceAndConverter< ? > > transformSourceAndConverters( List< SourceAndConverter< ? > > sourceAndConverters, List< SourceTransformer > sourceTransformers )
	{
		List< SourceAndConverter< ? > > transformed = new ArrayList<>( sourceAndConverters );
		if ( sourceTransformers != null )
		{
			for ( SourceTransformer sourceTransformer : sourceTransformers )
			{
				transformed = sourceTransformer.transform( transformed );
			}
		}

		return transformed;
	}
}
