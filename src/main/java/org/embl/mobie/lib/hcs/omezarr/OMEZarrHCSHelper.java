package org.embl.mobie.lib.hcs.omezarr;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.embl.mobie.io.util.IOHelper;
import org.embl.mobie.lib.serialize.JsonHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
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
        List< String > imageSitePaths = new ArrayList<>();
        Gson gson = JsonHelper.buildGson(false);

        String plateUri = hcsDirectory;
        final String plateJson = IOHelper.read( plateUri + ZATTRS );
        //System.out.println( plateJson );
        HCSMetadata hcsMetadata = gson.fromJson(plateJson, new TypeToken< HCSMetadata >() {}.getType());

        int i = 0;
        for ( Well well : hcsMetadata.plate.wells )
        {
            System.out.println( "Parsing well: " + well.path );
            String wellPath = well.path;
            String wellUri = IOHelper.combinePath( plateUri, wellPath);
            final String wellJson = IOHelper.read( wellUri + ZATTRS );
            WellMetadata wellMetadata = gson.fromJson( wellJson, new TypeToken< WellMetadata >() {}.getType() );

            for ( Image image : wellMetadata.well.images )
            {
                String imageUri = IOHelper.combinePath( wellUri, image.path );
                imageSitePaths.add( imageUri );
            }

            i++;
            if ( i > 1 ) break;
        }

        return imageSitePaths;
    }
}
