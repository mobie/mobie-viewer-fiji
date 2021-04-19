package de.embl.cba.mobie2.grid;

import de.embl.cba.mobie2.transform.GridSourceTransformer;
import net.imglib2.Interval;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AnnotatedIntervalCreator
{
	private final Map< String, List< String > > columns;
	private final GridSourceTransformer sourceTransformer;
	private List< DefaultAnnotatedIntervalTableRow > tableRows;

	public AnnotatedIntervalCreator( Map< String, List< String > > columns, GridSourceTransformer sourceTransformer )
	{
		this.columns = columns;
		this.sourceTransformer = sourceTransformer;
		create();
	}

	private void create()
	{
		tableRows = new ArrayList<>();
		final int numRows = columns.values().iterator().next().size();
		final List< Interval > intervals = sourceTransformer.getIntervals();
		for ( int row = 0; row < numRows; row++ )
		{
			intervals.get( row );
			tableRows.add(
					new DefaultAnnotatedIntervalTableRow(
							"grid position " + row,
							intervals.get( row ),
							columns,
							row )
			);
		}
	}

	public List< DefaultAnnotatedIntervalTableRow > getTableRows()
	{
		return tableRows;
	}
}
