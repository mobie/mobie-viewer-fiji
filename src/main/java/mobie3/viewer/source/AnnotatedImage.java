package mobie3.viewer.source;

import mobie3.viewer.table.SegmentRow;
import mobie3.viewer.table.SegmentsAnnData;
import net.imglib2.type.numeric.IntegerType;

public interface AnnotatedImage< T extends IntegerType< T >, SR extends SegmentRow > extends Image< AnnotationType< SR > >
{
	Image< T > getLabelMask();
	SegmentsAnnData< SR > getAnnData();
}
