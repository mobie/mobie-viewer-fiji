package mobie3.viewer.source;

import mobie3.viewer.table.SegmentAnnotation;
import mobie3.viewer.table.SegmentsAnnData;
import net.imglib2.type.numeric.IntegerType;

public interface AnnotatedImage< T extends IntegerType< T >, SA extends SegmentAnnotation > extends Image< AnnotationType< SA > >
{
	Image< T > getLabelMask();
	SegmentsAnnData< SA > getAnnData();
}
