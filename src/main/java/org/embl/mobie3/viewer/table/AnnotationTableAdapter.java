package org.embl.mobie3.viewer.table;

import org.embl.mobie3.viewer.annotation.Annotation;

public interface AnnotationTableAdapter< A extends Annotation >
{
	int rowIndex( A annotation );
	A getAnnotation( int rowIndex );
}
