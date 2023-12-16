package develop;

import ij.ImagePlus;
import loci.common.ByteArrayHandle;
import loci.common.DebugTools;
import loci.common.Location;
import org.embl.mobie.io.util.IOHelper;
import org.embl.mobie.lib.MoBIEHelper;

import java.io.*;

public class OpenWithBioFormatsFromS3
{
    public static void main( String[] args ) throws IOException
    {
        DebugTools.setRootLevel( "OFF" );

        long start;

        for ( int i = 0; i < 5; i++ )
        {
            // Mapped FILE
            start = System.currentTimeMillis();
            File inputFile = new File( "/Users/tischer/Downloads/incu-test-data/2207/19/1110/262/B3-1-C2.tif" );
            int fileSize = ( int ) inputFile.length();
            DataInputStream in = new DataInputStream( new FileInputStream( inputFile ) );
            byte[] inBytes = new byte[ fileSize ];
            in.readFully( inBytes );
            //System.out.println( fileSize + " bytes read from File." );
            Location.mapFile( "file.tif", new ByteArrayHandle( inBytes ) );
            ImagePlus imagePlusMapped = IOHelper.openWithBioFormats( "file.tif", 0 );
            System.out.println( "Mapped File [ms]: " + ( System.currentTimeMillis() - start ) );

            // S3
            start = System.currentTimeMillis();
            InputStream inputStream = IOHelper.getInputStream( "https://s3.embl.de/i2k-2020/incu-test-data/2207/19/1110/262/B3-1-C2.tif" );
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[ 1024 ];
            while ( ( nRead = inputStream.read( data, 0, data.length ) ) != -1 )
                buffer.write( data, 0, nRead );
            buffer.flush();
            byte[] byteArray = buffer.toByteArray();
            //System.out.println( byteArray.length + " bytes read from S3." );
            Location.mapFile( "s3.tif", new ByteArrayHandle( byteArray ) );
            ImagePlus imagePlusS3 = IOHelper.openWithBioFormats( "s3.tif", 0 );
            System.out.println( "S3 [ms]: " + ( System.currentTimeMillis() - start ) );
        }
    }
}
