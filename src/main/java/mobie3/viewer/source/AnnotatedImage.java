package mobie3.viewer.source;

import mobie3.viewer.table.AnnData;
import mobie3.viewer.table.Row;
import net.imglib2.type.numeric.IntegerType;

public interface AnnotatedImage< T extends IntegerType< T >, A extends Row > extends Image< AnnotationType< A > >
{
	Image< T > getLabelMask();
	AnnData< A > getAnnData();
}
