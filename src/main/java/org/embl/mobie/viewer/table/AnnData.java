package org.embl.mobie.viewer.table;

import org.embl.mobie.viewer.annotation.Annotation;

public interface AnnData< A extends Annotation >
{
	AnnotationTableModel< A > getTable();
}
