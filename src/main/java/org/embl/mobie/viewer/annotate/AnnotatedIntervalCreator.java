package org.embl.mobie.viewer.annotate;

import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import org.embl.mobie.viewer.TableColumnNames;
import org.embl.mobie.viewer.transform.TransformHelpers;
import net.imglib2.RealInterval;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AnnotatedIntervalCreator
{
	private final Map< String, List< String > > columns;
	private final Map< String, List< String > > annotationIdToSources;
	private final Function< String, SourceAndConverter< ? > > sourceAndConverterSupplier;
	private List< AnnotatedIntervalTableRow > annotatedIntervalTableRows;

	public AnnotatedIntervalCreator( Map< String, List< String > > columns, Map< String, List< String > > annotationIdToSources, Function< String, SourceAndConverter< ? > > sourceAndConverterSupplier)
	{
		this.columns = columns;
		this.annotationIdToSources = annotationIdToSources;
		this.sourceAndConverterSupplier = sourceAndConverterSupplier;
		createAnnotatedIntervals();
	}

	private void createAnnotatedIntervals()
	{
		annotatedIntervalTableRows = new ArrayList<>();
		final Set< String > annotationIds = annotationIdToSources.keySet();
		final List< String > annotationIdColumn = columns.get( TableColumnNames.ANNOTATION_ID );

		for ( String annotationId : annotationIds )
		{
			final List< ? extends Source< ? > > sources = annotationIdToSources.get( annotationId ).stream().map( name -> sourceAndConverterSupplier.apply( name ).getSpimSource() ).collect( Collectors.toList() );
			final RealInterval realInterval = TransformHelpers.unionRealInterval( sources );

			final int rowIndex = annotationIdColumn.indexOf( annotationId );

			annotatedIntervalTableRows.add(
					new DefaultAnnotatedIntervalTableRow(
							annotationId,
							realInterval,
							columns,
							rowIndex )
			);
		}
	}

	public List< AnnotatedIntervalTableRow > getAnnotatedIntervalTableRows()
	{
		return annotatedIntervalTableRows;
	}
}
