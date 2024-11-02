package org.embl.mobie.lib.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.embl.mobie.lib.table.columns.CollectionTableConstants;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5URI;
import org.janelia.saalfeldlab.n5.universe.N5Factory;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.csv.CsvWriteOptions;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class OpenOrganelleCollectionTableCreator
{
    public static void main( String[] args ) throws URISyntaxException, ExecutionException, InterruptedException
    {
        String[] uris = {
                "s3://janelia-cosem-datasets/jrc_macrophage-2/jrc_macrophage-2.n5",
                "s3://janelia-cosem-datasets/jrc_hela-3/jrc_hela-3.n5",
                "s3://janelia-cosem-datasets/jrc_jurkat-1/jrc_jurkat-1.n5"
        };

        String imagesPath = "/Users/tischer/Documents/mobie-viewer-fiji/src/test/resources/uriToImages.json";

        // fetchImagesFromUris( imagesPath, uris );

        Map< String, List< String > > uriToImages = readImages( imagesPath );

        String tablePath = "/Users/tischer/Documents/mobie-viewer-fiji/src/test/resources/collections/open-organelle.txt";

        createCollectionTable( uriToImages, tablePath );

        System.exit( 0 );
    }

    public static Map< String, List< String > > uriToImages( String[] uris ) throws URISyntaxException, ExecutionException, InterruptedException
    {
        HashMap< String, List< String > > uriToDatasets = new HashMap<>();

        for ( String uri : uris )
        {
            System.out.println( "Finding images in " + uri );
            N5URI n5URI = new N5URI( uri );
            String containerPath = n5URI.getContainerPath();
            N5Reader n5 = new N5Factory().openReader( containerPath );
            String[] datasets = n5.deepListDatasets( "/", Executors.newCachedThreadPool() );

            List< String > images = Arrays.stream( datasets )
                    .filter( s -> s.endsWith( "/s0" ) )
                    .map( s -> s.replace( "/s0", "" ) )
                    .collect( Collectors.toList() );

            uriToDatasets.put( uri, images );
            System.out.println( "...found " + images.size() );
        }

        return uriToDatasets;
    }

    private static void fetchImagesFromUris( final String outputPath, final String[] uris ) throws URISyntaxException, ExecutionException, InterruptedException
    {
        // https://openorganelle.janelia.org/datasets

        Map< String, List< String > > uriToImages = uriToImages( uris );
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (FileWriter writer = new FileWriter( outputPath )) {
            gson.toJson(uriToImages, writer);
        } catch ( IOException e) {
            e.printStackTrace();
        }

        System.out.println("Wrote results to " + outputPath );
    }

    private static void createCollectionTable( Map< String, List< String > > uriToImages, final String tablePath )
    {
        String[] imagesAndLabels = { "em/fibsem-uint16", "labels/pm_seg" };

        Table table = Table.create( "OpenOrganelle" );

        int numVolumes = uriToImages.size();
        int numRows = numVolumes * ( imagesAndLabels.length );

        StringColumn uriColumn = StringColumn.create( CollectionTableConstants.URI, numRows );
        StringColumn typeColumn = StringColumn.create( CollectionTableConstants.TYPE, numRows );
        StringColumn gridColumn = StringColumn.create( CollectionTableConstants.GRID, numRows );
        StringColumn nameColumn = StringColumn.create( CollectionTableConstants.NAME, numRows );
        StringColumn contrastColumn = StringColumn.create( CollectionTableConstants.NAME, numRows );

        table.addColumns(
                uriColumn,
                nameColumn,
                typeColumn,
                gridColumn,
                contrastColumn );

        int rowIndex = 0;

        for ( String relativePath : imagesAndLabels )
        {
            for ( String uri : uriToImages.keySet() )
            {
                String sampleName = Arrays.stream( uri.split( "/" ) )
                        .filter( s -> s.contains( ".n5" ) )
                        .map( s -> s.replace( ".n5", "" ) )
                        .findFirst().get();

                String type = relativePath.contains( "labels" ) ?
                        CollectionTableConstants.LABELS :
                        CollectionTableConstants.INTENSITIES;

                String contrast = relativePath.contains( "labels" ) ?
                        "" :
                        "(18000;32000)";

                uriColumn.set( rowIndex, uri + "/" + relativePath ) ;
                typeColumn.set( rowIndex, type ) ;
                gridColumn.set( rowIndex, relativePath  );
                nameColumn.set( rowIndex, sampleName + "/" + relativePath );
                contrastColumn.set( rowIndex, contrast );

                rowIndex++;
            }
        }


        CsvWriteOptions options = CsvWriteOptions.builder( tablePath ).separator( '\t' ).build();
        table.write().usingOptions( options );

        System.out.println( "Written collection table to " + tablePath );
    }

    private static Map< String, List< String > > readImages( String imagesPath )
    {
        Gson gson = new Gson();
        Type mapType = new TypeToken<Map<String, List<String>>>() {}.getType();

        try ( FileReader reader = new FileReader(imagesPath)) {
            return gson.fromJson(reader, mapType);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
