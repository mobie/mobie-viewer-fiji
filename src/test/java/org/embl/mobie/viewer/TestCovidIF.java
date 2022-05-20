package org.embl.mobie.viewer;

import java.io.IOException;

public class TestCovidIF
{
	public static void main( String[] args ) throws IOException
	{
		new TestCovidIF().test();
	}

	//@Test
	public void test() throws IOException
	{
		// TODO: Bug loading the multi-scales
//		final ImageJ imageJ = new ImageJ();
//		imageJ.ui().showUI();
//		final MoBIE moBIE = new MoBIE( "https://github.com/mobie/covid-if-project", MoBIESettings.settings().gitProjectBranch( "main" ).imageDataFormat( ImageDataFormat.OmeZarr ) );
	}
}
