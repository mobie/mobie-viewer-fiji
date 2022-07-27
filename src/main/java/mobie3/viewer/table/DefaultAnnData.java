package mobie3.viewer.table;

import mobie3.viewer.annotation.Annotation;
import mobie3.viewer.transform.AnnotatedSegmentTransformer;
import mobie3.viewer.transform.TransformedAnnotatedSegment;

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
