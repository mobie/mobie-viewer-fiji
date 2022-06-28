package org.embl.mobie.viewer.source;

import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import de.embl.cba.tables.TableColumns;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import net.imglib2.Volatile;
import net.imglib2.converter.Converter;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;
import org.embl.mobie.io.util.IOHelper;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.MoBIEHelper;
import org.embl.mobie.viewer.TableColumnNames;
import org.embl.mobie.viewer.color.LazyConverter;
import org.embl.mobie.viewer.color.VolatileLazyConverter;
import org.embl.mobie.viewer.table.TableHelper;
import org.embl.mobie.viewer.table.TableRowsTableModel;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * Needed functions by MergedGridSource without loading data:
 *
 * for reference source:
 * - getSourceTransform()
 * - rai.dimensionsAsLongArray() // could be provided by dedicated method in LazySource.
 *
 * in fact the other sources are not needed it seems...
 *
 * However, for transforming all the individual ones,
 * one needs the SpimSource and the Converters, which is crazy.
 * Can one do this another way? E.g. could the MergedGridSource
 * provide the positions of those sources?
 *
 * @param <N>
 */
public class SourceAndConverterAndTables< N extends NumericType< N > > extends SourceAndConverter< N >
{
	private LazyConverter lazyConverter;
	private final MoBIE moBIE;
	private String name;
	private final SourceAndConverter< N > initializationSourceAndConverter;
	private SourceAndConverter< N > sourceAndConverter;
	private LazySpimSource< N > lazySpimSource;
	private String primaryTable;
	private ArrayList< String > secondaryTables = new ArrayList<>();;
	private TableRowsTableModel< TableRowImageSegment > tableRows;
	private String tableRootDirectory;

	// TODO: maybe we do not need this class
	//   but only the lazy ingredients.
	//   the lazy ingredient could be initialised with a
	//   SpatialData object that can (on demand) provide
	//   the raw material for building the Source and VolatileSource.
	//   or ImageData and SegmentationData??
	//   Which methods do these classes need?
	//   SegmentationData:
	//      // to construct a SourceAndConverter
	//      // can I avoid having them Lazy internally (see below comment on FunctionGridSource )?
	//      // how to deal with the parts of the code where transformations are added?
	//      // maybe add the transformations to SegmentationData and then apply them only when the SAC is constructed?
	//      - public SegmentationSource getSource() // already converted with <S> pixel values??
	//      - public VolatileSegmentationSource getVolatileSource() // already converted with <VS> pixel values??
	//        // for tableView:
	//      - public List< TableRowImageSegment > getTableRowImageSegments();
	//        - internally calls: getTableColumns( "default.tsv" )
	//      - public getTableColumns( tableName ); // needed?
	//      - or: public mergeTable( tableName );
	//        How does the TableView access this method?
	//        Maybe a field List< SegmentationData > in SegmentationDisplay?
	//      - private < Segment > get( label_id, timepoint )
	//         -  this needs to getTableRows( "default.tsv" )
	//         The Segments will be the pixel values of the SegmentationSource
	//   Who is responsible for merging table columns?
	//      in fact the SegmentationSource does not really care about
	//      additional columns, but it is the only place that holds a
	//      reference to SegmentationData...?
	//   Then we need two version of Source (which probably is good):
	//   ImageSource and SegmentationSource
	//   Do we need explict Volatile counterparts?
	//   Both are lazy, nothing loaded initially (unless triggered)
	//   Which methods? What do they implement?
	//   SegmentationSource:
	//   - getTableRows();
	//   Can we rename the corresponding classes from the serialisation?
	//   Yes, seems like just Image and Segmentation would work.
	//   How does the pointSource look like (spatialOmics)?
	//   --
	//   The converters could be PlaceHolderConverter anyway
	//   for now as we do not use them (Maybe we can forget about the LazyConverter)?
	//   In fact it does not make any sense to keep this class, because
	//   it will be quickly ripped apart once the sources are wrapped and
	//   a new SAC will be constructed.
	//   Maybe the FunctionGridSource would actually take a List< SegmentationData >
	//   rather than a List< Source > ?
	public SourceAndConverterAndTables( MoBIE moBIE, String name, @Nullable SourceAndConverter< N > initializationSourceAndConverter )
	{
		super( null, null );
		this.moBIE = moBIE;
		this.name = name;
		this.initializationSourceAndConverter = initializationSourceAndConverter;
		lazySpimSource = new LazySpimSource( this );
		lazyConverter = new LazyConverter( this );
	}

	@Override
	public Source< N > getSpimSource()
	{
		return lazySpimSource;
	}

	@Override
	public Converter< N, ARGBType > getConverter()
	{
		return lazyConverter;
	}

	@Override
	public SourceAndConverter< ? extends Volatile< N > > asVolatile()
	{
		final VolatileLazySpimSource volatileLazySpimSource = new VolatileLazySpimSource( this );
		final VolatileLazyConverter volatileLazyConverter = new VolatileLazyConverter( this );
		return new SourceAndConverter( volatileLazySpimSource, volatileLazyConverter  );
	}

	public synchronized SourceAndConverter< N > getSourceAndConverter()
	{
		if ( sourceAndConverter == null )
		{
			// open associated tables
			if ( tableRows != null )
			{
				// if needed we could also open
				// other type of tables here,
				// based on the type of tableRows
				openSegmentationTables();
			}

			// open image
			sourceAndConverter = ( SourceAndConverter< N > ) moBIE.openSourceAndConverter( name, "Lazy open " );
		}

		return sourceAndConverter;
	}

	public void setName( String name )
	{
		this.name = name;
	}

	private void openSegmentationTables()
	{
		// primary
		final String path = IOHelper.combinePath( tableRootDirectory, primaryTable );
		final List< TableRowImageSegment > tableRowImageSegments = MoBIEHelper.readImageSegmentsFromTableFile( path, name );
		tableRows.addAll( tableRowImageSegments );

		// secondary
		for ( String secondaryTable : secondaryTables )
		{
			Map< String, List< String > > columns = TableColumns.stringColumnsFromTableFile( IOHelper.combinePath( tableRootDirectory, secondaryTable ) );
			TableHelper.addColumn( columns, TableColumnNames.LABEL_IMAGE_ID, name );
			tableRows.mergeColumns( columns );
		}
	}

	public void setTableRootDirectory( String tableRootDirectory )
	{
		this.tableRootDirectory = tableRootDirectory;
	}

	public void setPrimaryTable( String primaryTable )
	{
		this.primaryTable = primaryTable;
	}

	public void addTable( String tableName )
	{
		secondaryTables.add( tableName );
	}

	public void setTableRows( TableRowsTableModel< TableRowImageSegment > tableRows )
	{
		this.tableRows = tableRows;
	}

	public String getName()
	{
		return name;
	}

	public SourceAndConverter< N > getInitializationSourceAndConverter()
	{
		if ( initializationSourceAndConverter == null )
		{
			getSourceAndConverter();
			return sourceAndConverter;
		}
		else
		{
			return initializationSourceAndConverter;
		}
	}
}
