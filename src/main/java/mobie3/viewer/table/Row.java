package mobie3.viewer.table;

import java.util.List;

// Notes:
// - Maybe we don't need this but could just use
// the Row class from TableSaw directly.
// - If we use TableSaw internally we need an adapter for the columnTypes
public interface Row
{
	List< String > getColumnNames();
	Class< ? > getColumnClass( String columnName );
	Object getValue( String columnName );
}
