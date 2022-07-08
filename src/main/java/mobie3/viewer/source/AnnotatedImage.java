package mobie3.viewer.source;

import mobie3.viewer.table.AnnData;
import mobie3.viewer.table.Row;

public interface AnnotatedImage< A extends Row > extends Image< AnnotationType< A > >, AnnData< A >
{
}
