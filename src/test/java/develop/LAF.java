package develop;

import org.embl.mobie.viewer.ui.UserInterfaceHelper;

import javax.swing.*;

public class LAF
{
	public static void main( String[] args )
	{
		final LookAndFeel lookAndFeel = UIManager.getLookAndFeel();
		final String crossPlatformLookAndFeelClassName = UIManager.getCrossPlatformLookAndFeelClassName();
		final String systemLookAndFeelClassName = UIManager.getSystemLookAndFeelClassName();
	}
}
