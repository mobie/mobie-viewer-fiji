/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2022 EMBL
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

import bdv.util.BdvHandle;
import com.google.gson.Gson;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.bdv.utils.Logger;
import org.embl.mobie.lib.playground.BdvPlaygroundHelper;
import org.embl.mobie.lib.serialize.JsonHelper;
import org.embl.mobie.lib.transform.AffineViewerTransform;
import org.embl.mobie.lib.transform.NormalVectorViewerTransform;
import org.embl.mobie.lib.transform.NormalizedAffineViewerTransform;
import org.embl.mobie.lib.transform.PositionViewerTransform;
import org.embl.mobie.lib.transform.TransformHelper;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;

@Plugin( type = BdvPlaygroundActionCommand.class, name = ViewerTransformLoggerCommand.NAME, menuPath = CommandConstants.CONTEXT_MENU_ITEMS_ROOT + ViewerTransformLoggerCommand.NAME )
public class ViewerTransformLoggerCommand implements BdvPlaygroundActionCommand
{
	public static final String NAME = "Log Current Location";

	@Parameter
	BdvHandle bdv;

	@Override
	public void run()
	{
		new Thread( () -> {

			final int timepoint = bdv.getViewerPanel().state().getCurrentTimepoint();
			final double[] position = BdvPlaygroundHelper.getWindowCentreInCalibratedUnits( bdv );

			// position
			final PositionViewerTransform positionViewerTransform = new PositionViewerTransform( position, timepoint );

			// affine
			final AffineTransform3D affineTransform3D = new AffineTransform3D();
			bdv.getViewerPanel().state().getViewerTransform( affineTransform3D );
			final AffineViewerTransform affineViewerTransform = new AffineViewerTransform( affineTransform3D.getRowPackedCopy(), timepoint );

			// normalized affine
			final AffineTransform3D normalisedViewerTransform = TransformHelper.createNormalisedViewerTransform( bdv.getViewerPanel() );
			final NormalizedAffineViewerTransform normalizedAffineViewerTransform = new NormalizedAffineViewerTransform( normalisedViewerTransform.getRowPackedCopy(), timepoint );

			// normal vector
			double[] currentNormalVector = BdvUtils.getCurrentViewNormalVector( bdv );
			final NormalVectorViewerTransform normalVectorViewerTransform = new NormalVectorViewerTransform( currentNormalVector, timepoint );

			// print
			final Gson gson = JsonHelper.buildGson( false );
			Logger.log( "# Current view " );
			Logger.log( "To restore the view, any of below lines can be pasted into the \'location\' text field." );
			Logger.log( "To share views with other people we recommend \'normalizedAffine\'." );

			Logger.log( gson.toJson( positionViewerTransform ) );
			Logger.log( gson.toJson( affineViewerTransform ) );
			Logger.log( gson.toJson( normalizedAffineViewerTransform ) );
			Logger.log( gson.toJson( normalVectorViewerTransform ) );

		} ).start();
	}
}
