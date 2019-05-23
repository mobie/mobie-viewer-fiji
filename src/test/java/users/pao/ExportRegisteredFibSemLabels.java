package users.pao;

import de.embl.cba.transforms.utils.Transforms;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.interpolation.randomaccess.ClampingNLinearInterpolatorFactory;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.view.Views;

public class ExportRegisteredFibSemLabels
{
	public static void main( String[] args ) throws SpimDataException
	{

		SpimData spimData = new XmlIoSpimData().load( "" );

		final AffineTransform3D transform3D
				= spimData.getViewRegistrations()
				.getViewRegistration( 0, 0 ).getModel();

		final RandomAccessibleInterval< ? > image = spimData.getSequenceDescription()
				.getImgLoader().getSetupImgLoader( 0 )
				.getImage( 0 );

//		final RandomAccessible transformedRA =
//				Transforms.createTransformedRaView(
//						images.get( i ),
//						transforms.get( i ),
//						new ClampingNLinearInterpolatorFactory() );
//
//		final FinalInterval transformedInterval =
//				Transforms.createBoundingIntervalAfterTransformation(
//						images.get( i ), transforms.get( i ) );
//
//		transformed.add( Views.interval( transformedRA, transformedInterval ) );

	}
}
