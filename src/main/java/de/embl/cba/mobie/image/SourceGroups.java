package de.embl.cba.mobie.image;

import bdv.util.BdvHandle;
import bdv.util.BdvStackSource;
import bdv.viewer.SourceGroup;
import bdv.viewer.SynchronizedViewerState;
import de.embl.cba.tables.image.SourceAndMetadata;

import java.util.*;

public abstract class SourceGroups
{
	private static Map< String, SourceGroup > groupIdToSourceGroup = new HashMap<>();
	private static Map< String, List< SourceAndMetadata< ? > > > groupIdToSourceAndMetadataList = new HashMap<>();

	public static synchronized SourceGroup addSourceToGroup( SourceAndMetadata< ? > sourceAndMetadata )
	{
		final BdvStackSource< ? > bdvStackSource = sourceAndMetadata.metadata().bdvStackSource;
		final BdvHandle bdvHandle = bdvStackSource.getBdvHandle();
		final SynchronizedViewerState state = bdvHandle.getViewerPanel().state();

		String groupId = sourceAndMetadata.metadata().groupId;
		if ( ! groupIdToSourceGroup.containsKey( groupId ) )
		{
			SourceGroup sourceGroup = new SourceGroup();
			groupIdToSourceGroup.put( groupId, sourceGroup );
			state.addGroup( sourceGroup );
			state.setGroupName( sourceGroup, groupId );
			state.setGroupActive( sourceGroup, true );
		}

		if ( ! groupIdToSourceAndMetadataList.containsKey( groupId ) )
		{
			groupIdToSourceAndMetadataList.put( groupId, new ArrayList<>(  ) );
		}

		SourceGroup sourceGroup = groupIdToSourceGroup.get( groupId );
		state.addSourceToGroup( bdvStackSource.getSources().get( 0 ), sourceGroup );

		groupIdToSourceAndMetadataList.get( groupId ).add( sourceAndMetadata );

		return sourceGroup;
	}

	public static SourceGroup getSourceGroup( String groupId )
	{
		return groupIdToSourceGroup.get( groupId );
	}

	public static synchronized List< SourceAndMetadata< ? > > getSourcesAndMetadata( String groupId )
	{
		return Collections.unmodifiableList( groupIdToSourceAndMetadataList.get( groupId ) );
	}
}
