package org.embl.mobie.lib.image;

import net.imglib2.type.numeric.IntegerType;
import org.embl.mobie.lib.annotation.Annotation;

public interface AnnotatedLabelImage< A extends Annotation > extends AnnotationImage< A >
{
	// This image serves the locations of the annotations,
	// by corresponding to {@code annotation.label()}
	Image< ? extends IntegerType< ? > > getLabelImage();

	// FIXME: Add AnnotationAdapter?
}
