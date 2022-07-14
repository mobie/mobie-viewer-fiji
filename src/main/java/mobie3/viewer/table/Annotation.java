package mobie3.viewer.table;

public interface Annotation
{
	Object getValue( String columnName );
	void setString( String columnName, String value );
}
