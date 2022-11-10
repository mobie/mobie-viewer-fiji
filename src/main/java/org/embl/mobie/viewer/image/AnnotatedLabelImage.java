package org.embl.mobie.viewer.image;

import net.imglib2.type.numeric.IntegerType;
import org.embl.mobie.viewer.annotation.Annotation;
import org.embl.mobie.viewer.source.AnnotationType;
import org.embl.mobie.viewer.table.AnnData;

public interface AnnotatedLabelImage< A extends Annotation > extends AnnotationImage< A >
{
	// This image serves the locations of the annotations,
	// by corresponding to {@code annotation.label()}
	Image< ? extends IntegerType< ? > > getLabelImage();

	// FIXME: Add AnnotationAdapter?
}
