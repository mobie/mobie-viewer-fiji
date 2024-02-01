/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2023 EMBL
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

import bdv.gui.TransformTypeSelectDialog;
import bdv.tools.brightness.ConverterSetup;
import bdv.viewer.BigWarpViewerPanel;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.TransformListener;
import bigwarp.BigWarp;
import org.embl.mobie.command.CommandConstants;
import org.embl.mobie.lib.transform.TransformHelper;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.InvertibleRealTransform;
import org.embl.mobie.lib.transform.TransformationOutput;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.Button;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterBdvDisplayService;
import sc.fiji.bdvpg.services.ISourceAndConverterService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.register.BigWarpLauncher;

import java.util.*;

@Plugin(type = BdvPlaygroundActionCommand.class, menuPath = CommandConstants.CONTEXT_MENU_ITEMS_ROOT + "Transform>Registration - BigWarp")
public class BigWarpRegistrationCommand extends AbstractRegistrationCommand implements TransformListener< InvertibleRealTransform >
{
	static { net.imagej.patcher.LegacyInjector.preinit(); }

	@Parameter ( label = "Launch BigWarp", callback = "launchBigWarp")
	private Button launchBigWarp;

	@Parameter ( label = "Preview current transform", callback = "previewTransform")
	private Button previewTransform;

	@Parameter ( label = "Apply current transform and exit", callback = "applyTransform")
	private Button applyTransform;

	private BigWarp< ? >bigWarp;


	@Override
	public void initialize()
	{
		super.initialize();
	}

	public void previewTransform()
	{
		applyTransformInPlace( bigWarp.getBwTransform().affine3d() );
		bdvHandle.getViewerPanel().requestRepaint();
	}


	public void applyTransform()
	{
		movingSource.setFixedTransform( previousFixedTransform );

		if ( mode.equals( TransformationOutput.TransformMovingImage ) )
		{
			applyTransformInPlace( bigWarp.getBwTransform().affine3d() );
		}
		else if ( mode.equals( TransformationOutput.CreateNewImage ) )
		{
			createTransformedImage( bigWarp.getBwTransform().affine3d(), "BigWarp " + bigWarp.getTransformType() );
		}

		bdvHandle.getViewerPanel().requestRepaint();
		bigWarp.closeAll();
	}


	public void launchBigWarp()
	{
		ISourceAndConverterService sacService = SourceAndConverterServices.getSourceAndConverterService();
		SourceAndConverterBdvDisplayService bdvDisplayService = SourceAndConverterServices.getBdvDisplayService();

		SourceAndConverter< ? > fixedSac = sourceAndConverters.stream()
				.filter( sac -> sac.getSpimSource().getName().equals( fixedImageName ) )
				.findFirst().get();

		List< ConverterSetup > converterSetups = new ArrayList<>();
		converterSetups.add( sacService.getConverterSetup( movingSac ) );
		converterSetups.add( sacService.getConverterSetup( fixedSac ) );

		BigWarpLauncher bigWarpLauncher = new BigWarpLauncher(
				Collections.singletonList( movingSac ),
				Collections.singletonList( fixedSac ),
				"MoBIE Big Warp",
				converterSetups);
		bigWarpLauncher.run();

		bdvDisplayService.pairClosing( bigWarpLauncher.getBdvHandleQ(), bigWarpLauncher.getBdvHandleP() );

		bigWarp = bigWarpLauncher.getBigWarp();

		final AffineTransform3D normalisedViewerTransform = TransformHelper.createNormalisedViewerTransform( bdvHandle.getViewerPanel() );
		applyViewerTransform( normalisedViewerTransform, bigWarp.getViewerFrameQ().getViewerPanel() );
		applyViewerTransform( normalisedViewerTransform, bigWarp.getViewerFrameP().getViewerPanel() );
		bigWarp.setTransformType( TransformTypeSelectDialog.AFFINE );
		bigWarp.addTransformListener( this );
	}


	private void applyViewerTransform( AffineTransform3D normalisedViewerTransform, BigWarpViewerPanel viewerPanel )
	{
		viewerPanel.state().setViewerTransform( TransformHelper.createUnnormalizedViewerTransform( normalisedViewerTransform, viewerPanel ) );
	}

	@Override
	public void cancel()
	{
		movingSource.setFixedTransform( previousFixedTransform );
		bigWarp.closeAll();
	}

	@Override
	public void transformChanged( InvertibleRealTransform transform )
	{
		final String transformType = bigWarp.getTransformType();
		if ( transformType.equals( TransformTypeSelectDialog.TPS ) )
		{
			System.err.println( TransformTypeSelectDialog.TPS + "is currently not supported by MoBIE please choose any of the other transform types by selecting one of the BigWarp windows and pressing F2.");
		}
	}

}
