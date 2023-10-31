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

import bdv.tools.transformation.ManualTransformActiveListener;
import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import ij.gui.NonBlockingGenericDialog;
import net.imglib2.realtransform.AffineTransform3D;
import org.embl.mobie.DataStore;
import org.embl.mobie.command.CommandConstants;
import org.embl.mobie.command.ManualTransformationEditor;
import org.embl.mobie.lib.image.Image;
import org.embl.mobie.lib.image.RegionAnnotationImage;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import java.util.List;

@Plugin(type = BdvPlaygroundActionCommand.class, menuPath = CommandConstants.CONTEXT_MENU_ITEMS_ROOT + "Transform>Registration - Manual")
public class ManualRegistrationCommand implements BdvPlaygroundActionCommand, ManualTransformActiveListener
{
	static { net.imagej.patcher.LegacyInjector.preinit(); }

	@Parameter
	protected BdvHandle bdvh;

	@Parameter
	protected SourceAndConverter< ? >[] sourceAndConverters;
	private List< Image< ? > > transformableImages;

	@Override
	public void run()
	{
		new Thread( () -> showDialog() ).start();
	}

	private void showDialog()
	{
		// TODO https://imagesc.zulipchat.com/#narrow/stream/327326-BigDataViewer/topic/Manual.20transform.20editor
		final NonBlockingGenericDialog dialog = new NonBlockingGenericDialog( "Registration - Manual" );
		dialog.hideCancelButton();
		dialog.addMessage( "Manual translation, rotation and scaling transformations.\n\n" +
				"- Select the BDV window and press P and select the source to be transformed as the current source\n" +
				"- Press T to start the manual transform mode\n" +
				"  - While in manual transform mode the mouse and keyboard actions that normally change the view will now transform the current source\n" +
				"  - For example, right mouse button and mouse drag will translate the source\n\n" +
				"Press [ T ] again to fix the transformation\n" +
				"Press [ ESC ] to abort the transformation");
		dialog.showDialog();

		SourceAndConverter< ? > currentSource = bdvh.getViewerPanel().state().getCurrentSource();
		// FIXME: Use the DataStore for keeping the references Image <=> SAC
		Image< ? > image = DataStore.sourceToImage().get( currentSource );

		if ( image instanceof RegionAnnotationImage )
		{
			transformableImages = ( ( RegionAnnotationImage< ? > ) image ).getSelectedImages();
			ManualTransformationEditor transformationEditor = new ManualTransformationEditor( bdvh.getViewerPanel(), bdvh.getKeybindings() );
			transformationEditor.setTransformableSources(  );
			transformationEditor.setActive( true );
			transformationEditor.manualTransformActiveListeners().add( this );
		}
	}

	@Override
	public void manualTransformActiveChanged( boolean active )
	{
		if ( !active )
		{
			if ( transformableImages != null )
			{
				// transform each image with an identity transform in order to
				// trigger the update of the transform and notify listeners.
				// see the {@code SpimDataImage} implementation.
				transformableImages.stream().forEach( image -> image.transform( new AffineTransform3D() ) );
			}
		}
	}

}