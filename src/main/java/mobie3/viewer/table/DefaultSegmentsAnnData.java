package mobie3.viewer.table;

import mobie3.viewer.segment.TransformedAnnotatedSegment;
import mobie3.viewer.transform.Transformation;

public class DefaultSegmentsAnnData< SR extends AnnotatedSegment > implements SegmentsAnnData< SR >
{
	private SegmentsTableModel< SR > tableModel;

	public DefaultSegmentsAnnData( SegmentsTableModel< SR > tableModel )
	{
		this.tableModel = tableModel;
	}

	@Override
	public AnnotationTableModel< SR > getTable()
	{
		return tableModel;
	}

	@Override
	public SegmentsAnnData< TransformedAnnotatedSegment > transform( Transformation transformation )
	{
		final TransformedSegmentsTableModel transformedModel = new TransformedSegmentsTableModel( tableModel, transformation );
		final DefaultSegmentsAnnData< TransformedAnnotatedSegment > segmentsAnnData = new DefaultSegmentsAnnData<>( transformedModel );
		return segmentsAnnData;
	}
}
