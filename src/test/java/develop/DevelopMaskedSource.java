package develop;

import bdv.util.BdvFunctions;
import bdv.util.RandomAccessibleIntervalSource;
import bdv.util.volatiles.VolatileTypeMatcher;
import bdv.util.volatiles.VolatileViews;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Volatile;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.ByteArray;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.roi.RealMaskRealInterval;
import net.imglib2.roi.geom.GeomMasks;
import net.imglib2.roi.geom.real.WritableBox;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import org.embl.mobie.lib.source.mask.MaskedSource;

public class DevelopMaskedSource
{
    public static void main(String[] args) {

        long[] dimensions = new long[] { 100, 100, 100 };

        RandomAccessibleInterval< UnsignedByteType > rai = ArrayImgs.unsignedBytes( dimensions );

        rai.forEach( pixel -> pixel.set( (int) ( Math.random() * 255) ) );

        // Wrap the image in a Source
        AffineTransform3D scale = new AffineTransform3D();
        scale.scale( 0.1 );
        RandomAccessibleIntervalSource< UnsignedByteType > source
                = new RandomAccessibleIntervalSource<>(
                    rai,
                    new UnsignedByteType(),
                    scale,
                "Test Source" );

        WritableBox writableBox = GeomMasks.closedBox( new double[]{ 20, 20, 20 }, new double[]{ 80, 80, 80 } );
        RealMaskRealInterval transformed = writableBox.transform( scale );

        MaskedSource< UnsignedByteType > maskedSource = new MaskedSource<>( source, "test", transformed );
        BdvFunctions.show( maskedSource );
    }
}
