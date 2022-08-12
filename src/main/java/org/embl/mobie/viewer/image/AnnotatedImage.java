package org.embl.mobie.viewer.image;

import org.embl.mobie.viewer.image.Image;
import org.embl.mobie.viewer.source.AnnotationType;
import org.embl.mobie.viewer.table.AnnData;
import org.embl.mobie.viewer.annotation.Annotation;

public interface AnnotatedImage< A extends Annotation > extends Image< AnnotationType< A > >
{
	AnnData< A > getAnnData();
}
