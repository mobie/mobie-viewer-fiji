package org.embl.mobie.viewer.annotation;

import java.util.Set;
import java.util.stream.Collectors;

public interface AnnotationProvider< A >
{
	A getAnnotation( String annotationId );
	A createVariable();
	default Set< A > getAnnotations( Set< String > annotationIds )
	{
		return annotationIds.stream().map( id -> getAnnotation( id ) ).collect( Collectors.toSet() );
	}
}
