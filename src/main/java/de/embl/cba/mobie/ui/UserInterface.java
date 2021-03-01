package de.embl.cba.mobie.ui;

import javax.swing.*;

public class UserInterface extends JPanel
{
	public UserInterface( MoBIE moBIE )
	{
		this.setLayout( new BoxLayout( this, BoxLayout.Y_AXIS ) );
		final UserInterfacePanelsProvider panelsProvider = new UserInterfacePanelsProvider( moBIE );
		this.add( panelsProvider.createInfoPanel( moBIE.getProjectLocation(), moBIE.getOptions().values.getPublicationURL() ) );
		this.add( new JSeparator( SwingConstants.HORIZONTAL ) );
		this.add( panelsProvider.createDatasetSelectionPanel() );
		this.add( new JSeparator( SwingConstants.HORIZONTAL ) );
		this.add( panelsProvider.createSourceSelectionPanel() );
		this.add( new JSeparator( SwingConstants.HORIZONTAL ) );
		this.add( panelsProvider.createBookmarksPanel()  );
		this.add( panelsProvider.createMoveToLocationPanel()  );

		if ( moBIE.getLevelingVector() != null )
			this.add( panelsProvider.createLevelingPanel( moBIE.getLevelingVector() ) );
	}
}
