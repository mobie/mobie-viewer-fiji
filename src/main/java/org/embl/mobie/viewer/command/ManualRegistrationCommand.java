package org.embl.mobie.viewer.command;

import bdv.tools.transformation.TransformedSource;
import bdv.viewer.SourceAndConverter;
import ij.gui.NonBlockingGenericDialog;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;

import javax.swing.*;

@Plugin(type = BdvPlaygroundActionCommand.class, menuPath = CommandConstants.CONTEXT_MENU_ITEMS_ROOT + "Transform>Registration - 3D Manual")
public class ManualRegistrationCommand implements BdvPlaygroundActionCommand
{
	@Override
	public void run()
	{
		new Thread( () -> showDialog() ).start();
	}

	private void showDialog()
	{
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
	}
}
