package mobie3.viewer.source;

import mobie3.viewer.table.AnnData;
import mobie3.viewer.annotation.Annotation;
import net.imglib2.type.numeric.IntegerType;

public interface AnnotatedImage< A extends Annotation > extends Image< AnnotationType< A > >
{
	AnnData< A > getAnnData();
}
