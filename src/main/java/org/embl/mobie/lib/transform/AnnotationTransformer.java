package org.embl.mobie.lib.transform;

import org.embl.mobie.lib.annotation.Annotation;

public interface AnnotationTransformer< A extends Annotation, TA extends Annotation >
{
	TA transform( A annotation );
}
