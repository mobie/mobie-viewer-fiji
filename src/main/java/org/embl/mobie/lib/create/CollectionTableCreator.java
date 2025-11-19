package org.embl.mobie.lib.create;

import ij.IJ;
import loci.common.services.ServiceFactory;
import loci.formats.IFormatReader;
import loci.formats.ImageReader;
import loci.formats.meta.IMetadata;
import loci.formats.services.OMEXMLService;
import ome.xml.model.primitives.Color;
import org.jetbrains.annotations.NotNull;
import tech.tablesaw.api.IntColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

import java.io.File;
import java.util.ArrayList;

public class CollectionTableCreator
{

    private final File[] imageFiles;
    private final File outputFolder;
    private final String gridLayout;
    private static final String[] COLOR_STRINGS = new String[]{"Blue", "Green", "Red", "White"};

    public CollectionTableCreator( File[] imageFiles, File outputFolder, String gridLayout )
    {
        this.imageFiles = imageFiles;
        this.outputFolder = outputFolder;
        this.gridLayout = gridLayout;
    }

    public Table createTable()
    {
        boolean doGrid = gridLayout.equals( "Yes" );

        // Table columns
        ArrayList< String > uris = new ArrayList<>();
        ArrayList< Integer > channels = new ArrayList<>();
        ArrayList< String > colors = new ArrayList<>();
        ArrayList< String > displays = new ArrayList<>();
        ArrayList< String > views = new ArrayList<>();
        ArrayList< String > gridNames = new ArrayList<>();
        ArrayList< String > gridPositions = new ArrayList<>();

        double gridSize = Math.ceil( Math.sqrt( imageFiles.length ) );

        for ( int fileIndex = 0; fileIndex < imageFiles.length; fileIndex++ )
        {
            File imageFile = imageFiles[ fileIndex ];
            IJ.log("Parsing " + imageFile );
            try
            {
                // Create reader with metadata support
                ServiceFactory factory = new ServiceFactory();
                OMEXMLService service = factory.getInstance( OMEXMLService.class );
                IMetadata metadata = service.createOMEXMLMetadata();

                IFormatReader reader = new ImageReader();
                reader.setMetadataStore( metadata );
                reader.setId( imageFile.getAbsolutePath() );

                int sizeC = reader.getSizeC();

                // Extract channel names and colors
                for ( int c = 0; c < sizeC; c++ )
                {
                    String channelName = getChannelName( metadata, c );
                    IJ.log( "Channel " + c + " name: " + channelName  );

                    String colorString = getColorString( metadata, c, sizeC );
                    IJ.log( "Channel " + c + " color: " + channelName  );

                    // Add to table lists
                    views.add( "all data" );
                    uris.add( imageFile.getAbsolutePath() );
                    channels.add( c );
                    displays.add( channelName );
                    colors.add( colorString );
                    if ( doGrid )
                    {
                        gridNames.add( "grid" );
                        int x = ( int ) (fileIndex % gridSize);
                        int y = ( int ) (fileIndex / gridSize);
                        gridPositions.add( "("+x+","+y+")");
                    }
                }

                reader.close();
            } catch ( Exception e )
            {
                e.printStackTrace();
            }
        }
        
        // Create table
        Table table = Table.create( "MoBIE collection table" );

        // Add columns to table
        StringColumn uriCol = StringColumn.create( "uri", uris );
        IntColumn channelCol = IntColumn.create( "channel", channels.stream().mapToInt( Integer::intValue ).toArray() );
        StringColumn viewCol = StringColumn.create( "view", views );
        StringColumn displayCol = StringColumn.create( "display", displays );
        StringColumn colorCol = StringColumn.create( "color", colors );

        table.addColumns( uriCol, channelCol, viewCol, displayCol, colorCol );

        if ( doGrid )
        {
            StringColumn gridCol = StringColumn.create( "grid", gridNames );
            StringColumn gridPos = StringColumn.create( "grid_position", gridPositions );
            table.addColumns( gridCol, gridPos );
        }

        return table;
    }

    private static @NotNull String getChannelName( IMetadata metadata, int c )
    {
        // Get channel name
        String channelName = metadata.getChannelName( 0, c );
        if ( channelName == null || channelName.isEmpty() )
        {
            channelName = "Channel_" + c;
        }
        return channelName;
    }

    private static @NotNull String getColorString( IMetadata metadata, int c, int sizeC )
    {
        // Get channel color
        Color channelColor = metadata.getChannelColor( 0, c );
        String colorString = "White"; // default
        if ( sizeC > 1 )
        {
            if ( channelColor != null )
            {
                colorString = String.format( "a(%d)-r(%d)-g(%d)-b(%d)",
                        channelColor.getAlpha(),
                        channelColor.getRed(),
                        channelColor.getGreen(),
                        channelColor.getBlue()
                );
            } else
            {
                colorString = COLOR_STRINGS[ c ];
            }
        }
        return colorString;
    }
}
