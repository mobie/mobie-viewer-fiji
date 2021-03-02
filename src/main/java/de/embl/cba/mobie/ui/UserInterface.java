package de.embl.cba.mobie.ui;

import bdv.util.BdvHandle;
import bdv.viewer.Interpolation;
import de.embl.cba.bdv.utils.BdvUtils;
import ij.WindowManager;

import javax.swing.*;
import java.awt.*;

public class UserInterface extends JPanel
{
	private final UserInterfacePanelsProvider panelsProvider;

	public UserInterface( MoBIE moBIE, BdvHandle bdv )
	{
		panelsProvider = new UserInterfacePanelsProvider( moBIE );

		final JPanel actionPanel = createActionPanel( moBIE, bdv );
		// TODO createSourcesDisplaySettingsPanel

		final JFrame frame = createFrame( moBIE, actionPanel );
		setImageJLogWindowPositionAndSize( frame );
		setBdvWindowPositionAndSize( bdv, frame );
	}

	protected JPanel createActionPanel( MoBIE moBIE, BdvHandle bdv )
	{
		final JPanel actionPanel = new JPanel();
		actionPanel.setLayout( new BoxLayout( this, BoxLayout.Y_AXIS ) );

		actionPanel.add( panelsProvider.createInfoPanel( moBIE.getProjectLocation(), moBIE.getOptions().values.getPublicationURL() ) );
		actionPanel.add( new JSeparator( SwingConstants.HORIZONTAL ) );

		actionPanel.add( panelsProvider.createDatasetSelectionPanel() );
		actionPanel.add( new JSeparator( SwingConstants.HORIZONTAL ) );

		actionPanel.add( panelsProvider.createSourceSelectionPanel( moBIE.getSourcesDisplayManager() ) );
		actionPanel.add( new JSeparator( SwingConstants.HORIZONTAL ) );

		actionPanel.add( panelsProvider.createBookmarksPanel( moBIE.getBookmarkManager() )  );
		actionPanel.add( panelsProvider.createMoveToLocationPanel( bdv )  );

		if ( moBIE.getLevelingVector() != null )
		{
			actionPanel.add( panelsProvider.createLevelingPanel( bdv, moBIE.getLevelingVector() ) );
		}
		return actionPanel;
	}

	private JFrame createFrame( MoBIE moBIE, JPanel actionPanel )
	{
		JFrame frame = new JFrame( "MoBIE: " + moBIE.getProjectName() + "-" + moBIE.getDataset() );

		JSplitPane splitPane = new JSplitPane();
		splitPane.setOrientation( JSplitPane.VERTICAL_SPLIT );
		final int sourceSelectionPanelHeight = panelsProvider.getSourceSelectionPanelHeight();
		final int actionPanelHeight = sourceSelectionPanelHeight + 3 * 40;

		splitPane.setDividerLocation( actionPanelHeight );
		splitPane.setTopComponent( actionPanel );
		splitPane.setBottomComponent( sourcesDisplayManager );
		splitPane.setAutoscrolls( true );
		int frameWidth = 600;

		// show frame
		frame.setPreferredSize( new Dimension( frameWidth, actionPanelHeight + 200 ) );
		frame.getContentPane().setLayout( new GridLayout() );
		frame.getContentPane().add( splitPane );

		frame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
		frame.pack();
		frame.setVisible( true );

		return frame;
	}

	private void setImageJLogWindowPositionAndSize( JFrame parentComponent )
	{
		final Frame log = WindowManager.getFrame( "Log" );
		if (log != null) {
			Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			final int logWindowHeight = screenSize.height - ( parentComponent.getLocationOnScreen().y + parentComponent.getHeight() + 20 );
			log.setSize( parentComponent.getWidth(), logWindowHeight  );
			log.setLocation( parentComponent.getLocationOnScreen().x, parentComponent.getLocationOnScreen().y + parentComponent.getHeight() );
		}
	}

	private void setBdvWindowPositionAndSize( BdvHandle bdvHandle, JFrame parentComponent )
	{
		BdvUtils.getViewerFrame( bdvHandle ).setLocation(
				parentComponent.getLocationOnScreen().x + parentComponent.getWidth(),
				parentComponent.getLocationOnScreen().y );

		BdvUtils.getViewerFrame( bdvHandle ).setSize( parentComponent.getHeight(), parentComponent.getHeight() );

		bdvHandle.getViewerPanel().setInterpolation( Interpolation.NLINEAR );
	}
}
