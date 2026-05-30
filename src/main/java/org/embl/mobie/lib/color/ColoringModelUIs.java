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
package org.embl.mobie.lib.color;

import org.embl.mobie.ui.MoBIEWindowManager;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.WeakHashMap;

public abstract class ColoringModelUIs
{
	private static final WeakHashMap< ColoringModel< ? >, ColoringModelAdjustmentDialog > dialogs = new WeakHashMap<>();

	public static void show( ColoringModel< ? > coloringModel )
	{
		if ( coloringModel == null )
			return;

		SwingUtilities.invokeLater( () ->
		{
			ColoringModelAdjustmentDialog dialog = dialogs.get( coloringModel );
			if ( dialog == null || ! dialog.isDisplayable() )
			{
				dialog = new ColoringModelAdjustmentDialog( coloringModel );
				dialogs.put( coloringModel, dialog );
				MoBIEWindowManager.addWindow( dialog );
				final ColoringModelAdjustmentDialog finalDialog = dialog;
				dialog.addWindowListener( new WindowAdapter()
				{
					@Override
					public void windowClosed( WindowEvent e )
					{
						dialogs.remove( coloringModel, finalDialog );
					}
				} );
			}
			else
			{
				dialog.refresh();
				dialog.toFront();
			}
		} );
	}
}
