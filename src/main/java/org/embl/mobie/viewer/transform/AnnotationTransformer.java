package org.embl.mobie.viewer.transform;

import org.embl.mobie.viewer.annotation.Annotation;

public interface AnnotationTransformer< A extends Annotation, TA extends Annotation >
{
	TA transform( A annotation );
}
