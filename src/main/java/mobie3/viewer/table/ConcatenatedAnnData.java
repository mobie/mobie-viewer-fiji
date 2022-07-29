package mobie3.viewer.table;

import de.embl.cba.tables.table.ConcatenatedTableModel;
import mobie3.viewer.annotation.Annotation;

import java.util.Set;
import java.util.stream.Collectors;

public class ConcatenatedAnnData< A extends Annotation > implements AnnData< A >
{
	private final ConcatenatedAnnotationTableModel< A > concatenatedAnnotationTableModel;

	public ConcatenatedAnnData( Set< AnnData< A > > annDataSet )
	{
		final Set< AnnotationTableModel< A > > tableModels = annDataSet.stream().map( a -> a.getTable() ).collect( Collectors.toSet() );
		this.concatenatedAnnotationTableModel = new ConcatenatedAnnotationTableModel( tableModels );
	}

	@Override
	public AnnotationTableModel< A > getTable()
	{
		return concatenatedAnnotationTableModel;
	}
}
