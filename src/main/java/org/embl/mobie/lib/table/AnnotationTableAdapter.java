package org.embl.mobie.lib.table;

import org.embl.mobie.lib.annotation.Annotation;

public interface AnnotationTableAdapter< A extends Annotation >
{
	int rowIndex( A annotation );
	A getAnnotation( int rowIndex );
}
