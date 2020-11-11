package de.embl.cba.mobie.ui;

import de.embl.cba.mobie.ui.viewer.SourcesPanel;
import de.embl.cba.tables.image.SourceAndMetadata;
import ij.gui.NonBlockingGenericDialog;

import java.util.ArrayList;
import java.util.Set;

public class UniverseConfigurationDialog
{
	private final SourcesPanel sourcesPanel;

	public UniverseConfigurationDialog( SourcesPanel sourcesPanel )
	{
		this.sourcesPanel = sourcesPanel;
	}

	public void showDialog()
	{
		new Thread( () -> {
			final NonBlockingGenericDialog gd = new NonBlockingGenericDialog( "3D View Preferences" );

			ArrayList< String > sourceNames = new ArrayList< String >( sourcesPanel.getVisibleSourceNames() );
			gd.addMessage( "Resolution for 3D view [micrometer]" );
			gd.addMessage( "Put 0 for an automated resolution choice." );

			for ( String sourceName : sourceNames )
			{
				gd.addNumericField( sourceName,  sourcesPanel.getSourceAndCurrentMetadata( sourceName ).metadata().resolution3dView, 2 );
			}

			gd.showDialog();
			if ( gd.wasCanceled() ) return;

			for ( String sourceName : sourceNames )
			{
				sourcesPanel.setVoxelSpacing3DView( sourceName, gd.getNextNumber() );
			}
		} ).start();
	}
}
