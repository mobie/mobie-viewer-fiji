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
package org.embl.mobie.command;

import ij.IJ;
import org.embl.mobie.io.ImageDataOpener;
import org.embl.mobie.lib.data.DataStore;
import org.scijava.Initializable;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.command.Interactive;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.command.BdvPlaygroundActionCommand;

@Plugin(type = Command.class, menuPath = CommandConstants.MOBIE_PLUGIN_ROOT + "Settings>Configure MoBIE Settings...")
public class ConfigureMoBIESettingsCommand extends DynamicCommand implements Initializable
{
	static { net.imagej.patcher.LegacyInjector.preinit(); }

	// needed because enums do not persist in the SciJava parameters.
	private static final String GUI_MOBIE_N5 = "MoBIE N5";
	private static final String GUI_OME_ZARR_N5 = "OME-Zarr N5";
	private static final String GUI_OME_ZARR_JAVA = "OME-Zarr zarr-java";

	@Parameter(
			label = "OME-Zarr reader",
			choices = { GUI_MOBIE_N5, GUI_OME_ZARR_N5, GUI_OME_ZARR_JAVA },
			description = "Select which backend should open OME-Zarr data. " +
					"AutoDetect chooses a backend based on available dependencies."
	)
	public String zarrOpener = toGuiValue( ImageDataOpener.getZarrOpener() );

	@Override
	public void run()
	{
		final ImageDataOpener.ZarrOpener opener = toEnumValue( zarrOpener );

		if ( opener != ImageDataOpener.getZarrOpener() )
			DataStore.clearImageDataCache();

		ImageDataOpener.setZarrOpener( opener );
		IJ.log( "New OME-Zarr Reader: " + toGuiValue( ImageDataOpener.getZarrOpener() ) ) ;
	}

	@Override
	public void initialize()
	{
		IJ.log( "Current OME-Zarr Reader: " + toGuiValue( ImageDataOpener.getZarrOpener() ) ) ;
	}

	private static String toGuiValue( ImageDataOpener.ZarrOpener opener )
	{
		switch ( opener )
		{
			case MOBIE_N5:
				return GUI_MOBIE_N5;
			case OME_Zarr_N5:
				return GUI_OME_ZARR_N5;
			case OME_Zarr_Zarr_Java:
				return GUI_OME_ZARR_JAVA;
			default:
				throw new IllegalArgumentException( "Unsupported OME-Zarr opener: " + opener );
		}
	}

	private static ImageDataOpener.ZarrOpener toEnumValue( String opener )
	{
		switch ( opener )
		{
			case GUI_MOBIE_N5:
				return ImageDataOpener.ZarrOpener.MOBIE_N5;
			case GUI_OME_ZARR_N5:
				return ImageDataOpener.ZarrOpener.OME_Zarr_N5;
			case GUI_OME_ZARR_JAVA:
				return ImageDataOpener.ZarrOpener.OME_Zarr_Zarr_Java;
			default:
				throw new IllegalArgumentException( "Unsupported GUI OME-Zarr opener: " + opener );
		}
	}

	private boolean ensureBackendAvailable( String backendName, String className )
	{
		if ( isClassAvailable( className ) ) return true;

		IJ.showMessage(
				"OME-Zarr backend unavailable",
				"The selected OME-Zarr reader '" + backendName + "' is not available in this Fiji installation.\n" +
						"Missing class: " + className + "\n\n" +
						"To use this OME-Zarr reader please enable the OME-Zarr update site (Help > Update), update Fiji, and restart."
		);
		return false;
	}

	private boolean isClassAvailable( String className )
	{
		try
		{
			Class.forName( className );
			return true;
		}
		catch ( ClassNotFoundException | LinkageError e )
		{
			return false;
		}
	}
}
