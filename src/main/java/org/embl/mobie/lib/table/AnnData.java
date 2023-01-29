package org.embl.mobie.lib.table;

import org.embl.mobie.lib.annotation.Annotation;

public interface AnnData< A extends Annotation >
{
	AnnotationTableModel< A > getTable();
}
