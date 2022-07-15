package mobie3.viewer.source;

import mobie3.viewer.table.AnnData;
import mobie3.viewer.table.AnnotatedSegment;
import mobie3.viewer.table.Annotation;
import mobie3.viewer.table.SegmentsAnnData;
import net.imglib2.type.numeric.IntegerType;

public interface AnnotatedImage< A extends Annotation > extends Image< AnnotationType< A > >
{
	Image< ? extends IntegerType< ? > > getLabelMask();
	AnnData< A > getAnnData();
}
