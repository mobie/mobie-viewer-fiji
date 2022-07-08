package mobie3.viewer.table;

import net.imglib2.util.Pair;

import java.util.List;

public interface AnnotationTableModel< R extends Row >
{
	int getRowIndex( R row );
	R getRow( int rowIndex );
	void loadColumns( String columnsPath ); // load more chucks of columns
	List< String > getColumnPaths(); // where to load more chucks of columns
	List< String > getLoadedColumnPaths(); // which column chunks have been loaded
	Pair< Double, Double > getMinMax( String columnName ); // for contrast limits during rendering
}
