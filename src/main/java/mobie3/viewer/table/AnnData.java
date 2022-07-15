package mobie3.viewer.table;

import mobie3.viewer.annotation.Annotation;
import mobie3.viewer.transform.AnnotatedSegmentTransformer;
import mobie3.viewer.transform.Transformation;

public interface AnnData< A extends Annotation >
{
	AnnotationTableModel< A > getTable();
	AnnData< ? extends A > transform( AnnotatedSegmentTransformer annotatedSegmentTransformer );
}
