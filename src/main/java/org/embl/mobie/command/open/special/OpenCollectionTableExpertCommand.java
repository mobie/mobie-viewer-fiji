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
package org.embl.mobie.command.open.special;

import loci.common.DebugTools;
import mpicbg.spim.data.sequence.FinalVoxelDimensions;
import org.embl.mobie.MoBIESettings;
import org.embl.mobie.command.open.OpenCollectionTableCommand;
import org.embl.mobie.io.util.IOHelper;
import org.embl.mobie.lib.data.ProjectType;
import org.embl.mobie.command.CommandConstants;
import org.embl.mobie.lib.util.MoBIEHelper;
import org.embl.mobie.lib.bdv.BdvViewingMode;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;

@Plugin(type = Command.class, menuPath = CommandConstants.MOBIE_PLUGIN_OPEN_SPECIAL + "Open MoBIE Collection Table Expert Mode..." )
public class OpenCollectionTableExpertCommand extends OpenCollectionTableCommand
{
	// Don't change those Strings to stay compatible with recorded macros
	public static final String RELATIVE_TO_TABLE = "UseTableFolder";
	public static final String RELATIVE_TO_FOLDER = "UseBelowDataRootFolder";

	static { net.imagej.patcher.LegacyInjector.preinit(); }

	@Parameter( label = "Use Pixel Units for all Images",
			description = "Checking this will remove all spatial calibration\n" +
					"that is read from the image metadata." )
	public Boolean usePixelUnits;

	public enum DataRootType
	{
		PathsInTableAreAbsolute, // FIXME: Remove this, not needed anymore
		UseTableFolder,
		UseBelowDataRootFolder
	}

	@Parameter( label = "Data Root for Relative Paths",
			choices = { RELATIVE_TO_TABLE, RELATIVE_TO_FOLDER },
			description = "Specify which data root to prepend for relative paths.\n" +
					"This will do nothing if all your paths are absolute." )
	public String dataRootType; // important to keep the variable name the same for Macro recording

	// Does not work yet properly as a parameter:
	// https://github.com/scijava/scijava-common/issues/471
	public DataRootType dataRootTypeEnum;

	@Parameter( label = "( Data Root Folder )",
			style = "directory",
			description = "Optional. Use this is if the paths to the images and labels in the table are relative.",
			required = false )
	public File dataRoot;

	public BdvViewingMode bdvViewingModeEnum;


	@Override
	public void run()
	{
		DebugTools.setRootLevel( "OFF" );

		try
		{
			dataRootTypeEnum = dataRootTypeEnum == null ? DataRootType.valueOf( dataRootType ) : dataRootTypeEnum;
		}
		catch ( Exception e )
		{
			dataRootTypeEnum = DataRootType.UseTableFolder;
		}

		String dataRootString;
		switch ( dataRootTypeEnum )
		{
			case UseBelowDataRootFolder:
				dataRootString = dataRoot == null ? null : dataRoot.getAbsolutePath();
				break;
            default:
				dataRootString = IOHelper.getParentLocation( tableUri );
				break;
		}

		final MoBIESettings settings = new MoBIESettings()
				.projectType( ProjectType.CollectionTable )
				.dataRoot( dataRootString )
				.bdvViewingMode( bdvViewingModeEnum );

		if ( usePixelUnits )
			settings.setVoxelDimensions(
					new FinalVoxelDimensions( "pixel", 1.0, 1.0, 1.0 ) );

		if ( MoBIEHelper.notNullOrEmpty( s3AccessKey ) )
			settings.s3AccessAndSecretKey( new String[]{ s3AccessKey, s3SecretKey } );

		openTable( tableUri, settings );
	}
}
