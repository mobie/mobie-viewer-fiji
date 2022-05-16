package org.embl.mobie.viewer;

import net.imagej.ImageJ;
import org.embl.mobie.io.ImageDataFormat;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class OpenPlatybrowserTest
{
	public static void main( final String... args ) throws IOException
	{
		new OpenPlatybrowserTest().test();
	}

	@Test
	public void test() throws IOException
	{
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		final MoBIE moBIE = new MoBIE( "https://github.com/mobie/platybrowser-datasets" );
		moBIE.getViewManager().show( "Figure 2C: Muscle segmentation" );
	}
}
