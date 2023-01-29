package org.embl.mobie.lib.table;

import org.embl.mobie.lib.annotation.Annotation;

public class DefaultAnnData< A extends Annotation > implements AnnData< A >
{
	private AnnotationTableModel< A > tableModel;

	public DefaultAnnData( AnnotationTableModel< A > tableModel )
	{
		this.tableModel = tableModel;
	}

	@Override
	public AnnotationTableModel< A > getTable()
	{
		return tableModel;
	}
}
