package de.embl.cba.mobie.universe;

import de.embl.cba.mobie.ui.SourcesDisplayManager;
import ij.gui.NonBlockingGenericDialog;

import java.util.ArrayList;

public class UniverseConfigurationDialog
{
	private final SourcesDisplayManager sourcesDisplayManager;

	public UniverseConfigurationDialog( SourcesDisplayManager sourcesDisplayManager )
	{
		this.sourcesDisplayManager = sourcesDisplayManager;
	}

	public void showDialog()
	{
		new Thread( () -> {
			final NonBlockingGenericDialog gd = new NonBlockingGenericDialog( "3D View Preferences" );

			ArrayList< String > sourceNames = new ArrayList< String >( sourcesDisplayManager.getVisibleSourceNames() );
			gd.addMessage( "Resolution for 3D view [micrometer]" );
			gd.addMessage( "Put 0 for an automated resolution choice." );

			for ( String sourceName : sourceNames )
			{
				gd.addNumericField( sourceName,  sourcesDisplayManager.getSourceAndCurrentMetadata( sourceName ).metadata().resolution3dView, 2 );
			}

			gd.showDialog();
			if ( gd.wasCanceled() ) return;

			for ( String sourceName : sourceNames )
			{
				sourcesDisplayManager.setVoxelSpacing3DView( sourceName, gd.getNextNumber() );
			}
		} ).start();
	}
}
