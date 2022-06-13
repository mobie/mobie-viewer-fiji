package org.embl.mobie.viewer.source;

import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.Volatile;
import net.imglib2.converter.Converter;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.table.TableRowsTableModel;

import java.util.HashMap;


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
public class LazySourceAndConverter< N extends NumericType< N > > extends SourceAndConverter< N >
{
	private final MoBIE moBIE;
	private String name;
	private final N type;
	private SourceAndConverter< N > sourceAndConverter;
	private LazySpimSource< N > lazySpimSource;
	private boolean isOpen = false;

	public LazySourceAndConverter( MoBIE moBIE, String name, AffineTransform3D sourceTransform, VoxelDimensions voxelDimensions, N type, double[] min, double[] max )
	{
		super( null, null );
		this.moBIE = moBIE;
		this.name = name;
		this.type = type;
		this.lazySpimSource = new LazySpimSource( this, name, sourceTransform, voxelDimensions, min, max );
	}

	@Override
	public Source< N > getSpimSource()
	{
		return lazySpimSource;
	}

	@Override
	public Converter< N, ARGBType > getConverter()
	{
		return getSourceAndConverter().getConverter();
	}

	@Override
	public SourceAndConverter< ? extends Volatile< N > > asVolatile()
	{
		// TODO: how will this trigger the table loading??
		return getSourceAndConverter().asVolatile();
	}

	// TODO: Can I make this non-public? Only La
	public SourceAndConverter< N > getSourceAndConverter()
	{
		if ( sourceAndConverter == null )
		{
			sourceAndConverter = ( SourceAndConverter< N > ) moBIE.openSourceAndConverter( name, null );
		}
		return sourceAndConverter;
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

	// TODO: Can we remove?
	public MoBIE getMoBIE()
	{
		return moBIE;
	}
}
