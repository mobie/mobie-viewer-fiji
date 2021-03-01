package de.embl.cba.mobie.ui;

import javax.swing.*;

public class UserInterface extends JPanel
{
	public UserInterface( ProjectManager projectManager )
	{
		this.setLayout( new BoxLayout( this, BoxLayout.Y_AXIS ) );
		final UserInterfacePanelsProvider panelsProvider = new UserInterfacePanelsProvider( projectManager );
		this.add( panelsProvider.createInfoPanel( projectManager.getProjectLocation(), projectManager.getOptions().values.getPublicationURL() ) );
		this.add( new JSeparator( SwingConstants.HORIZONTAL ) );
		this.add( panelsProvider.createDatasetSelectionPanel() );
		this.add( new JSeparator( SwingConstants.HORIZONTAL ) );
		this.add( panelsProvider.createSourceSelectionPanel() );
		this.add( new JSeparator( SwingConstants.HORIZONTAL ) );
		this.add( panelsProvider.createBookmarksPanel()  );
		this.add( panelsProvider.createMoveToLocationPanel()  );

		if ( projectManager.getLevelingVector() != null )
			this.add( panelsProvider.createLevelingPanel( projectManager.getLevelingVector() ) );
	}
}
