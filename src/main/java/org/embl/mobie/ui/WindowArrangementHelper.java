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
package org.embl.mobie.ui;

import java.awt.*;

import static org.embl.mobie.ui.UserInterfaceHelper.*;

public class WindowArrangementHelper
{
	static { net.imagej.patcher.LegacyInjector.preinit(); }

	public static void rightAlignWindow( Window reference, Window window, boolean adjustHeight, boolean maximiseWidth )
	{
		final int x = reference.getLocationOnScreen().x + reference.getWidth() + SPACING;
		final int y = reference.getLocationOnScreen().y;
		int width = window.getWidth();
		int height = window.getHeight();

		if ( adjustHeight )
			height = reference.getHeight();

		if ( maximiseWidth )
		{
			Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			width = screenSize.width - x - SPACING;
		}

		window.setLocation( x, y );
		window.setSize( width, height );
	}

	public static void bottomAlignWindow( Window reference, Window window, boolean adjustWidth, boolean maximiseHeight )
	{
		final int x = reference.getLocationOnScreen().x;
		final int y = reference.getLocationOnScreen().y + reference.getHeight() + SPACING;
		int width = window.getWidth();
		int height = window.getHeight();

		if ( adjustWidth )
			width = reference.getWidth();

		if ( maximiseHeight )
		{
			Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			height = screenSize.height - y - SPACING;
		}

		window.setLocation( x, y );
		window.setSize( width, height );
	}
}
