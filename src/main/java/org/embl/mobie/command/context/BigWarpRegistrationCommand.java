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

import bdv.tools.brightness.ConverterSetup;
import bdv.viewer.BigWarpViewerPanel;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.TransformListener;
import bigwarp.BigWarp;
import bigwarp.landmarks.LandmarkTableModel;
import bigwarp.transforms.BigWarpTransform;
import org.embl.mobie.MoBIE;
import org.embl.mobie.command.CommandConstants;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.InvertibleRealTransform;
import org.embl.mobie.lib.data.ProjectType;
import org.embl.mobie.lib.image.Image;
import org.embl.mobie.lib.serialize.View;
import org.embl.mobie.lib.serialize.transformation.AffineTransformation;
import org.embl.mobie.lib.serialize.transformation.TpsTransformation;
import org.embl.mobie.lib.util.MoBIEHelper;
import org.embl.mobie.lib.view.ViewManager;
import org.embl.mobie.ui.UserInterfaceHelper;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.Button;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterBdvDisplayService;
import sc.fiji.bdvpg.services.ISourceAndConverterService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.register.BigWarpLauncher;

import java.util.*;
import java.util.stream.Collectors;

@Plugin(type = BdvPlaygroundActionCommand.class, menuPath = CommandConstants.CONTEXT_MENU_ITEMS_ROOT + "Transform>" + BigWarpRegistrationCommand.COMMAND_NAME )
public class BigWarpRegistrationCommand extends AbstractRegistrationCommand implements TransformListener< InvertibleRealTransform >
{
	public static final String COMMAND_NAME = "Registration - BigWarp";

	static { net.imagej.patcher.LegacyInjector.preinit(); }

	@Parameter ( label = "Launch BigWarp", callback = "launchBigWarp" )
	private Button launchBigWarp;

	@Parameter ( label = "Apply transform", callback = "applyTransform" )
	private Button applyTransform;

	private BigWarp< ? > bigWarp;


	@Override
	public void initialize()
	{
		super.initialize();

		getInfo().getMutableInput( "suffix", String.class )
				.setValue( this, "bw");
	}

	public void applyTransform()
	{
		if ( bigWarp.getTransformType().equals( BigWarpTransform.AFFINE ) )
		{
			applyTransform( bigWarp.getBwTransform().affine3d().inverse() );
		}
		else if ( bigWarp.getTransformType().equals( BigWarpTransform.TPS ) )
		{
			boolean applied = applyTpsTransform( bigWarp.getLandmarkPanel().getTableModel() );
			if ( applied ) removeMovingImages();
			else resetTransforms();
		}

		bigWarp.closeAll();
		UserInterfaceHelper.closeWindowByName( COMMAND_NAME );
	}

	private boolean applyTpsTransform( LandmarkTableModel tableModel )
	{
		for ( Image< ? > movingImage : movingImages )
		{
			String transformedImageName = movingImage.getName();
			if ( ! suffix.isEmpty() )
				transformedImageName += "-" + suffix;

			TpsTransformation transformation = new TpsTransformation(
					suffix,
					tableModel.toJson().toString(),
					Collections.singletonList( movingImage.getName() ),
					Collections.singletonList( transformedImageName )
			);

			if ( createImageView( movingImage, transformedImageName, transformation ) ) return false;
		}

		return true;
	}

	public void launchBigWarp()
	{
		setMovingImages();

		ISourceAndConverterService sacService = SourceAndConverterServices.getSourceAndConverterService();
		SourceAndConverterBdvDisplayService bdvDisplayService = SourceAndConverterServices.getBdvDisplayService();

		SourceAndConverter< ? > fixedSac = sacs.stream()
				.filter( sac -> sac.getSpimSource().getName().equals( fixedImageName ) )
				.findFirst().get();

		List< ConverterSetup > converterSetups = movingSacs.stream()
				.map( sacService::getConverterSetup )
				.collect( Collectors.toList() );

		converterSetups.add( sacService.getConverterSetup( fixedSac ) );

		BigWarpLauncher bigWarpLauncher = new BigWarpLauncher(
				new ArrayList<>( movingSacs ),
				Collections.singletonList( fixedSac ),
				"MoBIE Big Warp",
				converterSetups);
		bigWarpLauncher.run();

		bdvDisplayService.pairClosing( bigWarpLauncher.getBdvHandleQ(), bigWarpLauncher.getBdvHandleP() );

		bigWarp = bigWarpLauncher.getBigWarp();

		final AffineTransform3D normalisedViewerTransform = MoBIEHelper.createNormalisedViewerTransform( bdvHandle.getViewerPanel() );
		applyViewerTransform( normalisedViewerTransform, bigWarp.getViewerFrameQ().getViewerPanel() );
		applyViewerTransform( normalisedViewerTransform, bigWarp.getViewerFrameP().getViewerPanel() );
		bigWarp.setTransformType( BigWarpTransform.AFFINE );
		bigWarp.addTransformListener( this );
	}


	private void applyViewerTransform( AffineTransform3D normalisedViewerTransform, BigWarpViewerPanel viewerPanel )
	{
		viewerPanel.state().setViewerTransform( MoBIEHelper.createUnnormalizedViewerTransform( normalisedViewerTransform, viewerPanel ) );
	}

	@Override
	public void cancel()
	{
		resetTransforms();
		bigWarp.closeAll();
	}

	@Override
	public void transformChanged( InvertibleRealTransform transform )
	{
		final String transformType = bigWarp.getTransformType();
		if ( transformType.equals( BigWarpTransform.TPS ) )
		{
			System.out.println( transformType );
		}
	}

}
