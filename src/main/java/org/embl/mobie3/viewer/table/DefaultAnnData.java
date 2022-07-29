package org.embl.mobie3.viewer.table;

import org.embl.mobie3.viewer.annotation.Annotation;

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
