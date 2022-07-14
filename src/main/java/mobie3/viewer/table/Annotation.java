package mobie3.viewer.table;

public interface Annotation
{
	String getId();
	Object getValue( String columnName );
	void setString( String columnName, String value );
}
