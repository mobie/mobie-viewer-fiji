package tests;

import de.embl.cba.platynereis.platyviews.PlatyViews;
import org.junit.Test;

import java.io.IOException;

public class TestBookmarksLoadingFromGitlab
{
	//@Test
	public static void main( String[] args ) throws IOException
	{
		// Old style
//		final PlatyViews platyViews = new PlatyViews( "https://git.embl.de/tischer/platy-browser-tables/raw/master/data/0.4.0/misc/bookmarks.json" );

		// New style
		final PlatyViews platyViewsV2 = new PlatyViews( null, "https://git.embl.de/tischer/platy-browser-tables/raw/master/data/test_n5/0.6.5/misc/bookmarks.json", "0.7.0" );
	}
}
