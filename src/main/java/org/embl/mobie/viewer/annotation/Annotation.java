package org.embl.mobie.viewer.annotation;

public interface Annotation extends Location
{
	String uuid();

	// Data source (can be a table and/or a label mask image)
	String source();

	// Integer label for representing the annotation as a
	// region in one time point of a label image
	int label();

	// For retrieving features (measurements)
	// (typically: feature = column in an annotation table)
	Object getValue( String feature );

	// For adding manual annotations
	void setString( String columnName, String value );

	// for merging tables
	String[] idColumns();
}
