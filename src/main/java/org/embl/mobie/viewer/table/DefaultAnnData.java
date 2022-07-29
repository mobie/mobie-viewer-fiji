package org.embl.mobie.viewer.table;

import org.embl.mobie.viewer.annotation.Annotation;

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
