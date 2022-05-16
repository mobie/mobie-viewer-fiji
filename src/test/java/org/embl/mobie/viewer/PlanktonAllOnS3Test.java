package org.embl.mobie.viewer;

import net.imagej.ImageJ;
import org.embl.mobie.viewer.view.View;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

public class PlanktonAllOnS3Test
{
	public static void main( String[] args ) throws IOException
	{
		new PlanktonAllOnS3Test().allOnS3Test();
	}

	@Test
	public void allOnS3Test() throws IOException
	{
		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();

		// This is special as it does not go via github but
		// all information is on S3
		final MoBIE moBIE = new MoBIE("https://s3.embl.de/plankton-fibsem", MoBIESettings.settings());

		// Check all views
		final Map< String, View > views = moBIE.getViews();
		for ( View view : views.values() )
		{
			moBIE.getViewManager().show( view );
		}
	}
}
