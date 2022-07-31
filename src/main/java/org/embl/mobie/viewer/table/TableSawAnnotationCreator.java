package org.embl.mobie.viewer.table;

import org.embl.mobie.viewer.annotation.Annotation;
import tech.tablesaw.api.Table;

public interface TableSawAnnotationCreator< A extends Annotation >
{
	A create( Table table, int rowIndex );
}
