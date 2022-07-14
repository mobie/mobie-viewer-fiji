package mobie3.viewer.table;

import net.imglib2.util.Pair;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public interface AnnotationTableModel< A extends Annotation >
{
	List< String > columnNames();
	Class< ? > columnClass( String columnName );
	int numRows();
	int rowIndex( A annotation );
	A row( int rowIndex );
	void loadColumns( String columnsPath ); // load more chucks of columns
	void setColumnPaths( Collection< String> columnPaths );
	Collection< String > columnPaths(); // where to load more chucks of columns
	LinkedHashSet< String > loadedColumnPaths(); // which column chunks have been loaded, in the order in which they have been loaded
	Pair< Double, Double > computeMinMax( String columnName ); // for contrast limits during rendering
	Set< A > rows();
}
