package tests;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import de.embl.cba.platynereis.platybrowser.Bookmark;
import de.embl.cba.platynereis.platybrowser.PlatyViews;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;

public class TestBookmarksLoadingFromGitlab
{
	public static void main( String[] args ) throws IOException
	{
		// Old style
		final PlatyViews platyViews = new PlatyViews( "https://git.embl.de/tischer/platy-browser-tables/raw/master/data/0.4.0/misc/bookmarks.json" );

		// New style
		final PlatyViews platyViewsV2 = new PlatyViews( "https://git.embl.de/tischer/platy-browser-tables/raw/master/data/test_n5/0.6.5/misc/bookmarks.json", "0.7.0" );
	}
}
