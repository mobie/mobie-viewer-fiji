package org.embl.mobie.lib.hcs;

import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.sequence.VoxelDimensions;
import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.lib.io.StorageLocation;
import org.embl.mobie.lib.io.TPosition;
import org.embl.mobie.lib.io.ZPosition;

import java.util.LinkedHashMap;
import java.util.Map;

public class Site extends StorageLocation
{
	// note that the ID is not the final name of the corresponding image,
	// but the image name will be concatenated also using the channel and well name.
	private final String id;
	private int[] dimensions;
	private Map< TPosition, Map< ZPosition, String > > paths = new LinkedHashMap();
	private VoxelDimensions voxelDimensions;
	private ImageDataFormat imageDataFormat;
	private AbstractSpimData< ? > spimData;

	public Site( String id, ImageDataFormat imageDataFormat )
	{
		this.id = id;
		this.imageDataFormat = imageDataFormat;
	}

	public Site( String id, ImageDataFormat imageDataFormat, AbstractSpimData< ? > spimData, int imageIndex )
	{
		this.id = id;
		this.imageDataFormat = imageDataFormat;
		this.spimData = spimData;
		this.channel = imageIndex;
	}

	public String getID()
	{
		return id;
	}

	public int[] getDimensions()
	{
		return dimensions;
	}

	public void setDimensions( int[] dimensions )
	{
		this.dimensions = dimensions;
	}

	public void addPath( TPosition t, ZPosition z, String path )
	{
		if ( ! paths.containsKey( t ) )
		{
			paths.put( t, new LinkedHashMap<>() );
		}

		paths.get( t ).put( z, path );
	}

	public void addPath( String t, String z, String path )
	{
		final TPosition tPosition = new TPosition( t );
		final ZPosition zPosition = new ZPosition( z );
		addPath( tPosition, zPosition, path );
	}

	public Map< TPosition, Map< ZPosition, String > > getPaths()
	{
		return paths;
	}

	public void setVoxelDimensions( VoxelDimensions voxelDimensions )
	{
		this.voxelDimensions = voxelDimensions;
	}

	public VoxelDimensions getVoxelDimensions()
	{
		return voxelDimensions;
	}

	public ImageDataFormat getImageDataFormat()
	{
		return imageDataFormat;
	}

	public AbstractSpimData< ? > getSpimData()
	{
		return spimData;
	}
}
