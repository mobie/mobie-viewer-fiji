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

import org.embl.mobie.command.CommandConstants;
import org.embl.mobie.command.MoBIEManualTransformationEditor;
import org.scijava.ItemVisibility;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.Button;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;

import java.util.Arrays;

@Plugin(type = BdvPlaygroundActionCommand.class, attrs = {}, menuPath = CommandConstants.CONTEXT_MENU_ITEMS_ROOT + "Transform>Registration - Manual")
public class ManualTransformationCommand extends AbstractTransformationCommand
{
	public static final String INACTIVE = "Manual transform inactive";

	static { net.imagej.patcher.LegacyInjector.preinit(); }

	@Parameter ( label = "Start manual transform", callback = "startManualTransform" )
	public Button startManualTransform;

	@Parameter ( label = "Accept manual transform", callback = "acceptManualTransform" )
	public Button acceptManualTransform;

	@Parameter ( label = "Cancel manual transform", callback = "cancelManualTransform" )
	public Button cancelManualTransform;

	@Parameter( visibility = ItemVisibility.MESSAGE )
	private final String status = INACTIVE;

	private MoBIEManualTransformationEditor transformationEditor;

	@Override
	public void initialize()
	{
		super.initialize();
	}


	public void startManualTransform()
	{
		setMovingImages();

		transformationEditor = new MoBIEManualTransformationEditor( bdvHandle.getViewerPanel(), bdvHandle.getKeybindings() );
		transformationEditor.setTransformableSources( movingSacs );
		transformationEditor.setActive( true );

		getInfo().getMutableInput( "status", String.class )
				.setValue( this, "Transforming: " + String.join( ",", selectedImages.getNames() ));
	}

	private void acceptManualTransform()
	{
		if ( transformationEditor == null ) return;

		applyTransform( transformationEditor.getManualTransform() );

		// This will cause the transformed image to jump back to its original position,
		// but this is intended as the transformed image is now a new image that is stored as a new view.
		// And this new transformed image will also be shown by the above applyTransform function.
		transformationEditor.setActive( false );

		getInfo().getMutableInput( "status", String.class ).setValue( this, INACTIVE );

		// TODO: make the non-transformed sources invisible

		// TODO: Close the Command UI, but how?
		// https://imagesc.zulipchat.com/#narrow/stream/327238-Fiji/topic/Close.20Scijava.20Command.20UI
	}

	private void cancelManualTransform()
	{
		if ( transformationEditor == null ) return;

		transformationEditor.setActive( false );

		getInfo().getMutableInput( "status", String.class )
				.setValue( this, INACTIVE );
	}
}
