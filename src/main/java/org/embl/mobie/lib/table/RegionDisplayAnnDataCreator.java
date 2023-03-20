package org.embl.mobie.lib.table;

import org.embl.mobie.MoBIE;
import org.embl.mobie.lib.DataStore;
import org.embl.mobie.lib.MoBIEHelper;
import org.embl.mobie.lib.annotation.AnnotatedRegion;
import org.embl.mobie.lib.io.StorageLocation;
import org.embl.mobie.lib.serialize.RegionDataSource;
import org.embl.mobie.lib.serialize.display.RegionDisplay;
import org.embl.mobie.lib.table.saw.TableSawAnnotatedRegion;
import org.embl.mobie.lib.table.saw.TableSawAnnotatedRegionCreator;
import org.embl.mobie.lib.table.saw.TableSawAnnotationCreator;
import org.embl.mobie.lib.table.saw.TableSawAnnotationTableModel;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegionDisplayAnnDataCreator
{
	private final MoBIE moBIE;
	private final RegionDisplay< ? > regionDisplay;

	private Table table;
	private StorageLocation tableLocation;
	private TableDataFormat tableFormat;

	public RegionDisplayAnnDataCreator( MoBIE moBIE, RegionDisplay< ? > regionDisplay )
	{
		this.moBIE = moBIE; // TODO it would feel cleaner not to have to pass moBIE here
		this.regionDisplay = regionDisplay;
	}

	public AnnData< AnnotatedRegion > getAnnData()
	{
		// TODO: it may have the table already!
		if (  regionDisplay.tableSource == null )
		{
			createTable();
		}
		else
		{
			loadTable();
		}

		final TableSawAnnotationCreator< TableSawAnnotatedRegion > annotationCreator = new TableSawAnnotatedRegionCreator( table, regionDisplay.sources );

		final TableSawAnnotationTableModel< AnnotatedRegion > tableModel = new TableSawAnnotationTableModel( regionDisplay.getName(), annotationCreator, tableLocation, tableFormat, table );

		final DefaultAnnData< AnnotatedRegion > annData = new DefaultAnnData<>( tableModel );

		return annData;
	}

	private void loadTable()
	{
		final RegionDataSource regionDataSource = ( RegionDataSource ) DataStore.getRawData( regionDisplay.tableSource );
		table = regionDataSource.table;
		tableLocation = moBIE.getTableLocation( regionDataSource.tableData );
		tableFormat = moBIE.getTableDataFormat( regionDataSource.tableData );

		// only keep the subset of rows (regions)
		// that are actually referred to in regionIdToImageNames
		final Set< String > regionIDs = regionDisplay.sources.keySet();
		final ArrayList< Integer > dropRows = new ArrayList<>();
		final int rowCount = table.rowCount();
		for ( int rowIndex = 0; rowIndex < rowCount; rowIndex++ )
		{
			final String regionId = table.row( rowIndex ).getObject( ColumnNames.REGION_ID ).toString();
			if ( !regionIDs.contains( regionId ) )
				dropRows.add( rowIndex );
		}

		if ( dropRows.size() > 0 )
			table = table.dropRows( dropRows.stream().mapToInt( i -> i ).toArray() );
	}

	private void createTable( )
	{
		tableLocation = new StorageLocation();
		tableFormat = TableDataFormat.Table;
		table = Table.create( regionDisplay.getName() );

		// copy the regions into a list to
		// fix the order
		final List< String > regions = new ArrayList<>( regionDisplay.sources.keySet() );

		table.addColumns( StringColumn.create( ColumnNames.REGION_ID, regions ) );

		if ( regionDisplay.getSourceNamesRegex() != null )
		{
			addSourceNameParsingColumns( regions );
		}
	}

	private void addSourceNameParsingColumns( List< String > regionNames )
	{
		final Pattern pattern = Pattern.compile( regionDisplay.getSourceNamesRegex() );
		final HashMap< String, List< String > > columnToValues = new HashMap<>();
		final List< String > groupNames = MoBIEHelper.getGroupNames( regionDisplay.getSourceNamesRegex() );

		Integer groupCount = null;
		Boolean useNames = null;

		for ( String region : regionNames )
		{
			final String source = regionDisplay.sources.get( region ).get( 0 );
			final Matcher matcher = pattern.matcher( source );
			if ( ! matcher.matches() )
			{
				throw new RuntimeException( "Could not match regex for " + source );
			}

			// use the first region to initialise
			// whether there are named groups
			// to be used for the column names
			if ( groupCount == null ) groupCount = matcher.groupCount();
			if ( useNames == null ) useNames = groupNames.size() == groupCount;

			for ( int groupIndex = 0; groupIndex < groupCount; groupIndex++ )
			{
				final String value = matcher.group( groupIndex + 1 );

				String column = useNames ? groupNames.get( groupIndex ) : "" + groupIndex;

				if ( ! columnToValues.containsKey( column ) )
					columnToValues.put( column, new ArrayList<>() );

				columnToValues.get( column ).add( value );
			}
		}

		for ( String column : columnToValues.keySet() )
		{
			table.addColumns( StringColumn.create( column, columnToValues.get( column ) ) );
		}
	}

}

