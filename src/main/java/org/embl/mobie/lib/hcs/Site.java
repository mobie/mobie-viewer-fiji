package org.embl.mobie.lib.hcs;

import mpicbg.spim.data.sequence.VoxelDimensions;
import org.embl.mobie.lib.io.StorageLocation;
import org.embl.mobie.lib.io.TPosition;
import org.embl.mobie.lib.io.ZPosition;

import java.util.LinkedHashMap;
import java.util.Map;

public class Site extends StorageLocation
{
	private final String name;
	private int[] pixelDimensions;
	private Map< TPosition, Map< ZPosition, String > > paths = new LinkedHashMap();
	private VoxelDimensions voxelDimensions;

	public Site( String name )
	{
		this.name = name;
	}

	public String getName()
	{
		return name;
	}

	public int[] getPixelDimensions()
	{
		return pixelDimensions;
	}

	public void setPixelDimensions( int[] pixelDimensions )
	{
		this.pixelDimensions = pixelDimensions;
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
}
