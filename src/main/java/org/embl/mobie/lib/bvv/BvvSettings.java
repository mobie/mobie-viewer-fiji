package org.embl.mobie.lib.bvv;

import IceInternal.Ex;
import ij.IJ;
import org.embl.mobie.command.context.ConfigureBVVRenderingCommand;
import org.scijava.command.CommandInfo;
import org.scijava.command.CommandModuleItem;
import org.scijava.command.CommandService;
import org.scijava.module.ModuleItem;
import org.scijava.module.ModuleService;

import ch.epfl.biop.bdv.img.Services;

public class BvvSettings
{
	//BVV canvas rendering parameters, can be changed/adjusted somewhere else
	
	//parameters that can be changed at runtime
	static double dCam = 2000.;
	static double dClipNear = 1000.;
	static double dClipFar = 15000.;			
	
	// parameters that require bvv restart, 
	// see https://github.com/ekatrukha/BigTrace/wiki/Volume-Render-Settings
	static int renderWidth = 800;
	static int renderHeight = 600;
	static int numDitherSamples = 3; 
	static int cacheBlockSize = 32;
	static int maxCacheSizeInMB = 500;
	static int ditherWidth = 3;
	
	/** Reads current BVV rendering settings from the corresponding SciJava command.
	 * Returns true if new settings require BVV restart **/
	@SuppressWarnings( "unchecked" )
	public static boolean readBVVRenderSettings()
	{
		try
		{
			boolean bRestartBVV = false;

			final CommandService cs = Services.commandService;

			final ModuleService ms = Services.commandService.moduleService();

			final CommandInfo info = cs.getCommand( ConfigureBVVRenderingCommand.class );

			dCam = Math.abs( ( double ) getSettingsValue( info, ms, "dCam" ) );

			dClipFar = Math.abs( ( double ) getSettingsValue( info, ms, "dClipFar" ) );

			dClipNear = Math.abs( ( double ) getSettingsValue( info, ms, "dClipNear" ) );

			//dClipNear should be less than dCam
			if ( dCam < dClipNear )
			{
				dCam = dClipNear + 5.0;
				ms.save( ( ModuleItem< Double > ) info.getInput( "dCam" ), dCam );
			}

			int nTempInt = ( int ) getSettingsValue( info, ms, "renderWidth" );
			if ( renderWidth != nTempInt )
			{
				bRestartBVV = true;
			}
			renderWidth = nTempInt;

			nTempInt = ( int ) getSettingsValue( info, ms, "renderHeight" );
			if ( renderHeight != nTempInt )
			{
				bRestartBVV = true;
			}
			renderHeight = nTempInt;

			nTempInt = ( int ) getSettingsValue( info, ms, "numDitherSamples" );
			if ( numDitherSamples != nTempInt )
			{
				bRestartBVV = true;
			}
			numDitherSamples = nTempInt;

			nTempInt = ( int ) getSettingsValue( info, ms, "cacheBlockSize" );
			if ( cacheBlockSize != nTempInt )
			{
				bRestartBVV = true;
			}
			cacheBlockSize = nTempInt;

			nTempInt = ( int ) getSettingsValue( info, ms, "maxCacheSizeInMB" );
			if ( maxCacheSizeInMB != nTempInt )
			{
				bRestartBVV = true;
			}
			maxCacheSizeInMB = nTempInt;

			String dithering = ( String ) getSettingsValue( info, ms, "dithering" );
			final int ditherWidthIn;
			switch ( dithering )
			{
				case "none (always render full resolution)":
				default:
					ditherWidthIn = 1;
					break;
				case "2x2":
					ditherWidthIn = 2;
					break;
				case "3x3":
					ditherWidthIn = 3;
					break;
				case "4x4":
					ditherWidthIn = 4;
					break;
				case "5x5":
					ditherWidthIn = 5;
					break;
				case "6x6":
					ditherWidthIn = 6;
					break;
				case "7x7":
					ditherWidthIn = 7;
					break;
				case "8x8":
					ditherWidthIn = 8;
					break;
			}

			if ( ditherWidth != ditherWidthIn )
			{
				bRestartBVV = true;
			}

			ditherWidth = ditherWidthIn;

			return bRestartBVV;
		}
		catch ( Exception e )
		{
			IJ.log("[WARN]: Could not fetch BVV rendering settings");
			return false;
		}
	}
	
	@SuppressWarnings( "unchecked" )
	public static <T> T getSettingsValue( final CommandInfo info, final ModuleService ms, String name) 
	{
		CommandModuleItem< T > item = ( CommandModuleItem< T > ) info.getInput( name );
		if ( ms.load( item ) == null )
		{
			return ms.getDefaultValue( item );
		}
		return ms.load( item );
	}
}
