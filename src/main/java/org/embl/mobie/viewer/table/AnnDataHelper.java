package org.embl.mobie.viewer.table;

import org.embl.mobie.viewer.annotation.Annotation;
import org.embl.mobie.viewer.image.AnnotatedLabelImage;
import org.embl.mobie.viewer.image.AnnotationImage;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class AnnDataHelper
{
	public static < A extends Annotation > AnnData< A > concatenate( List< ? extends AnnotationImage< A > > annotationImages )
	{
		final Set< AnnData< A > > annDataSet = annotationImages.stream().map( image -> image.getAnnData() ).collect( Collectors.toSet() );

		if ( annDataSet.size() == 1 )
			return annDataSet.iterator().next();
		else
			return new ConcatenatedAnnData( annDataSet );
	}
}
