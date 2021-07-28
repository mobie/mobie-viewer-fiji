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
	private List< AnnotatedIntervalTableRow > annotatedIntervalTableRows;

	/**
	 * For each annotation Id create one TableRow.
	 *
	 * @param columns
	 * 					Columns containing the values for the table rows. The columns may contain more rows than annotation ids. Only the rows that are matching one of the annotation ids are used.
	 * @param annotationIdToSourceNames
	 * @param sourceAndConverterSupplier
	 */
	public AnnotatedIntervalCreator( Map< String, List< String > > columns, Map< String, List< String > > annotationIdToSourceNames, Function< String, SourceAndConverter< ? > > sourceAndConverterSupplier)
	{
		annotatedIntervalTableRows = new ArrayList<>();
		final List< String > annotationIdColumn = columns.get( "annotation_id" );

		for ( String annotationId : annotationIdToSourceNames.keySet() )
		{
			final List< ? extends Source< ? > > sources = annotationIdToSourceNames.get( annotationId ).stream().map( name -> sourceAndConverterSupplier.apply( name ).getSpimSource() ).collect( Collectors.toList() );
			final RealInterval realInterval = TransformHelper.unionRealInterval( sources );

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
