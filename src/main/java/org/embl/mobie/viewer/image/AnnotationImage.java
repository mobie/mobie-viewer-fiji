package org.embl.mobie.viewer.image;

import org.embl.mobie.viewer.annotation.Annotation;
import org.embl.mobie.viewer.source.AnnotationType;
import org.embl.mobie.viewer.table.AnnData;

// Image where each pixel value is a {@code Annotation},
// which are the rows of an {@code AnnData}.
public interface AnnotationImage< A extends Annotation > extends Image< AnnotationType< A > >
{
	AnnData< A > getAnnData();
}
