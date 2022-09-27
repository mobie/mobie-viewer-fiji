package org.embl.mobie.viewer.table.saw;

import org.embl.mobie.viewer.annotation.Annotation;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;

import java.util.function.Supplier;

public interface TableSawAnnotationCreator< A extends Annotation >
{
	A create( Supplier< Table > tableSupplier , int rowIndex );
}
