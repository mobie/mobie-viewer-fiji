package org.embl.mobie.viewer.table;

import org.embl.mobie.viewer.annotation.Annotation;
import org.embl.mobie.viewer.source.AnnotatedImage;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class AnnDataHelper
{
	public static < A extends Annotation >  AnnData< A > getConcatenatedAnnData( List< AnnotatedImage< A > > annotatedImages )
	{
		final Set< AnnData< A > > annDataSet = annotatedImages.stream().map( image -> image.getAnnData() ).collect( Collectors.toSet() );

		if ( annDataSet.size() == 1 )
			return annDataSet.iterator().next();
		else
			return new ConcatenatedAnnData( annDataSet );
	}
}
