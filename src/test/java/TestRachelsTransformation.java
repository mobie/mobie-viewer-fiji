import bdv.util.*;
import de.embl.cba.platynereis.transforms.TransformConversions;
import ij.IJ;
import ij.ImagePlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;

public class TestRachelsTransformation
{
	public static void main( String[] args )
	{
		// Euler transformation in Elastix:
		// The parameter vector μ consists of the Euler angles (in rad)
		// and the translation vector.
		// In 3D, this gives a vector of length 6: μ = (θx,θy,θz,tx,ty,tz).
		// The centre of rotation is not part of μ; it is a fixed setting,
		// usually the centre of the image.

		// ProSPr to EM Rachel, as manually found using Amira:
		// translation in micrometer: -64.894, -108.420, 16.081
		// rotation in degrees: 98.6
		// rotation axis: -0.61,-0.47,-0.636


		final ImagePlus musclesProsprImp = IJ.openImage( "/Users/tischer/Documents/detlev-arendt-clem-registration--data/data/prospr-new/muscles.zip" );
		final ImagePlus musclesRachelImp = IJ.openImage( "/Users/tischer/Documents/rachel-mellwig-em-prospr-registration/data/FIB segmentation/muscle.tif" );

		final RandomAccessibleInterval musclesProspr = ImageJFunctions.wrapReal( musclesProsprImp );
		final RandomAccessibleInterval musclesRachel = ImageJFunctions.wrapReal( musclesRachelImp );

		final double[] centre = TransformConversions.getImageCentreInPixelUnits( musclesProspr );

		final AffineTransform3D affineTransform3D = TransformConversions.getAmiraAsAffineTransform3D(
				new double[]{ -0.61, -0.47, -0.636 },
				98.6,
				new double[]{ -64.894, -108.42, +16.081 },
				0.5,
				centre );

		Bdv bdv = BdvFunctions.show( musclesProspr,
				"muscles-prospr-transformed",
				BdvOptions.options().sourceTransform( affineTransform3D )).getBdvHandle();
		final BdvStackSource rachel = BdvFunctions.show( musclesRachel, "rachel",
				BdvOptions.options().addTo( bdv ) );

		rachel.setColor( new ARGBType( ARGBType.rgba( 0,255,0,255 ) ) );

	}

}
