package org.embl.mobie.lib.hcs;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum HCSPattern
{
	MolDevSitesChannels,
	MolDevSites,
	Operetta;

	public static final String WELL = "W";
	public static final String SITE = "S";
	public static final String CHANNEL = "C";

	private final String MD_SITES_CHANNELS = ".*_(?<"+WELL+">[A-Z]{1}[0-9]{2})_s(?<"+SITE+">.*)_w(?<"+CHANNEL+">[0-9]{1}).*";
	private final String MD_SITES = ".*_(?<"+WELL+">[A-Z]{1}[0-9]{2})_s(?<"+SITE+">[0-9]{1}).*";


	// TODO: add the ones below
	private final String PATTERN_MD_A01_WAVELENGTH = ".*_(?<"+WELL+">[A-Z]{1}[0-9]{2})_(?<"+CHANNEL+">.*).tif";
	private final String PATTERN_ALMF_TREAT1_TREAT2_WELLNUM_POSNUM_CHANNEL = ".*--(.*)--(.*)--W(?<"+WELL+">[0-9]{4})--P(?<"+SITE+">[0-9]{3})--T[0-9]{4,5}--Z[0-9]{3}--(?<"+CHANNEL+">.*)";
	private final String PATTERN_SCANR_WELLNUM_SITENUM_CHANNEL = ".*--W(?<"+WELL+">[0-9]{5})--P(?<"+SITE+">[0-9]{5}).*--.*--(?<"+CHANNEL+">.*)\\..*";
	private final String PATTERN_NIKON_TI2_HDF5 = ".*Well([A-Z]{1}[0-9]{2})_Point[A-Z]{1}[0-9]{2}_([0-9]{4})_.*h5$";
	private final String OPERETTA = ".*(?<"+WELL+">r[0-9]{2}c[0-9]{2})f(?<"+SITE+">[0-9]{2})p[0-9]{2}.*-ch(?<"+CHANNEL+">[0-9])sk.*.tiff$";

	public static HCSPattern fromPath( String fileName )
	{
		for ( HCSPattern hcsPattern : HCSPattern.values() )
		{
			final Matcher matcher = hcsPattern.getMatcher( fileName );
			if ( matcher.matches() )
				return hcsPattern;
		}

		return null;
	}

	public Matcher getMatcher( String path )
	{
		switch( this )
		{
			case Operetta:
				return Pattern.compile( OPERETTA ).matcher( path );
			case MolDevSites:
				return Pattern.compile( MD_SITES ).matcher( path );
			case MolDevSitesChannels:
			default:
				return Pattern.compile( MD_SITES_CHANNELS ).matcher( path );
		}
	}
}
