package org.embl.mobie.lib.hcs.omezarr;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import ij.IJ;
import org.embl.mobie.io.util.IOHelper;
import org.embl.mobie.lib.MoBIEHelper;
import org.embl.mobie.lib.ThreadHelper;
import org.embl.mobie.lib.serialize.JsonHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class OMEZarrHCSHelper
{
    public static final String ZATTRS = "/.zattrs";

    public static List< String > sitePathsFromFolderStructure( String hcsDirectory ) throws IOException
    {
        List< String > imageSitePaths;
        final int minDepth = 3;
        final int maxDepth = 3;
        final Path rootPath = Paths.get( hcsDirectory );
        final int rootPathDepth = rootPath.getNameCount();
        imageSitePaths = Files.walk( rootPath, maxDepth )
                .filter( e -> e.toFile().isDirectory() )
                .filter( e -> e.getNameCount() - rootPathDepth >= minDepth )
                .map( e -> e.toString() )
                .collect( Collectors.toList() );
        return imageSitePaths;
    }

    public static List< String > sitePathsFromMetadata( String hcsDirectory ) throws IOException
    {
        List< String > imageSitePaths = new CopyOnWriteArrayList<>();
        Gson gson = JsonHelper.buildGson(false);

        String plateUri = hcsDirectory;
        final String plateJson = IOHelper.read( plateUri + ZATTRS );
        //System.out.println( plateJson );
        HCSMetadata hcsMetadata = gson.fromJson(plateJson, new TypeToken< HCSMetadata >() {}.getType());

        int numWells = hcsMetadata.plate.wells.size();

        // lots of code for nice logging...
        AtomicInteger wellIndex = new AtomicInteger(0);
        AtomicInteger sourceLoggingModulo = new AtomicInteger(1);
        AtomicLong lastLogMillis = new AtomicLong( System.currentTimeMillis() );
        final long startTime = System.currentTimeMillis();
        IJ.log( "Parsing " + numWells + " wells..." );
        parseWells( hcsMetadata, wellIndex, numWells, sourceLoggingModulo, lastLogMillis, plateUri, gson, imageSitePaths );
        IJ.log( "Parsed " + numWells + " wells in " + (System.currentTimeMillis() - startTime) + " ms, using up to " + ThreadHelper.getNumIoThreads() + " thread(s).");

        return imageSitePaths;
    }

    private static void parseWells( HCSMetadata hcsMetadata, AtomicInteger wellIndex, int numWells, AtomicInteger sourceLoggingModulo, AtomicLong lastLogMillis, String plateUri, Gson gson, List< String > imageSitePaths )
    {
        ArrayList< Future< ? > > futures = ThreadHelper.getFutures();
        for ( Well well : hcsMetadata.plate.wells )
        {
            futures.add(
                ThreadHelper.ioExecutorService.submit( () ->
                    {
                        String log = MoBIEHelper.getLog( wellIndex, numWells, sourceLoggingModulo, lastLogMillis );
                        if ( log != null )
                            IJ.log( log + well.path );

                        String wellUri = IOHelper.combinePath( plateUri, well.path);
                        final String wellJson;
                        try
                        {
                            wellJson = IOHelper.read( wellUri + ZATTRS );
                        } catch ( IOException e )
                        {
                            throw new RuntimeException( e );
                        }
                        WellMetadata wellMetadata = gson.fromJson( wellJson, new TypeToken< WellMetadata >() {}.getType() );

                        for ( Image image : wellMetadata.well.images )
                        {
                            String imageUri = IOHelper.combinePath( wellUri, image.path );
                            imageSitePaths.add( imageUri );
                        }
                    }
                ) );
        }
        ThreadHelper.waitUntilFinished( futures );
    }
}
