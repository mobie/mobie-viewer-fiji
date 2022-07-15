package mobie3.viewer.table;

import mobie3.viewer.annotation.AnnotatedSegment;
import mobie3.viewer.annotation.TransformedAnnotatedSegment;
import mobie3.viewer.transform.Transformation;

public interface SegmentsAnnData< AS extends AnnotatedSegment > extends AnnData< AS >
{
	SegmentsAnnData< TransformedAnnotatedSegment > transform( Transformation transformer );
}
