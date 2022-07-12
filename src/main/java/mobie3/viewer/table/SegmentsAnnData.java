package mobie3.viewer.table;

import mobie3.viewer.segment.TransformedAnnotatedSegment;
import mobie3.viewer.transform.Transformation;

public interface SegmentsAnnData< SR extends AnnotatedSegment > extends AnnData< SR >
{
	SegmentsAnnData< TransformedAnnotatedSegment > transform( Transformation transformer );
}
