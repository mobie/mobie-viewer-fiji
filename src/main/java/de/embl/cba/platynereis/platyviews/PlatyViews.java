package de.embl.cba.platynereis.platyviews;


import com.google.gson.internal.LinkedTreeMap;
import de.embl.cba.bdv.utils.sources.Metadata;
import de.embl.cba.platynereis.utils.Utils;
import de.embl.cba.platynereis.utils.Version;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * TODO: probably read this from a editable text file so that users can add own views.
 *
 *
 */
public class PlatyViews
{
	private Map< String, Bookmark > nameToBookmark;


	public PlatyViews( String viewsSourcePath )
	{
		this( viewsSourcePath, "0.0.0" );
	}

	public PlatyViews( String viewsSourcePath, String versionString )
	{
		final Version version = new Version( versionString );

		nameToBookmark = new BookmarkParser( viewsSourcePath ).call();
	}


	public void setView( String bookmarkId )
	{

	}
}
