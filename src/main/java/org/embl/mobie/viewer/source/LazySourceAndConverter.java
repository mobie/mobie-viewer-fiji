package org.embl.mobie.viewer.source;

import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.Volatile;
import net.imglib2.converter.Converter;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import org.embl.mobie.viewer.MoBIE;


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
public class LazySourceAndConverter extends SourceAndConverter
{
	private final MoBIE moBIE;
	private String name;
	private AffineTransform3D sourceTransform;
	private VoxelDimensions voxelDimensions;
	private SourceAndConverter< ? > sourceAndConverter;

	public LazySourceAndConverter( MoBIE moBIE, String name, AffineTransform3D sourceTransform, VoxelDimensions voxelDimensions )
	{
		super( null );
		this.moBIE = moBIE;
		this.name = name;
		this.sourceTransform = sourceTransform;
		this.voxelDimensions = voxelDimensions;
	}

	private void open()
	{
		sourceAndConverter = moBIE.openSourceAndConverter( name, "Opening " + name + "..." );
	}

	@Override
	public Source< ? > getSpimSource()
	{
		if ( spimSource == null )
			open();

		return sourceAndConverter.getSpimSource();
	}

	@Override
	public Converter< ?, ARGBType > getConverter()
	{
		if ( spimSource == null )
			open();

		return sourceAndConverter.getConverter();
	}

	public SourceAndConverter< ? extends Volatile< ? > > asVolatile()
	{
		if ( spimSource == null )
			open();

		return sourceAndConverter.asVolatile();
	}

	public void getSourceTransform( AffineTransform3D transform3D )
	{
		transform3D.set( sourceTransform );
	}

	public VoxelDimensions getVoxelDimensions()
	{
		return voxelDimensions;
	}

	public void setSourceTransform( AffineTransform3D sourceTransform )
	{
		this.sourceTransform = sourceTransform;
	}

	public void setName( String name )
	{
		this.name = name;
	}
}
