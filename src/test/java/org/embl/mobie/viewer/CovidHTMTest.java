package org.embl.mobie.viewer;

import net.imagej.ImageJ;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class CovidHTMTest
{
	public static void main( String[] args ) throws IOException
	{
		new CovidHTMTest().testDefaultView();
	}

	@Test
	public void testDefaultView() throws IOException
	{
		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();
		final MoBIE moBIE = new MoBIE( "https://github.com/mobie/covid-if-project" );
	}
}
