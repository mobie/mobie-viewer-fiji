package org.embl.mobie.viewer.source;


import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.NumericType;
import org.embl.mobie.io.util.IOHelper;
import org.embl.mobie.viewer.MoBIEHelper;
import org.embl.mobie.viewer.table.TableHelper;
import org.embl.mobie.viewer.table.TableRowsTableModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LazySpimSource< T extends NumericType< T > > implements Source< T >
{
	private final LazySourceAndConverter< T > lazySourceAndConverter;
	private String name;
	private AffineTransform3D sourceTransform;
	private VoxelDimensions voxelDimensions;
	private final double[] min;
	private final double[] max;
	private Source< T > spimSource;
	private String primaryTable;
	private ArrayList< String > secondaryTables = new ArrayList<>();;
	private TableRowsTableModel< TableRowImageSegment > tableRows;
	private String tableRootDirectory;

	public LazySpimSource( LazySourceAndConverter< T > lazySourceAndConverter, String name, AffineTransform3D sourceTransform, VoxelDimensions voxelDimensions, double[] min, double[] max )
	{
		this.lazySourceAndConverter = lazySourceAndConverter;
		this.name = name;
		this.sourceTransform = sourceTransform;
		this.voxelDimensions = voxelDimensions;
		this.min = min;
		this.max = max;
	}

	@Override
	public boolean isPresent( int t )
	{
		return source().isPresent( t );
	}

	@Override
	public RandomAccessibleInterval< T > getSource( int t, int level )
	{
		return source().getSource( t, level );
	}

	@Override
	public RealRandomAccessible< T > getInterpolatedSource( int t, int level, Interpolation method )
	{
		return source().getInterpolatedSource( t, level, method );
	}

	@Override
	public void getSourceTransform( int t, int level, AffineTransform3D transform )
	{
		if ( spimSource == null )
			transform.set( sourceTransform );
		else
			spimSource.getSourceTransform( t, level, transform );
	}

	@Override
	public T getType()
	{
		return source().getType();
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public VoxelDimensions getVoxelDimensions()
	{
		return voxelDimensions;
	}

	@Override
	public int getNumMipmapLevels()
	{
		Thread.dumpStack();
		throw new RuntimeException("PlaceHolderSpimSource...");
	}

	public void setName( String name )
	{
		this.name = name;
	}

	public void setSourceTransform( AffineTransform3D sourceTransform )
	{
		this.sourceTransform = sourceTransform;
	}

	public double[] getMin()
	{
		return min;
	}

	public double[] getMax()
	{
		return max;
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

	private Source< T > source()
	{
		if ( spimSource == null )
		{
			// open tables

			// primary
			final String path = IOHelper.combinePath( tableRootDirectory, primaryTable );
			final List< TableRowImageSegment > tableRowImageSegments = MoBIEHelper.readImageSegmentsFromTableFile( path, name );
			tableRows.addAll( tableRowImageSegments );

			// secondary
			for ( String secondaryTable : secondaryTables )
			{
				Map< String, List< String > > columns = TableHelper.loadTableAndAddImageIdColumn( name, lazySourceAndConverter.getMoBIE().getTablePath( ( SegmentationSource ) lazySourceAndConverter.getMoBIE().getImageSource( name ), secondaryTable ) );
				tableRows.mergeColumns( columns );
			}

			// open image
			spimSource = lazySourceAndConverter.getSourceAndConverter().getSpimSource();
		}

		return spimSource;
	}

	public void setTableRootDirectory( String tableRootDirectory )
	{
		this.tableRootDirectory = tableRootDirectory;
	}
}

