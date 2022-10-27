package org.embl.mobie.viewer.annotation;

import net.imglib2.realtransform.AffineTransform3D;

public interface Annotation extends Location
{
	String uuid();

	// Data source (can be a table and/or a label mask image)
	String source();

	// Integer label for representing the annotation as a
	// region in one time point of a label image
	int label(); // FIXME we can probably get rid of this (see less labels)

	// For retrieving features (measurements)
	// (typically: feature = column in an annotation table)
	Object getValue( String feature );

	// For retrieving numerical features (measurements)
	// (typically: feature = column in an annotation table)
	Double getNumber( String feature );

	// For adding manual annotations
	void setString( String columnName, String value );

	// For merging tables.
	String[] idColumns();

	// Transform the spatial coordinates of this annotation.
	// Note that there are also methods to transform annotations,
	// which create a copy of the annotation;
	// e.g. {@code AffineTransformedAnnotatedSegment};
	// use those methods if you need both the transformed and
	// untransformed annotations.
	void transform( AffineTransform3D affineTransform3D );
}
