package mobie3.viewer.table;

import mobie3.viewer.segment.TransformedSegmentAnnotation;
import mobie3.viewer.transform.Transformation;

public interface SegmentsAnnData< SR extends SegmentAnnotation > extends AnnData< SR >
{
	SegmentsAnnData< TransformedSegmentAnnotation > transform( Transformation transformer );
}
