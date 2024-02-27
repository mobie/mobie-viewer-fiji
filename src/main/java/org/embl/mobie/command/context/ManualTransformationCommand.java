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

import bdv.viewer.SourceAndConverter;
import org.embl.mobie.DataStore;
import org.embl.mobie.command.CommandConstants;
import org.embl.mobie.command.MoBIEManualTransformationEditor;
import org.embl.mobie.lib.image.Image;
import org.embl.mobie.lib.image.RegionAnnotationImage;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.Button;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Plugin(type = BdvPlaygroundActionCommand.class, menuPath = CommandConstants.CONTEXT_MENU_ITEMS_ROOT + "Transform>Registration - Manual")
public class ManualTransformationCommand extends AbstractTransformationCommand
{
	static { net.imagej.patcher.LegacyInjector.preinit(); }

	@Parameter ( label = "Start manual transform", callback = "startManualTransform" )
	public Button startManualTransform;

	@Parameter ( label = "Accept manual transform", callback = "acceptManualTransform" )
	public Button acceptManualTransform;

	@Parameter ( label = "Cancel manual transform", callback = "cancelManualTransform" )
	public Button cancelManualTransform;

	private List< Image< ? > > transformableImages;
	private MoBIEManualTransformationEditor transformationEditor;

	@Override
	public void initialize()
	{
		super.initialize();
	}


	public void startManualTransform()
	{
		Image< ? > image = DataStore.sourceToImage().get( movingSource );

		List< SourceAndConverter< ? > > movingSACs;

		if ( image instanceof RegionAnnotationImage &&
				!( ( RegionAnnotationImage< ? > ) image ).getSelectedImages().isEmpty() )
		{
			transformableImages = ( ( RegionAnnotationImage< ? > ) image ).getSelectedImages();

			movingSACs = transformableImages.stream()
					.map( img -> DataStore.sourceToImage().inverse().get( img ) )
					.collect( Collectors.toList() );
		}
		else
		{
			movingSACs = Collections.singletonList( movingSac );
		}


		transformationEditor = new MoBIEManualTransformationEditor( bdvHandle.getViewerPanel(), bdvHandle.getKeybindings() );
		transformationEditor.setTransformableSources( movingSACs );
		transformationEditor.setActive( true );
	}

	private void acceptManualTransform()
	{
		if ( transformationEditor == null ) return;

		applyAffineTransform3D( transformationEditor.getManualTransform(), "manual-affine" );

		// This will cause the transformed image to jump back to its original position,
		// but this is intended as the transfomerd image is now a new image that is stored as a new view.
		transformationEditor.setActive( false );
	}

	private void cancelManualTransform()
	{
		if ( transformationEditor == null ) return;

		transformationEditor.setActive( false );
	}
}