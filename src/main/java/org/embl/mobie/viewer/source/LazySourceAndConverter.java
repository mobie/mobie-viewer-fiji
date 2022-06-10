package org.embl.mobie.viewer.source;

import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import de.embl.cba.tables.tablerow.TableRow;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.Volatile;
import net.imglib2.converter.Converter;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;
import org.embl.mobie.viewer.MoBIE;

import java.util.HashMap;
import java.util.List;


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
 * @param <T>
 */
public class LazySourceAndConverter< T extends NumericType< T > > extends SourceAndConverter< T >
{
	private final MoBIE moBIE;
	private String name;
	private final T type;
	private final double[] min;
	private final double[] max;
	private SourceAndConverter< T > sourceAndConverter;
	private LazySpimSource< T > lazySpimSource;
	private boolean isOpen = false;
	private HashMap< String, List< TableRow > > tablesToTableRows;

	public LazySourceAndConverter( MoBIE moBIE, String name, AffineTransform3D sourceTransform, VoxelDimensions voxelDimensions, T type, double[] min, double[] max )
	{
		super( null, null );
		this.moBIE = moBIE;
		this.name = name;
		this.type = type;
		this.min = min;
		this.max = max;
		this.lazySpimSource = new LazySpimSource( this, name, sourceTransform, voxelDimensions, min, max );
		this.tablesToTableRows = new HashMap< String, List< TableRow  > >();
	}

	@Override
	public Source< T > getSpimSource()
	{
		return lazySpimSource;
	}

	@Override
	public Converter< T, ARGBType > getConverter()
	{
		return getSourceAndConverter().getConverter();
	}

	@Override
	public SourceAndConverter< ? extends Volatile< T > > asVolatile()
	{
		return getSourceAndConverter().asVolatile();
	}

	public SourceAndConverter< T > getSourceAndConverter()
	{
		if ( sourceAndConverter == null )
		{
			for ( String tableName : tablesToTableRows.keySet() )
			{
				final List< TableRowImageSegment > tableRows = moBIE.loadImageSegmentsTable( name, tableName, name );
				tablesToTableRows.get( tableName ).addAll( tableRows );
			}
			sourceAndConverter = ( SourceAndConverter< T > ) moBIE.openSourceAndConverter( name, null );
			isOpen = true;
		}

		return sourceAndConverter;
	}

	public boolean isOpen()
	{
		return isOpen;
	}

	public void setName( String name )
	{
		this.name = name;
		lazySpimSource.setName( name );
	}

	public void setSourceTransform( AffineTransform3D sourceTransform )
	{
		lazySpimSource.setSourceTransform( sourceTransform );
	}

	public HashMap< String, List< TableRow > > getTablesToTableRows()
	{
		return tablesToTableRows;
	}
}
