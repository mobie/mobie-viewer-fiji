package mobie3.viewer.source;

import mobie3.viewer.table.AnnotatedSegment;
import mobie3.viewer.table.SegmentsAnnData;
import net.imglib2.type.numeric.IntegerType;

public interface AnnotatedImage< T extends IntegerType< T >, SA extends AnnotatedSegment > extends Image< AnnotationType< SA > >
{
	Image< T > getLabelMask();
	SegmentsAnnData< SA > getAnnData();
}
