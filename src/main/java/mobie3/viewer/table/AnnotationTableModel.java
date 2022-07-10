package mobie3.viewer.table;

import net.imglib2.util.Pair;

import java.util.Collection;
import java.util.List;

public interface AnnotationTableModel< A extends Annotation >
{
	List< String > getColumnNames();
	Class< ? > getColumnClass( String columnName );
	int getNumRows();
	int getRowIndex( A annotation );
	A getRow( int rowIndex );
	void loadColumns( String columnsPath ); // load more chucks of columns
	void setColumnPaths( Collection< String> columnPaths );
	Collection< String > getColumnPaths(); // where to load more chucks of columns
	List< String > getLoadedColumnPaths(); // which column chunks have been loaded, in the order in which they have been loaded
	Pair< Double, Double > getMinMax( String columnName ); // for contrast limits during rendering
}
