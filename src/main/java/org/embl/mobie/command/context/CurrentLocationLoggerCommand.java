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
package org.embl.mobie.command.context;

import bdv.util.BdvHandle;
import com.google.gson.Gson;
import ij.IJ;
import net.imglib2.util.LinAlgHelpers;
import org.embl.mobie.command.CommandConstants;
import org.embl.mobie.lib.playground.BdvPlaygroundHelper;
import org.embl.mobie.lib.serialize.JsonHelper;
import org.embl.mobie.lib.transform.viewer.AffineViewerTransform;
import org.embl.mobie.lib.transform.viewer.NormalVectorViewerTransform;
import org.embl.mobie.lib.serialize.transformation.NormalizedAffineViewerTransform;
import org.embl.mobie.lib.transform.viewer.PositionViewerTransform;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;

import static org.embl.mobie.lib.util.MoBIEHelper.getCurrentViewNormalVector;

@Plugin( type = BdvPlaygroundActionCommand.class, name = CurrentLocationLoggerCommand.NAME, menuPath = CommandConstants.CONTEXT_MENU_ITEMS_ROOT + CurrentLocationLoggerCommand.NAME )
public class CurrentLocationLoggerCommand implements BdvPlaygroundActionCommand
{
	private static PositionViewerTransform mousePointerPositionTransform;

	static { net.imagej.patcher.LegacyInjector.preinit(); }

	public static final String NAME = "Log Current Location [ C ]";

	public static final String SHORTCUT = "C";

	@Parameter
	BdvHandle bdvHandle;

	@Override
	public void run()
	{
		new Thread( () ->
		{
			logCurrentPosition( bdvHandle,
					BdvPlaygroundHelper.getWindowCentreInCalibratedUnits( bdvHandle ),
					null );
		} ).start();
	}

	public static void logCurrentPosition( BdvHandle bdvHandle,
										   double[] windowCentre,
										   double[] mousePointer )
	{

		// position
		final int timePoint = bdvHandle.getViewerPanel().state().getCurrentTimepoint();
		final PositionViewerTransform positionViewerTransform = new PositionViewerTransform( windowCentre, timePoint );

		// affine
		final AffineTransform3D affineTransform3D = new AffineTransform3D();
		bdvHandle.getViewerPanel().state().getViewerTransform( affineTransform3D );
		final AffineViewerTransform affineViewerTransform = new AffineViewerTransform( affineTransform3D.getRowPackedCopy(), timePoint );

		// normalized affine
		final NormalizedAffineViewerTransform normalizedAffineViewerTransform = new NormalizedAffineViewerTransform( bdvHandle );

		// normal vector
		double[] currentNormalVector = getCurrentViewNormalVector( bdvHandle );
		final NormalVectorViewerTransform normalVectorViewerTransform = new NormalVectorViewerTransform( currentNormalVector, timePoint );

		// print
		final Gson gson = JsonHelper.buildGson( false );
		IJ.log( "" );
		IJ.log( "# Current location" );
		IJ.log( "To restore the current location, any of the below {...} JSON strings can be pasted into MoBIE's \"location\"  field." );
		IJ.log( "To share views with other people we recommend using the normalised viewer transform." );

		IJ.log("## Mouse pointer position" );
		Double distanceToRecentPosition = null;
		if ( mousePointer == null )
		{
			IJ.log( "Only given when using the [" + SHORTCUT + "] keyboard shortcut in the BDV window to trigger this action." );
		}
		else
		{
			if ( mousePointerPositionTransform != null )
			{
				distanceToRecentPosition = LinAlgHelpers.distance( mousePointerPositionTransform.getParameters(), mousePointer );
			}
			mousePointerPositionTransform = new PositionViewerTransform( mousePointer, timePoint );
			IJ.log( gson.toJson( mousePointerPositionTransform ) );
		}
		IJ.log("## Window center position" );
		IJ.log( gson.toJson( positionViewerTransform ) );
		IJ.log("## Viewer transform" );
		IJ.log( gson.toJson( affineViewerTransform ) );
		IJ.log("## Normalised viewer transform" );
		IJ.log( gson.toJson( normalizedAffineViewerTransform ) );
		IJ.log("## Normal vector" );
		IJ.log( gson.toJson( normalVectorViewerTransform ) );

		if ( distanceToRecentPosition != null )
		{
			IJ.log("" );
			IJ.log( "Distance between current and most recent mouse position: " +  distanceToRecentPosition );
		}
	}
}
