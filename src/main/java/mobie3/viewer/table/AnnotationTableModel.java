package mobie3.viewer.table;

import net.imglib2.util.Pair;

import java.awt.image.BufferedImageOp;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public interface AnnotationTableModel< A extends Annotation > extends Iterable< A >
{
	List< String > columnNames();
	Class< ? > columnClass( String columnName );
	int numRows();
	int getRowIndex( A annotation );
	A getRow( int rowIndex );
	void loadColumns( String columnsPath ); // load more chucks of columns
	void setColumnPaths( Collection< String> columnPaths );
	Collection< String > columnPaths(); // where to load more chucks of columns
	List< String > loadedColumnPaths(); // which column chunks have been loaded, in the order in which they have been loaded
	Pair< Double, Double > computeMinMax( String columnName ); // for contrast limits during rendering
	List< A > rows();
}
