package de.embl.cba.mobie.annotate;

import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import de.embl.cba.mobie.transform.TransformHelper;
import net.imglib2.RealInterval;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AnnotatedIntervalCreator
{
	private final Map< String, List< String > > columns;
	private final Map< String, List< String > > annotationIdToSources;
	private final Function< String, SourceAndConverter< ? > > sourceAndConverterSupplier;
	private List< AnnotatedIntervalTableRow > tableRows;

	public AnnotatedIntervalCreator( Map< String, List< String > > columns, Map< String, List< String > > annotationIdToSources, Function< String, SourceAndConverter< ? > > sourceAndConverterSupplier)
	{
		this.columns = columns;
		this.annotationIdToSources = annotationIdToSources;
		this.sourceAndConverterSupplier = sourceAndConverterSupplier;
		create();
	}

	private void create()
	{
		tableRows = new ArrayList<>();
		final int numRows = columns.values().iterator().next().size();
		final List< String > annotationIds = columns.get( "source_annotation_id" );

		for ( int rowIndex = 0; rowIndex < numRows; rowIndex++ )
		{
			final String annotationId = annotationIds.get( rowIndex );
			final List< ? extends Source< ? > > sources = annotationIdToSources.get( annotationId ).stream().map( name -> sourceAndConverterSupplier.apply( name ).getSpimSource() ).collect( Collectors.toList() );
			final RealInterval realInterval = TransformHelper.unionRealInterval( sources );

			tableRows.add(
					new DefaultAnnotatedIntervalTableRow(
							annotationId,
							realInterval,
							columns,
							rowIndex )
			);
		}
	}

	public List< AnnotatedIntervalTableRow > getTableRows()
	{
		return tableRows;
	}
}
