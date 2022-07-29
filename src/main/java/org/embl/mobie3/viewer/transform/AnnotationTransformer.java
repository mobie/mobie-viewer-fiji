package org.embl.mobie3.viewer.transform;

import org.embl.mobie3.viewer.annotation.Annotation;

public interface AnnotationTransformer< A extends Annotation, TA extends Annotation >
{
	TA transform( A annotation );
}
