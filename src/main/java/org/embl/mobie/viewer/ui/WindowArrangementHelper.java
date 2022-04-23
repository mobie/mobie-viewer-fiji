package org.embl.mobie.viewer.ui;

import bdv.util.BdvHandle;
import bdv.viewer.Interpolation;
import de.embl.cba.bdv.utils.BdvUtils;
import ij.WindowManager;

import javax.swing.*;
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
		final int height = (int) ( relativeHeight * screenSize.height - 2 * UserInterfaceHelpers.SPACING );
		final int width = screenSize.width / 2 - 2 * UserInterfaceHelpers.SPACING;

		BdvUtils.getViewerFrame( bdvHandle ).setLocation(
				screenSize.width / 2 + UserInterfaceHelpers.SPACING,
				UserInterfaceHelpers.SPACING );

		BdvUtils.getViewerFrame( bdvHandle ).setSize( width, height );
	}

	public static void setLogWindowPositionAndSize( Window reference )
	{
		final Frame log = WindowManager.getFrame( "Log" );
		if (log != null) {
			Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			final int logWindowHeight = screenSize.height - ( reference.getLocationOnScreen().y + reference.getHeight() + 2 * UserInterfaceHelpers.SPACING );
			log.setSize( reference.getWidth(), logWindowHeight  );
			log.setLocation( reference.getLocationOnScreen().x, reference.getLocationOnScreen().y + UserInterfaceHelpers.SPACING + reference.getHeight() );
		}
	}

	public static void setLogWindowPositionAndSize()
	{
		final Frame log = WindowManager.getFrame( "Log" );
		if (log != null) {
			Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			final double relativeHeight = 1.0/3;
			final int height = (int) ( relativeHeight * screenSize.height ) - 2 * UserInterfaceHelpers.SPACING;
			final int width = screenSize.width - 2 * UserInterfaceHelpers.SPACING;
			log.setSize( width, height  );
			log.setLocation( UserInterfaceHelpers.SPACING, (int) ( ( 1.0 - relativeHeight ) * screenSize.height ) + UserInterfaceHelpers.SPACING );
		}
	}

	public static void rightAlignWindow( Window reference, Window window, boolean adjustWidth, boolean adjustHeight )
	{
		window.setLocation(
				reference.getLocationOnScreen().x + reference.getWidth() + UserInterfaceHelpers.SPACING,
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
				reference.getLocationOnScreen().y + reference.getHeight() + UserInterfaceHelpers.SPACING
		);
	}
}
