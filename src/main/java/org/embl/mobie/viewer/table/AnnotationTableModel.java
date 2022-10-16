package org.embl.mobie.viewer.table;

import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.ValuePair;
import org.embl.mobie.viewer.annotation.Annotation;
import net.imglib2.util.Pair;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public interface AnnotationTableModel< A extends Annotation >
{
	default ValuePair< Double, Double > getColumnMinMax( String columnName, Set< A > annotations )
	{
		double min = Double.MAX_VALUE;
		double max = -min;
		for ( A annotation : annotations )
		{
			final Double number = annotation.getNumber( columnName );

			if ( number > max )
				max = number;

			if ( number < min )
				min = number;
		}

		return new ValuePair<>( min, max );
	}

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
	Pair< Double, Double > getMinMax( String columnName ); // for contrast limits during rendering
	Set< A > annotations();
	void addStringColumn( String columnName );
	boolean isDataLoaded();
	String dataStore();
	void transform( AffineTransform3D affineTransform3D );
}
