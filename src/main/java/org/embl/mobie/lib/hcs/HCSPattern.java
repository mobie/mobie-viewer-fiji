/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2024 EMBL
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.embl.mobie.lib.hcs;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum HCSPattern
{
	OMEZarr,
	Operetta,
	IncuCyte,
	IncuCyteRaw,
	InCell, // https://github.com/embl-cba/plateviewer/issues/45
	MolecularDevices,
	YokogawaCQ1,
	InCarta,
	Araceli;

	static { net.imagej.patcher.LegacyInjector.preinit(); }

	public static final String WELL = "W";
	public static final String SITE = "S";
	public static final String CHANNEL = "C";
	public static final String TIME = "T";
	public static final String SLICE = "Z";

	/*
	example:
	/g/cba/exchange/hcs-test/hcs-test.zarr/A/1/0/
	well = C05, site = 1, channel = 1
 	*/
	//private static final String OME_ZARR = ".*.zarr/(?<"+WELL+">[A-Z]/[0-9]+)/(?<"+SITE+">[0-9]+)$";
	private static final String OME_ZARR = ".+\\.zarr[\\\\/](?<"+WELL+">[A-Z][\\\\/][0-9]+)[\\\\/](?<"+SITE+">[0-9]+)$";

	/*
	example:
	r01c01f04p01-ch1sk1fk1fl1.tiff
	well = r01c01, site = 04, channel = 1
	 */
	private static final String OPERETTA = ".*(?<"+WELL+">r[0-9]{2}c[0-9]{2})f(?<"+SITE+">[0-9]{2})p[0-9]{2}.*-ch(?<"+CHANNEL+">[0-9])sk.*.tiff$";

	/*
	example:
	MiaPaCa2-PhaseOriginal_A2_1_03d06h40m.tif
	well = A2, site = 1, frame = 03d06h40m
	 */
	private static final String INCUCYTE = ".*_(?<"+WELL+">[A-Z]{1}[0-9]{1,2})_(?<"+SITE+">[0-9]{1,2})_(?<"+TIME+">[0-9]{2}d[0-9]{2}h[0-9]{2}m).tif$";

	/*
	example:
	B3-3-C2.tif
	well = B3, site = 3, channel = C2
	time point is encoded in the folder
	 */
	private static final String INCUCYTE_RAW = ".*[/\\\\](?<"+ TIME +">\\d+)[/\\\\]\\d+[/\\\\](?<"+WELL+">[A-Z]{1}[0-9]{1,2})-(?<"+SITE+">[0-9]{1,2})-(?<"+CHANNEL+">.*).tif$";

	/*
	examples:
	MIP-2P-2sub_C05_s1_w146C9B2CD-0BB3-4B8A-9187-2805F4C90506.tif
	well = C05, site = 1, channel = 1
	Plate10x4sites_A01_s1_w1.TIF
	well = A01, site = 1, channel = 1
	 */
	private static final String MOLDEV = ".*_(?<"+WELL+">[A-Z]{1}[0-9]{2})_s(?<"+SITE+">.*)_w(?<"+CHANNEL+">[0-9])[^_thumb].*";

	/*
	example:
	A - 01(fld 1 wv Green - dsRed z 3).tif
	well = A - 01, site = 1, channel = 1
 	*/
	public static final String INCELL = ".*(?<"+WELL+">[A-Z]{1} - [0-9]{2})\\(fld (?<"+SITE+">[0-9]{1}) (?<"+CHANNEL+">.*)\\).tif";

	/*
	example:
	t1_D04_s1_w1_z3.tif
	well = D04, site = 1, channel = 1, slice = 3
	 */
	public static final String INCARTA = ".*_(?<"+WELL+">[A-Z][0-9]{2})_s(?<"+SITE+">[0-9])_w(?<"+CHANNEL+">[0-9])_z(?<"+SLICE+">[0-9]).tif";

	/*
	example:
	A1_s1_w1_z-m_20241121T095306Z_01a46354-f00a-4207-ba40-15c281fd4049_thumbnail.tiff
	well = A1, site = 1, channel = 1

	company: https://www.aracelibio.com/
	 */
	public static final String ARACELI = ".*(?<"+WELL+">[A-Z][0-9]{1,2})_s(?<"+SITE+">[0-9]{1,2})_w(?<"+CHANNEL+">[0-9])_z.*.tiff";

	/*
	example:
	W0018F0001T0001Z001C1.tif
	well = A - 01, site = 1, channel = 1
	 */
	public static final String YOKOGAWACQ1 = ".*W(?<"+WELL+">[0-9]+)F(?<"+SITE+">[0-9]+)T(?<"+TIME+">[0-9]+)(?<"+SLICE+">Z[0-9]+)C(?<"+CHANNEL+">[0-9]+).tif";

	private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

	// TODO: add the ones below
	private final String PATTERN_MD_A01_WAVELENGTH = ".*_(?<"+WELL+">[A-Z]{1}[0-9]{2})_(?<"+CHANNEL+">.*).tif";
	private final String PATTERN_ALMF_TREAT1_TREAT2_WELLNUM_POSNUM_CHANNEL = ".*--(.*)--(.*)--W(?<"+WELL+">[0-9]{4})--P(?<"+SITE+">[0-9]{3})--T[0-9]{4,5}--Z[0-9]{3}--(?<"+CHANNEL+">.*)";
	private final String PATTERN_SCANR_WELLNUM_SITENUM_CHANNEL = ".*--W(?<"+WELL+">[0-9]{5})--P(?<"+SITE+">[0-9]{5}).*--.*--(?<"+CHANNEL+">.*)\\..*";
	private final String PATTERN_NIKON_TI2_HDF5 = ".*Well([A-Z]{1}[0-9]{2})_Point[A-Z]{1}[0-9]{2}_([0-9]{4})_.*h5$";
	private final String MD_SITES = ".*_(?<"+WELL+">[A-Z]{1}[0-9]{2})_s(?<"+SITE+">[0-9]{1}).*";

	private Matcher matcher;
	private List< String > channels;

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

	private Matcher getMatcher( String path )
	{
		switch( this )
		{
			case OMEZarr:
				return Pattern.compile( OME_ZARR ).matcher( path );
			case Operetta:
				return Pattern.compile( OPERETTA ).matcher( path );
			case MolecularDevices:
				return Pattern.compile( MOLDEV ).matcher( path );
			case InCell:
				return Pattern.compile( INCELL ).matcher( path );
			case IncuCyteRaw:
				return Pattern.compile( INCUCYTE_RAW ).matcher( path );
			case YokogawaCQ1:
				return Pattern.compile( YOKOGAWACQ1 ).matcher( path );
			case InCarta:
				return Pattern.compile( INCARTA ).matcher( path );
			case Araceli:
				return Pattern.compile( ARACELI ).matcher( path );
			default:
			case IncuCyte:
				return Pattern.compile( INCUCYTE ).matcher( path );
		}
	}

	public boolean setMatcher( String path )
	{
		if ( new File( path ).getName().startsWith( "." ) )
			return false;

		matcher = getMatcher( path );

		return matcher.matches();
	}

	public int[] decodeWellGridPosition( String well )
	{
		switch ( this )
		{
			case OMEZarr:
				return decodeOMEZarrWellPosition( well );
			case Operetta:
				return decodeOperettaWellPosition( well );
			case InCell:
				return decodeInCellWellPosition( well );
			case YokogawaCQ1:
				return decodeYokogawaQC1WellPosition( well );
			default:
				return decodeA01WellPosition( well );
		}
	}

	private static int[] decodeOMEZarrWellPosition( String well )
	{
		final Matcher matcher = Pattern.compile( "(?<row>[A-Z])/(?<col>[0-9]+)" ).matcher( well );
		matcher.matches();
		final int x = Integer.parseInt( matcher.group( "col" ) ) - 1;
		final int y = ALPHABET.indexOf( matcher.group("row") );
		return new int[]{ x, y };
	}

	private static int[] decodeYokogawaQC1WellPosition( String well )
	{
		final int wellIndex = Integer.parseInt( well );
		final int numColumns = 24; // 384 well plate TODO: could it be also another plate type?
		final int x = (wellIndex - 1) % numColumns;
		final int y = (wellIndex - 1) / numColumns;
		return new int[]{ x, y };
	}

	private static int[] decodeOperettaWellPosition( String well )
	{
		final Matcher matcher = Pattern.compile( "r(?<row>[0-9]{2})c(?<col>[0-9]{2})" ).matcher( well );
		matcher.matches();
		final int x = Integer.parseInt( matcher.group( "col" ) ) - 1;
		final int y = Integer.parseInt( matcher.group( "row" ) ) - 1;
		return new int[]{ x, y };
	}

	private static int[] decodeInCellWellPosition( String well )
	{
		// (?<"+WELL+">[A-Z]{1} - [0-9]{2})
		final Matcher matcher = Pattern.compile( "(?<row>[A-Z]{1}) - (?<col>[0-9]{2})" ).matcher( well );
		matcher.matches();
		final int x = Integer.parseInt( matcher.group( "col" ) ) - 1;
		final int y = ALPHABET.indexOf( matcher.group( "row" ).toUpperCase() );
		return new int[]{ x, y };
	}

	public static int[] decodeA01WellPosition( String well )
	{
		final int length = well.length();
		final int x = Integer.parseInt( well.substring( 1, length ) ) - 1;
		final int y = ALPHABET.indexOf( well.substring( 0, 1 ).toUpperCase() );
		return new int[]{ x, y };
	}

	private boolean hasChannels()
	{
		switch ( this )
		{
			case OMEZarr:
			case Operetta:
			case MolecularDevices:
			case IncuCyteRaw:
			case InCarta:
			case Araceli:
				return true;
			default:
				return false;
		}
	}

	public boolean hasT()
	{
		switch ( this )
		{
			case OMEZarr:
			case Operetta:
			case MolecularDevices:
			case InCell:
			case Araceli:
			case InCarta: // TODO could be made true
				return false;
			default:
				return true;
		}
	}

	/**
	 *
	 * @return boolean indicating whether there are multiple z-positions distributed over multiple files
	 */
	public boolean hasZ()
	{
		switch ( this )
		{
			case YokogawaCQ1:
			case InCarta:
				return true;
			default:
				return false;
		}
	}

	public String getChannelGroup()
	{
		if ( hasChannels() )
			return matcher.group( HCSPattern.CHANNEL );
		else
			return "1" ;
	}

	public List< String > getChannels()
	{
		if ( hasChannels() )
			if (this == OMEZarr)
				return channels;
			else
				return Collections.singletonList( matcher.group( HCSPattern.CHANNEL ) );
		else
			return Collections.singletonList( "1" );
	}

	public String getWellGroup()
	{
		return matcher.group( HCSPattern.WELL );
	}

	public String getSiteGroup()
	{
		return matcher.group( HCSPattern.SITE );
	}

	public String getT()
	{
		if ( hasT() )
			return matcher.group( HCSPattern.TIME );
		else
			return "1";
	}

	public String getZ()
	{
		if ( hasZ() )
			return matcher.group( HCSPattern.SLICE );
		else
			return "1";
	}

	public void setChannelNames( List< String > channels )
	{
		this.channels = channels;
	}

}
