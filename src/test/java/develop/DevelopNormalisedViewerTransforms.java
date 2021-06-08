package develop;

import de.embl.cba.mobie.MoBIE;
import de.embl.cba.mobie.MoBIESettings;
import de.embl.cba.mobie.source.ImageDataFormat;
import net.imagej.ImageJ;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.Scale3D;

import java.io.IOException;

public class DevelopNormalisedViewerTransforms
{
	public static void main( String[] args )
	{
		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();

		testNormalisationAndReversion();

		try {
			final MoBIE moBIE = new MoBIE("https://github.com/mobie-org/covid-em-datasets",
					MoBIESettings.settings().imageDataFormat( ImageDataFormat.BdvN5S3 ) );
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void testNormalisationAndReversion()
	{
		final AffineTransform3D affineTransform3D = new AffineTransform3D();
		System.out.println( "Identity: " + affineTransform3D );

		// transform the transform

		// translate
		final AffineTransform3D translation = new AffineTransform3D();
		translation.translate( 10, 10, 0 );
		affineTransform3D.preConcatenate( translation );

		// scale
		final Scale3D scale3D = new Scale3D( 0.1, 0.1, 0.1 );
		affineTransform3D.preConcatenate( scale3D );

		System.out.println( "Normalised translated and scaled: " + affineTransform3D );

		// invert above transformations
		affineTransform3D.preConcatenate( scale3D.inverse() );
		affineTransform3D.preConcatenate( translation.inverse() );

		System.out.println( "Reversed: " + affineTransform3D ); // should be identity again
	}

}
