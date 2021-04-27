package de.embl.cba.mobie2.ui;

import bdv.util.BdvHandle;
import bdv.viewer.Interpolation;
import de.embl.cba.bdv.utils.BdvUtils;
import ij.WindowManager;

import javax.swing.*;
import java.awt.*;

public class WindowArrangementHelper
{
	public static int getDefaultWindowWidth()
	{
		// TODO: does not work for multiple screens
		return (int) ( Toolkit.getDefaultToolkit().getScreenSize().width / 3.1 );
	}

	@Deprecated
	public static void setImageJLogWindowPositionAndSize( JFrame parentComponent )
	{
		final Frame log = WindowManager.getFrame( "Log" );
		if (log != null) {
			Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			final int logWindowHeight = screenSize.height - ( parentComponent.getLocationOnScreen().y + parentComponent.getHeight() + 20 );
			log.setSize( parentComponent.getWidth(), logWindowHeight  );
			log.setLocation( parentComponent.getLocationOnScreen().x, parentComponent.getLocationOnScreen().y + parentComponent.getHeight() );
		}
	}

	public static void setBdvWindowPositionAndSize( BdvHandle bdvHandle, JFrame frame )
	{
		BdvUtils.getViewerFrame( bdvHandle ).setLocation(
				frame.getLocationOnScreen().x + frame.getWidth(),
				frame.getLocationOnScreen().y );

		BdvUtils.getViewerFrame( bdvHandle ).setSize( frame.getHeight(), frame.getHeight() );

		bdvHandle.getViewerPanel().setInterpolation( Interpolation.NLINEAR );
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
