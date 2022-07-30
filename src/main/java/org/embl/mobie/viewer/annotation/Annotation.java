package org.embl.mobie.viewer.annotation;

public interface Annotation extends Location
{
	String id();
	int labelId();
	Object getValue( String columnName );
	void setString( String columnName, String value );
}
