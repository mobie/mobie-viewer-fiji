package de.embl.cba.mobie.image;

import bdv.util.BdvHandle;
import bdv.util.BdvStackSource;
import bdv.viewer.SourceGroup;
import bdv.viewer.SynchronizedViewerState;
import de.embl.cba.tables.image.SourceAndMetadata;

import java.util.HashMap;
import java.util.Map;

public abstract class SourceGroupings
{
	private static Map< String, SourceGroup > groupIdToSourceGroup = new HashMap<>(  );

	public static synchronized SourceGroup addSourceToGroup( SourceAndMetadata< ? > sourceAndMetadata )
	{
		final BdvStackSource< ? > bdvStackSource = sourceAndMetadata.metadata().bdvStackSource;
		final BdvHandle bdvHandle = bdvStackSource.getBdvHandle();
		final SynchronizedViewerState state = bdvHandle.getViewerPanel().state();

		String groupId = sourceAndMetadata.metadata().groupId;
		if ( ! groupIdToSourceGroup.keySet().contains( groupId ) )
		{
			SourceGroup sourceGroup = new SourceGroup();
			groupIdToSourceGroup.put( groupId, sourceGroup );
			state.addGroup( sourceGroup );
			state.setGroupName( sourceGroup, groupId );
			state.setGroupActive( sourceGroup, true );
		}

		SourceGroup sourceGroup = groupIdToSourceGroup.get( groupId );
		state.addSourceToGroup( bdvStackSource.getSources().get( 0 ), sourceGroup );

		return sourceGroup;
	}
}
