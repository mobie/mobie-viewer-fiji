package mobie3.viewer.table;

import mobie3.viewer.annotation.Annotation;
import net.imglib2.util.Pair;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class ConcatenatedAnnotationTableModel< A extends Annotation > implements AnnotationTableModel< A >
{
	private final Set< AnnotationTableModel< A > > tableModels;
	private AnnotationTableModel< A > referenceTable;

	public ConcatenatedAnnotationTableModel( Set< AnnotationTableModel< A > > tableModels )
	{
		this.tableModels = tableModels;
		referenceTable = tableModels.iterator().next();
	}

	@Override
	public List< String > columnNames()
	{
		return referenceTable.columnNames();
	}

	@Override
	public List< String > numericColumnNames()
	{
		return referenceTable.numericColumnNames();
	}

	@Override
	public Class< ? > columnClass( String columnName )
	{
		return referenceTable.columnClass( columnName );
	}

	@Override
	public int numRows()
	{
		return 0;
	}

	@Override
	public int indexOf( A annotation )
	{
		return 0;
	}

	@Override
	public A row( int rowIndex )
	{
		return null;
	}

	@Override
	public void loadColumns( String columnsPath )
	{

	}

	@Override
	public void setColumnPaths( Collection< String > columnPaths )
	{

	}

	@Override
	public Collection< String > columnPaths()
	{
		return null;
	}

	@Override
	public LinkedHashSet< String > loadedColumnPaths()
	{
		return null;
	}

	@Override
	public Pair< Double, Double > computeMinMax( String columnName )
	{
		return null;
	}

	@Override
	public Set< A > rows()
	{
		return null;
	}

	@Override
	public void addStringColumn( String columnName )
	{

	}
}
