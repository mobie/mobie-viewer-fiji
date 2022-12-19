package org.embl.mobie.viewer.annotation;

import java.util.Set;

public interface AnnotationAdapter< A extends Annotation >
{
	A createVariable();

	// UUID for de-serialisation of selected segments
	// https://github.com/mobie/mobie-viewer-fiji/issues/827
	A getAnnotation( String uuid );

	// For mapping of voxels within an
	// {@code AnnotatedLabelSource}
	// to the corresponding annotation.
	A getAnnotation( String source, int timePoint, int label );

	Set< A > getAnnotations( Set< String > uuids );
}
