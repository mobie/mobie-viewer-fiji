package org.embl.mobie.viewer.annotation;

public interface Annotation extends Location
{
	// UUID for serialisation of selected annotations
	String id();

	// integer label for representing the annotation as a
	// region in one time point of a label image
	int label();

	// for retrieving features (measurements)
	// (typically: feature = column in an annotation table)
	Object getValue( String feature );

	// for adding manual annotations
	void setString( String columnName, String value );
}
