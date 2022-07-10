package mobie3.viewer.table;

import mobie3.viewer.segment.TransformedSegmentRow;
import mobie3.viewer.transform.Transformation;

public interface SegmentsAnnData< SR extends SegmentRow > extends AnnData< SR >
{
	SegmentsAnnData< TransformedSegmentRow > transform( Transformation transformer );
}
