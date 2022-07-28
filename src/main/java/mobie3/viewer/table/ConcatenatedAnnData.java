package mobie3.viewer.table;

import de.embl.cba.tables.table.ConcatenatedTableModel;
import mobie3.viewer.annotation.Annotation;

import java.util.Set;
import java.util.stream.Collectors;

public class ConcatenatedAnnData< A extends Annotation > implements AnnData< A >
{
	public ConcatenatedAnnData( Set< AnnData< A > > annDataSet )
	{
		final Set< AnnotationTableModel< A > > tableModels = annDataSet.stream().map( a -> a.getTable() ).collect( Collectors.toSet() );

		new ConcatenatedAnnotationTableModel( tableModels )
		// TODO
		//   - it should not access the tables if not needed (only lazy)
		//   - numRows() should only return the number of currently available rows, this may need to be updated during loading
		//   -
	}

	@Override
	public AnnotationTableModel< A > getTable()
	{
		return null;
	}
}
