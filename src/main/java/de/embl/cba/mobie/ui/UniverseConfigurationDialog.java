package de.embl.cba.mobie.ui;

import de.embl.cba.mobie.ui.viewer.SourcesManager;
import ij.gui.NonBlockingGenericDialog;

import java.util.ArrayList;

public class UniverseConfigurationDialog
{
	private final SourcesManager sourcesManager;

	public UniverseConfigurationDialog( SourcesManager sourcesManager )
	{
		this.sourcesManager = sourcesManager;
	}

	public void showDialog()
	{
		new Thread( () -> {
			final NonBlockingGenericDialog gd = new NonBlockingGenericDialog( "3D View Preferences" );

			ArrayList< String > sourceNames = new ArrayList< String >( sourcesManager.getVisibleSourceNames() );
			gd.addMessage( "Resolution for 3D view [micrometer]" );
			gd.addMessage( "Put 0 for an automated resolution choice." );

			for ( String sourceName : sourceNames )
			{
				gd.addNumericField( sourceName,  sourcesManager.getSourceAndCurrentMetadata( sourceName ).metadata().resolution3dView, 2 );
			}

			gd.showDialog();
			if ( gd.wasCanceled() ) return;

			for ( String sourceName : sourceNames )
			{
				sourcesManager.setVoxelSpacing3DView( sourceName, gd.getNextNumber() );
			}
		} ).start();
	}
}
