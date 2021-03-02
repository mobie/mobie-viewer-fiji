package de.embl.cba.mobie.image;

import de.embl.cba.tables.image.SourceAndMetadata;

public interface SourceAndMetadataChangedListener
{
	void addedToBDV( SourceAndMetadata< ? > sam );

	void removedFromBDV( SourceAndMetadata< ? > sam );

	void colorChanged( SourceAndMetadata< ? > sam );
}
