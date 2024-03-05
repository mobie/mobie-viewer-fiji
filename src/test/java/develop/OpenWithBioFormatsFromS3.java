/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2024 EMBL
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
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
