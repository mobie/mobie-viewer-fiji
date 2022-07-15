package mobie3.viewer.transform;

import mobie3.viewer.annotation.Annotation;

public interface AnnotationTransformer< A extends Annotation, TA extends A >
{
	TA transform( A annotation );
}
