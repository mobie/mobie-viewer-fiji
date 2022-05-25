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
package org.embl.mobie.viewer.ui;

import bdv.util.BdvHandle;
import de.embl.cba.bdv.utils.BdvUtils;
import ij.WindowManager;

import java.awt.*;

public class WindowArrangementHelper
{

	static { net.imagej.patcher.LegacyInjector.preinit(); }

	public static int getDefaultWindowWidth()
	{
		// TODO: does not work for multiple screens
		return (int) ( Toolkit.getDefaultToolkit().getScreenSize().width / 3.1 );
	}

	public static void setBdvWindowPositionAndSize( BdvHandle bdvHandle )
	{
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		final double relativeHeight = 2.0/3;
		final int height = (int) ( relativeHeight * screenSize.height - 2 * UserInterfaceHelper.SPACING );
		final int width = screenSize.width / 2 - 2 * UserInterfaceHelper.SPACING;

		BdvUtils.getViewerFrame( bdvHandle ).setLocation(
				screenSize.width / 2 + UserInterfaceHelper.SPACING,
				UserInterfaceHelper.SPACING );

		BdvUtils.getViewerFrame( bdvHandle ).setSize( width, height );
	}

	public static void setLogWindowPositionAndSize( Window reference )
	{
		final Frame log = WindowManager.getFrame( "Log" );
		if (log != null) {
			Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			final int logWindowHeight = screenSize.height - ( reference.getLocationOnScreen().y + reference.getHeight() + 2 * UserInterfaceHelper.SPACING );
			log.setSize( reference.getWidth(), logWindowHeight  );
			log.setLocation( reference.getLocationOnScreen().x, reference.getLocationOnScreen().y + UserInterfaceHelper.SPACING + reference.getHeight() );
		}
	}

	public static void setLogWindowPositionAndSize()
	{
		final Frame log = WindowManager.getFrame( "Log" );
		if (log != null) {
			Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			final double relativeHeight = 1.0/3;
			final int height = (int) ( relativeHeight * screenSize.height ) - 2 * UserInterfaceHelper.SPACING;
			final int width = screenSize.width - 2 * UserInterfaceHelper.SPACING;
			log.setSize( width, height  );
			log.setLocation( UserInterfaceHelper.SPACING, (int) ( ( 1.0 - relativeHeight ) * screenSize.height ) + UserInterfaceHelper.SPACING );
		}
	}

	public static void rightAlignWindow( Window reference, Window window, boolean adjustWidth, boolean adjustHeight )
	{
		window.setLocation(
				reference.getLocationOnScreen().x + reference.getWidth() + UserInterfaceHelper.SPACING,
				reference.getLocationOnScreen().y );

		if ( adjustWidth )
			window.setSize( reference.getWidth(), window.getHeight() );

		if ( adjustHeight )
			window.setSize( window.getWidth(), reference.getHeight() );
	}

	public static void bottomAlignWindow( Window reference, Window window )
	{
		window.setLocation(
				reference.getLocationOnScreen().x,
				reference.getLocationOnScreen().y + reference.getHeight() + UserInterfaceHelper.SPACING
		);
	}
}
