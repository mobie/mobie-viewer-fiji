package org.embl.mobie.viewer;

import bdv.viewer.SourceAndConverter;
import net.imagej.ImageJ;
import org.embl.mobie.viewer.source.LabelSource;
import org.embl.mobie.viewer.source.SourceHelper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

public class CLEMExampleTest
{
	public static void main( String[] args ) throws IOException
	{
		new CLEMExampleTest().testFigure2a();
	}

	@Test
	public void testFigure2a() throws IOException
	{
		// Init
		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();

		// Open
		final MoBIE moBIE = new MoBIE( "https://github.com/mobie/clem-example-project", MoBIESettings.settings().view( "Figure2a" ) );

		// Test
		moBIE.sourceNameToSourceAndConverter().keySet().stream().forEach( s -> System.out.println( s ) );
		final SourceAndConverter< ? > sourceAndConverter = moBIE.sourceNameToSourceAndConverter().get( "fluorescence-annotations" );
		final LabelSource< ? > labelSource = SourceHelper.getLabelSource( sourceAndConverter );
		final boolean showAsBoundaries = labelSource.isShowAsBoundaries();
		assertTrue( showAsBoundaries );
	}
}
