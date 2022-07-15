package mobie3.viewer.table;

import mobie3.viewer.annotation.Annotation;

public interface AnnData< A extends Annotation >
{
	AnnotationTableModel< A > getTable();
}
