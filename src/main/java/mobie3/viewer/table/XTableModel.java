package mobie3.viewer.table;

import net.imglib2.util.Pair;

import javax.swing.table.TableModel;
import java.util.List;

public interface XTableModel< A extends Row > extends TableModel
{
	void loadColumns( String columnsPath ); // load more chucks of columns
	List< String > getColumnPaths(); // where to load more chucks of columns
	List< String > getLoadedColumnPaths(); // which column chunks have been loaded
	Pair< Double, Double > getMinMax( String columnName ); // for contrast limits during rendering
}
