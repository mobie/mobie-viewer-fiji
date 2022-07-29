package org.embl.mobie3.viewer.table;

import org.embl.mobie3.viewer.annotation.Annotation;

public interface AnnData< A extends Annotation >
{
	AnnotationTableModel< A > getTable();
}
