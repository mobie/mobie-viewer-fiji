package org.embl.mobie.lib.hcs;

import ij.IJ;
import ij.ImagePlus;
import org.embl.mobie.lib.color.ColorHelper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Plate
{
	private final String hcsDirectory;
	private HCSPattern hcsPattern;
	private HashMap< Channel, Map< Well, Set< Site > > > channelWellSites;
	private double[] siteRealDimensions;
	private int sitesPerWell;

	public Plate( String hcsDirectory ) throws IOException
	{
		this.hcsDirectory = hcsDirectory;

		final List< String > paths = Files.walk( Paths.get( hcsDirectory ) ).map( p -> p.toString() ).collect( Collectors.toList() );

		hcsPattern = determineHCSPattern( hcsDirectory, paths );

		buildPlateMap( paths );
	}

	private void buildPlateMap( List< String > paths )
	{
		channelWellSites = new HashMap<>();

		for ( String path : paths )
		{
			if ( ! hcsPattern.setPath( path ) )
				continue;

			// store this file's channel, well, and site
			//

			// TODO: https://github.com/mobie/mobie-viewer-fiji/issues/972
			//    WellH06_PointH06_0007_ChannelDAPI,WF_GFP,TRITC,WF_Cy5,DIA_Seq0502.tiff
			//    Maybe change to hcsPattern.getChannels(); (plural) ?
			//    and then loop through the channels
			//    and add a suffix to the path "--c0" to indicate which channel to load?
			//    Maybe I need something more complex than a String to represent the path?
			//    In fact, could I use StorageLocation already here?

			Channel channel = hcsPattern.getChannel();
			if ( ! channelWellSites.containsKey( channel ) )
			{
				channelWellSites.put( channel, new HashMap<>() );
				final ImagePlus imagePlus = IJ.openImage( path );
				final String color = ColorHelper.getString( imagePlus.getLuts()[ 0 ] );
				channel.setColor( color );
				final double[] contrastLimits = new double[]
				{
					imagePlus.getDisplayRangeMin(),
					imagePlus.getDisplayRangeMax()
				};
				channel.setContrastLimits( contrastLimits );

				siteRealDimensions = new double[]
				{
					imagePlus.getWidth() * imagePlus.getCalibration().pixelWidth,
					imagePlus.getHeight() * imagePlus.getCalibration().pixelHeight
				};
			}

			Well well = hcsPattern.getWell();
			if ( ! channelWellSites.get( channel ).containsKey( well ) )
			{
				channelWellSites.get( channel ).put( well, new HashSet<>() );
			}

			final Site site = hcsPattern.getSite();
			channelWellSites.get( channel ).get( well ).add( site );
			final int numSites = channelWellSites.get( channel ).get( well ).size();
			if ( numSites > sitesPerWell )
				sitesPerWell = numSites; // needed to compute the site position within a well

			final String t = hcsPattern.getT();
			final String z = hcsPattern.getZ();

			site.storageLocation().addPath( t, z, path );
		}
	}

	private HCSPattern determineHCSPattern( String hcsDirectory, List< String > paths )
	{
		for ( String path : paths )
		{
			final HCSPattern hcsPattern = HCSPattern.fromPath( path );
			if ( hcsPattern != null )
				return hcsPattern;
		}

		throw new RuntimeException( "Could not determine HCSPattern for " + hcsDirectory );
	}

	public Set< Channel > getChannels()
	{
		return channelWellSites.keySet();
	}

	public Set< Well > getWells( Channel channel )
	{
		return channelWellSites.get( channel ).keySet();
	}

	public Set< Site > getSites( Channel channel, Well well )
	{
		return channelWellSites.get( channel ).get( well );
	}


	public int[] computeGridPosition( Site site )
	{
		switch ( hcsPattern )
		{
			default:
			case Operetta:
				int siteIndex = Integer.parseInt( site.getName() ) - 1;
				int numSiteColumns = (int) Math.sqrt( sitesPerWell );

				int[] gridPosition = new int[ 2 ];
				gridPosition[ 0 ] = siteIndex % numSiteColumns; // column
				gridPosition[ 1 ] = siteIndex / numSiteColumns; // row

				// System.out.println( "Site  = " + site + ", c = " + gridPosition[ 0 ] + ", r = " + gridPosition[ 1 ]);

				return gridPosition;
		}
	}

	public int[] getWellGridPosition( Well well )
	{
		switch ( hcsPattern )
		{
			default:
			case Operetta:
				final int[] gridPosition = hcsPattern.decodeWellGridPosition( well.getName() );
				// System.out.println( "Well  = " + well + ", c = " + gridPosition[ 0 ] + ", r = " + gridPosition[ 1 ]);
				return gridPosition;
		}
	}

	public boolean is2D()
	{
		return true; // for now...
	}

	public String getName()
	{
		return new File( hcsDirectory ).getName();
	}

	public HCSPattern getHcsPattern()
	{
		return hcsPattern;
	}

	public double[] getSiteRealDimensions()
	{
		return siteRealDimensions;
	}
}
