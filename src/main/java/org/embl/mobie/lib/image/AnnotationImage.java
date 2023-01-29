package org.embl.mobie.lib.image;

import org.embl.mobie.lib.annotation.Annotation;
import org.embl.mobie.lib.source.AnnotationType;
import org.embl.mobie.lib.table.AnnData;

// Image where each pixel value is a {@code Annotation},
// which are the rows of an {@code AnnData}.
public interface AnnotationImage< A extends Annotation > extends Image< AnnotationType< A > >
{
	AnnData< A > getAnnData();
}
