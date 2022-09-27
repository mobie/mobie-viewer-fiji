package org.embl.mobie.viewer.table;

import org.embl.mobie.viewer.annotation.Annotation;
import net.imglib2.util.Pair;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

// MAYBE AbstractAnnotationTableModel?
public interface AnnotationTableModel< A extends Annotation >
{
	List< String > columnNames();
	List< String > numericColumnNames();
	Class< ? > columnClass( String columnName );
	int numAnnotations(); // TODO: avoid this as it could hamper lazy loading..?! rather use rows() below? Or maybe add back the iterator?
	int rowIndexOf( A annotation );
	A annotation( int rowIndex );
	void requestColumns( String columnsPath ); // load more chucks of columns
	void setAvailableColumnPaths( Set< String> availableColumnPaths );
	Collection< String > availableColumnPaths(); // where to load more chucks of columns
	LinkedHashSet< String > loadedColumnPaths(); // which column chunks have been loaded, in the order in which they have been loaded
	Pair< Double, Double > computeMinMax( String columnName ); // for contrast limits during rendering
	Set< A > annotations();
	void addStringColumn( String columnName );
	boolean isDataLoaded();
	String dataStore();
}
