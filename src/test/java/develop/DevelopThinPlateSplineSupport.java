package develop;

import bdv.cache.SharedQueue;
import bdv.img.WarpedSource;
import bdv.util.BdvFunctions;
import bdv.viewer.Source;
import bigwarp.landmarks.LandmarkTableModel;
import bigwarp.transforms.BigWarpTransform;
import bigwarp.transforms.io.TransformWriterJson;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.imglib2.Volatile;
import net.imglib2.realtransform.InvertibleRealTransform;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.util.Pair;
import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.io.ImageDataOpener;
import org.embl.mobie.io.imagedata.ImageData;
import org.embl.mobie.lib.serialize.BigWarpLandmarks;

import java.io.File;
import java.io.IOException;

public class DevelopThinPlateSplineSupport
{
    public static < T extends NumericType< T > & NativeType< T > >  void main( String[] args ) throws IOException
    {
        File mriStackFile = new File( "src/test/resources/mri-stack.zip" );
        ImageData< T > imageData = ImageDataOpener.open( mriStackFile.getAbsolutePath(), ImageDataFormat.ImageJ, new SharedQueue( 1 ) );
        Pair< Source< T >, Source< ? extends Volatile< T > > > sourcePair = imageData.getSourcePair( 0 );
        WarpedSource< ? extends Volatile< T > > warpedVolatileSource = new WarpedSource<>( sourcePair.getB(), "_vs" );

        LandmarkTableModel ltm = new LandmarkTableModel( 3 );
        ltm.load( new File("src/test/resources/bigwarp_mri_stack_landmarks.csv") );
        String jsonString = ltm.toJson().toString();
        JsonElement json = ltm.toJson();
        JsonElement jsonElement = JsonParser.parseString( jsonString );
        BigWarpLandmarks landmarks = BigWarpLandmarks.fromJson( jsonString );
        System.out.println(landmarks);
        LandmarkTableModel ltm2 = new LandmarkTableModel( landmarks.getNumDimensions() );
        //ltm2.fromJson(  );
        // to convert back to JSON:
        String producedJson = landmarks.toJson();
        System.out.println(producedJson);

        ltm.fromJson( jsonElement );
        System.out.println( jsonString );

        BigWarpTransform bigWarpTransform = new BigWarpTransform( ltm, BigWarpTransform.TPS );

        TransformWriterJson.write( ltm, bigWarpTransform, new File("src/test/resources/bigwarp_mri_stack_landmarks.json")  );

        InvertibleRealTransform invertibleRealTransform = bigWarpTransform.getTransformation();
        warpedVolatileSource.updateTransform( invertibleRealTransform );
        warpedVolatileSource.setIsTransformed( true );

        //BdvFunctions.show( warpedVolatileSource );

        // Things to look at:
        // BigWarpApplyTests.class: transformToTarget
    }
}
