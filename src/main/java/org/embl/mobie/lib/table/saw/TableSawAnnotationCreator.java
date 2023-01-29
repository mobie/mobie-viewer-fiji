package org.embl.mobie.lib.table.saw;

import org.embl.mobie.lib.annotation.Annotation;


public interface TableSawAnnotationCreator< A extends Annotation >
{
	A create( TableSawAnnotationTableModel< A > tableModel, int rowIndex );

	int[] removeColumns();
}
