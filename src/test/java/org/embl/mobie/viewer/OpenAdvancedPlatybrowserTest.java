package org.embl.mobie.viewer;

import net.imagej.ImageJ;
import org.embl.mobie.io.ImageDataFormat;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class OpenAdvancedPlatybrowserTest
{
	public static void main( final String... args ) throws IOException
	{
		new OpenAdvancedPlatybrowserTest().test();
	}

	@Test
	public void test() throws IOException
	{
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		new MoBIE(
				"https://github.com/mobie/platybrowser-datasets",
				MoBIESettings.settings()
						.gitProjectBranch( "main" )
						.imageDataFormat( ImageDataFormat.BdvN5 )
						.imageDataLocation( "https://github.com/mobie/platybrowser-datasets" )
						.tableDataLocation( "https://github.com/mobie/platybrowser-datasets" )
						.gitTablesBranch( "main" ) );

	}
}
