package org.embl.mobie.viewer.image;

import org.embl.mobie.viewer.annotation.Annotation;
import org.embl.mobie.viewer.source.AnnotationType;
import org.embl.mobie.viewer.table.AnnData;

public interface AnnotatedImage< A extends Annotation > extends Image< AnnotationType< A > >
{
	AnnData< A > getAnnData();
}
