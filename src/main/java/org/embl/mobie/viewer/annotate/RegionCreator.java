package org.embl.mobie.viewer.annotate;

import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import ij.IJ;
import net.imglib2.roi.RealMaskRealInterval;
import org.embl.mobie.viewer.MoBIEHelper;
import org.embl.mobie.viewer.TableColumnNames;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class RegionCreator
{
	private final Map< String, List< String > > columns;
	private final Map< String, List< String > > annotationIdToSources;
	private final Function< String, SourceAndConverter< ? > > sourceAndConverterSupplier;
	private List< RegionTableRow > regionTableRows;

	public RegionCreator( Map< String, List< String > > columns, Map< String, List< String > > annotationIdToSources, Function< String, SourceAndConverter< ? > > sourceAndConverterSupplier)
	{
		this.columns = columns;
		this.annotationIdToSources = annotationIdToSources;
		this.sourceAndConverterSupplier = sourceAndConverterSupplier;
		createAnnotatedMasks();
	}

	private void createAnnotatedMasks()
	{
		final long currentTimeMillis = System.currentTimeMillis();

		regionTableRows = new ArrayList<>();
		final Set< String > annotationIds = annotationIdToSources.keySet();
		final List< String > annotationIdColumn = columns.get( TableColumnNames.REGION_ID );

		for ( String annotationId : annotationIds )
		{
			final ArrayList< Source< ? > > sources = getSources( annotationId );

			final RealMaskRealInterval mask = MoBIEHelper.unionRealMask( sources );
			//System.out.println( annotationId );
			//System.out.println( sources.size() );
			//System.out.println( Arrays.toString( mask.minAsDoubleArray() ));

			regionTableRows.add(
					new DefaultRegionTableRow(
							annotationId,
							mask,
							columns,
							annotationIdColumn.indexOf( annotationId ) )
			);
		}

		final long durationMillis = System.currentTimeMillis() - currentTimeMillis;
		if ( durationMillis > 100 )
			IJ.log("Created " + annotationIds.size() + " annotated intervals in " + durationMillis + " ms.");
	}

	private ArrayList< Source< ? > > getSources( String annotationId )
	{
		final ArrayList< Source< ? > > sources = new ArrayList<>();
		final List< String > sourceNames = annotationIdToSources.get( annotationId );
		for ( String sourceName : sourceNames )
		{
			try
			{
				final Source< ? > source = sourceAndConverterSupplier.apply( sourceName ).getSpimSource();
				sources.add( source );
			} catch ( Exception e )
			{
				System.err.println( "Could not find " + sourceName + " among the image sources that are associated to this project.\nPlease check the project's dataset.json file to see whether it may be missing. ");
				e.printStackTrace();
				throw e;
			}
		}
		return sources;
	}

	public List< RegionTableRow > getRegionTableRows()
	{
		return regionTableRows;
	}
}
