package org.embl.mobie.viewer.table;

import org.embl.mobie.viewer.annotation.Annotation;

public interface AnnotationTableAdapter< A extends Annotation >
{
	int rowIndex( A annotation );
	A getAnnotation( int rowIndex );
}
