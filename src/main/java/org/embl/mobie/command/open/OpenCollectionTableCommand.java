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
package org.embl.mobie.command.open;

import loci.common.DebugTools;
import org.embl.mobie.MoBIESettings;
import org.embl.mobie.command.open.special.OpenSimpleCollectionTableCommand;
import org.embl.mobie.io.util.IOHelper;
import org.embl.mobie.lib.data.ProjectType;
import org.embl.mobie.command.CommandConstants;
import org.embl.mobie.lib.util.MoBIEHelper;
import org.embl.mobie.lib.bdv.BdvViewingMode;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;

@Plugin(type = Command.class, menuPath = CommandConstants.MOBIE_PLUGIN_OPEN + "Open Collection Table..." )
public class OpenCollectionTableCommand extends OpenSimpleCollectionTableCommand
{

	// Don't change those Strings to stay compatible with recorded macros
	public static final String ABSOLUTE = "PathsInTableAreAbsolute";
	public static final String RELATIVE_TO_TABLE = "UseTableFolder";
	public static final String RELATIVE_TO_FOLDER = "UseBelowDataRootFolder";

	static { net.imagej.patcher.LegacyInjector.preinit(); }

	public enum DataRootType
	{
		PathsInTableAreAbsolute,
		UseTableFolder,
		UseBelowDataRootFolder
	}


	@Parameter( label = "Data Root",
			choices = { ABSOLUTE, RELATIVE_TO_TABLE, RELATIVE_TO_FOLDER },
			description = "Specify whether the data URIs in the table are absolute or relative." )
	public String dataRootType; // important to keep the variable name the same for Macro recording

	// Does not work yet properly as a parameter:
	// https://github.com/scijava/scijava-common/issues/471
	public DataRootType dataRootTypeEnum;

	@Parameter( label = "( Data Root Folder )",
			style = "directory",
			description = "Optional. Use this is if the paths to the images and labels in the table are relative.",
			required = false )
	public File dataRoot;

	@Parameter( label = "Viewing mode",
			choices = {"ThreeDimensional", "TwoDimensional"},
			description = "ThreeDimensional viewing enables arbitrary plane slicing.\n" +
					"TwoDimensional viewing mode will restrict browsing to the XY, YZ, or XZ planes.",
			required = false )
	public String bdvViewingMode; // important to keep the variable name the same for Macro recording

	// Does not work yet properly as a parameter:
	// https://github.com/scijava/scijava-common/issues/471
	public BdvViewingMode bdvViewingModeEnum;

	@Parameter ( label = "( S3 Access Key )",
			description = "Optional. Access key for a protected S3 bucket.",
			persist = false,
			required = false )
	public String s3AccessKey;

	@Parameter ( label = "( S3 Secret Key )",
			description = "Optional. Secret key for a protected S3 bucket.",
			persist = false,
			required = false )
	public String s3SecretKey;

	@Override
	public void run()
	{
		DebugTools.setRootLevel( "OFF" );

		dataRootTypeEnum = dataRootTypeEnum == null ? DataRootType.valueOf( dataRootType ) : dataRootTypeEnum;
		bdvViewingModeEnum = bdvViewingModeEnum == null ? BdvViewingMode.valueOf( bdvViewingMode ) : bdvViewingModeEnum;

		String dataRootString;
		switch ( dataRootTypeEnum )
		{
			case UseBelowDataRootFolder:
				dataRootString = dataRoot == null ? null : dataRoot.getAbsolutePath();
				break;
			case UseTableFolder:
				dataRootString = IOHelper.getParentLocation( tableUri );
				break;
			case PathsInTableAreAbsolute:
			default:
				dataRootString = null;
				break;
		}

		final MoBIESettings settings = new MoBIESettings()
				.projectType( ProjectType.CollectionTable )
				.dataRoot( dataRootString )
				.bdvViewingMode( bdvViewingModeEnum );

		if ( MoBIEHelper.notNullOrEmpty( s3AccessKey ) )
			settings.s3AccessAndSecretKey( new String[]{ s3AccessKey, s3SecretKey } );

		openTable( tableUri, settings );
	}
}
