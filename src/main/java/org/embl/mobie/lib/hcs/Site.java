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
	private final String name;
	private int[] dimensions;
	private Map< TPosition, Map< ZPosition, String > > paths = new LinkedHashMap();
	private VoxelDimensions voxelDimensions;
	private ImageDataFormat imageDataFormat;
	private AbstractSpimData< ? > spimData;

	public Site( String name, ImageDataFormat imageDataFormat )
	{
		this.name = name;
		this.imageDataFormat = imageDataFormat;
	}

	public Site( String name, ImageDataFormat imageDataFormat, AbstractSpimData< ? > spimData, int imageIndex )
	{
		this.name = name;
		this.imageDataFormat = imageDataFormat;
		this.spimData = spimData;
		this.channel = imageIndex;
	}

	public String getName()
	{
		return name;
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
