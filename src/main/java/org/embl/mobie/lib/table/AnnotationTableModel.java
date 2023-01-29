package org.embl.mobie.lib.table;

import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.ValuePair;
import org.embl.mobie.lib.annotation.Annotation;
import net.imglib2.util.Pair;
import org.embl.mobie.lib.io.StorageLocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

public interface AnnotationTableModel< A extends Annotation > extends AnnotationListener< A >
{
	default ValuePair< Double, Double > getColumnMinMax( String columnName, ArrayList< A > annotations )
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
	void loadTableChunk( String tableChunk ); // load additional chunks from the table source
	void loadExternalTableChunk( StorageLocation location ); // load chunks from an external table source
	//void setAvailableTableChunks( Set< String> availableColumnPaths );
	Collection< String > getAvailableTableChunks();
	LinkedHashSet< String > getLoadedTableChunks(); // loaded chunks, in the order in which they have been loaded
	Pair< Double, Double > getMinMax( String columnName ); // for contrast limits during rendering
	ArrayList< A > annotations();
	void addStringColumn( String columnName );
	StorageLocation getStorageLocation();
	void transform( AffineTransform3D affineTransform3D );
	void addAnnotationListener(  AnnotationListener< A > listener );
}