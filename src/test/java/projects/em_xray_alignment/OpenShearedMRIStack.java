package projects.em_xray_alignment;

import net.imagej.ImageJ;
import net.imglib2.realtransform.AffineTransform3D;
import org.embl.mobie.MoBIE;
import org.embl.mobie.command.context.SIFTXYAlignCommand;
import org.embl.mobie.command.open.OpenMultipleImagesAndLabelsCommand;

import java.io.File;
import java.io.IOException;

public class OpenShearedMRIStack
{
    public static void main( String[] args ) throws IOException
    {
        AffineTransform3D shearTransformXY = new AffineTransform3D();
        shearTransformXY.set(
                0.9967208555433267, -0.1981387047703669, 0.0, 2.9848361329209316,
                0.00626758147615312, 1.0002354640459286, -0.0, -9.235423965765829,
                -0.0, 0.0, 1.0, -0.0 );

        AffineTransform3D rotation = new AffineTransform3D();
        rotation.rotate( 0, 90.0 * Math.PI / 180.0 );

        AffineTransform3D affineTransform3D = new AffineTransform3D();

        affineTransform3D.preConcatenate( rotation );
        affineTransform3D.preConcatenate( shearTransformXY );
        affineTransform3D.preConcatenate( rotation.inverse() );

        System.out.printf( "rotate " + rotation );
        System.out.printf( "final " + affineTransform3D );

        /*

        0.9967208555433267, -1.2132496529211612E-17, 0.1981387047703669, 2.9848361329209316,
        3.837786796583081E-19, 1.0, -1.4418014508029665E-20, -5.655066199221939E-16,
        -0.00626758147615312, -1.4418014508029665E-20, 1.0002354640459286, 9.235423965765829)

         */

//        // OpenerLogging.setLogging( true );
        final ImageJ imageJ = new ImageJ();
        imageJ.ui().showUI();
        OpenMultipleImagesAndLabelsCommand command = new OpenMultipleImagesAndLabelsCommand();
        command.image0 = new File("/Users/tischer/Desktop/em-xray/mri-stack-calibrated.tif");
        command.image1 = new File("/Users/tischer/Desktop/em-xray/mri-stack-calibrated-sheared.tif");
        command.run();
//

//        SIFTXYAlignCommand sift2DAlignCommand = new SIFTXYAlignCommand();
//        sift2DAlignCommand.bdvHandle = MoBIE.getInstance().getViewManager().getSliceViewer().getBdvHandle();
//        sift2DAlignCommand.run();
    }
}
