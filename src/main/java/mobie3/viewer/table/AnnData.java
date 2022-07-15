package mobie3.viewer.table;

import mobie3.viewer.annotation.Annotation;

public interface AnnData< R extends Annotation >
{
	AnnotationTableModel< R > getTable();
}
