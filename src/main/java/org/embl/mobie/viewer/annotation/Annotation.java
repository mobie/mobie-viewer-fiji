package org.embl.mobie.viewer.annotation;

public interface Annotation extends Location
{
	String getId();
	Object getValue( String columnName );
	void setString( String columnName, String value );
}
