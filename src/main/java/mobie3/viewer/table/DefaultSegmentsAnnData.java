package mobie3.viewer.table;

import mobie3.viewer.annotation.AnnotatedSegment;
import mobie3.viewer.annotation.TransformedAnnotatedSegment;
import mobie3.viewer.transform.Transformation;

public class DefaultSegmentsAnnData< SR extends AnnotatedSegment > implements SegmentsAnnData< SR >
{
	private AnnotatedSegmentTableModel< SR > tableModel;

	public DefaultSegmentsAnnData( AnnotatedSegmentTableModel< SR > tableModel )
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
		final TransformedAnnotatedSegmentTableModel transformedModel = new TransformedAnnotatedSegmentTableModel( tableModel, transformation );
		final DefaultSegmentsAnnData< TransformedAnnotatedSegment > segmentsAnnData = new DefaultSegmentsAnnData<>( transformedModel );
		return segmentsAnnData;
	}
}
