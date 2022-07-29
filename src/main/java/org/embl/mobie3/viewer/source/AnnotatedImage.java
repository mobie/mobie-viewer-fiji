package org.embl.mobie3.viewer.source;

import org.embl.mobie3.viewer.table.AnnData;
import org.embl.mobie3.viewer.annotation.Annotation;

public interface AnnotatedImage< A extends Annotation > extends Image< AnnotationType< A > >
{
	AnnData< A > getAnnData();
}
