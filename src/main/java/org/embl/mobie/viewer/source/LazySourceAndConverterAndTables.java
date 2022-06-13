package org.embl.mobie.viewer.source;

import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import de.embl.cba.tables.TableColumns;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.Volatile;
import net.imglib2.converter.Converter;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;
import org.embl.mobie.io.util.IOHelper;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.MoBIEHelper;
import org.embl.mobie.viewer.TableColumnNames;
import org.embl.mobie.viewer.table.TableHelper;
import org.embl.mobie.viewer.table.TableRowsTableModel;

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
public class LazySourceAndConverterAndTables< N extends NumericType< N > > extends SourceAndConverter< N >
{
	private LazyConverter lazyConverter;
	private final MoBIE moBIE;
	private String name;
	private final AffineTransform3D sourceTransform;
	private final VoxelDimensions voxelDimensions;
	private final N type;
	private final double[] min;
	private final double[] max;
	private SourceAndConverter< N > sourceAndConverter;
	private LazySpimSource< N > lazySpimSource;
	private String primaryTable;
	private ArrayList< String > secondaryTables = new ArrayList<>();;
	private TableRowsTableModel< TableRowImageSegment > tableRows;
	private String tableRootDirectory;

	public LazySourceAndConverterAndTables( MoBIE moBIE, String name, AffineTransform3D sourceTransform, VoxelDimensions voxelDimensions, N type, double[] min, double[] max )
	{
		super( null, null );
		this.moBIE = moBIE;
		this.name = name;
		this.sourceTransform = sourceTransform;
		this.voxelDimensions = voxelDimensions;
		this.type = type;
		this.min = min;
		this.max = max;
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
		final VolatileLazySpimSource volatileLazySpimSource = new VolatileLazySpimSource( this, name, sourceTransform, voxelDimensions, min, max );
		final VolatileLazyConverter volatileLazyConverter = new VolatileLazyConverter( this );
		return new SourceAndConverter( volatileLazySpimSource, volatileLazyConverter  );
	}

	public SourceAndConverter< N > openSourceAndConverter()
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
			sourceAndConverter = ( SourceAndConverter< N > ) moBIE.openSourceAndConverter( name, null );
		}
		return sourceAndConverter;
	}

	public void setName( String name )
	{
		this.name = name;
		lazySpimSource.setName( name );
	}

	// TODO: Can we remove?
	public MoBIE getMoBIE()
	{
		return moBIE;
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

	public VoxelDimensions getVoxelDimensions()
	{
		return voxelDimensions;
	}

	public N getType()
	{
		return type;
	}

	public double[] getMin()
	{
		return min;
	}

	public double[] getMax()
	{
		return max;
	}

	public String getName()
	{
		return name;
	}
}
