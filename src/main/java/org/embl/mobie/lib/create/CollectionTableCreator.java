package org.embl.mobie.lib.create;

import ij.IJ;
import loci.common.services.ServiceFactory;
import loci.formats.IFormatReader;
import loci.formats.ImageReader;
import loci.formats.meta.IMetadata;
import loci.formats.services.OMEXMLService;
import ome.xml.model.primitives.Color;
import org.apache.commons.io.FilenameUtils;
import org.embl.mobie.lib.util.MoBIEHelper;
import org.jetbrains.annotations.NotNull;
import tech.tablesaw.api.IntColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.embl.mobie.command.create.CreateMoBIECollectionTableCommand.*;

public class CollectionTableCreator
{

    private final File[] imageFiles;
    private final File outputFolder;
    private final String viewLayout;
    private final String regExp;
    private static final String[] COLOR_STRINGS = new String[]{"Blue", "Green", "Red", "White"};

    public CollectionTableCreator( File[] imageFiles, File outputFolder, String viewLayout, String regExp )
    {
        this.imageFiles = imageFiles;
        this.outputFolder = outputFolder;
        this.viewLayout = viewLayout;
        this.regExp = regExp;
    }

    public Table createTable()
    {
        boolean doGrid = viewLayout.equals( "Grid" );

        final Pattern pattern = Pattern.compile( regExp );
        final List< String > regExpGroups = MoBIEHelper.getNamedGroups( regExp );
        int groupCount = regExpGroups.size();

        // Table columns
        ArrayList< String > uris = new ArrayList<>();
        ArrayList< Integer > channels = new ArrayList<>();
        ArrayList< String > colors = new ArrayList<>();
        ArrayList< String > displays = new ArrayList<>();
        ArrayList< String > views = new ArrayList<>();
        ArrayList< String > gridNames = new ArrayList<>();
        ArrayList< String > gridPositions = new ArrayList<>();
        ArrayList< ArrayList< String > > regExpValuesList = new ArrayList<>();
        for ( int groupIndex = 0; groupIndex < groupCount; groupIndex++ )
            regExpValuesList.add( new ArrayList<>() );

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

                int numChannels = reader.getSizeC();

                // Match regular expressions
                if ( MoBIEHelper.notNullOrEmpty( regExp ) )
                {
                    final Matcher matcher = pattern.matcher( imageFile.getName() );
                    if ( ! matcher.matches() )
                    {
                        System.err.println( "Could not match regex for " + imageFile.getName() );
                        for ( int groupIndex = 0; groupIndex < groupCount; groupIndex++ )
                        {
                            for ( int channelIndex = 0; channelIndex < numChannels; channelIndex++ )
                            {
                                regExpValuesList.get( groupIndex ).add( "???" );
                            }
                        }
                    } else
                    {
                        for ( int groupIndex = 0; groupIndex < groupCount; groupIndex++ )
                        {
                            for ( int channelIndex = 0; channelIndex < numChannels; channelIndex++ )
                            {
                                regExpValuesList.get( groupIndex ).add( matcher.group( groupIndex + 1 ) );
                            }
                        }
                    }
                }

                // We could convert to OME-Zarr, using
                // OMEZarrWriter.write( imp, filePath, getImageType( imageType ), overwrite );
                // I would however do less chunking, maybe 10 MB instead of one

                // Channel loop
                for ( int c = 0; c < numChannels; c++ )
                {
                    String channelName = getChannelName( metadata, c );
                    //IJ.log( "Channel " + c + " name: " + channelName  );

                    String colorString = getColorString( metadata, c, numChannels );
                    //IJ.log( "Channel " + c + " color: " + channelName  );

                    // Add to table lists
                    if ( viewLayout.equals( TOGETHER ) || viewLayout.equals( GRID )  )
                        views.add( "all data" );
                    else if ( viewLayout.equals( INDIVIDUAL ) )
                        views.add( FilenameUtils.removeExtension( imageFile.getName() ) );

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
        } // file loop
        
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

        if ( MoBIEHelper.notNullOrEmpty( regExp ) )
        {
            for ( int groupIndex = 0; groupIndex < groupCount; groupIndex++ )
            {
                StringColumn stringColumn = StringColumn.create(
                        regExpGroups.get( groupIndex ),
                        regExpValuesList.get( groupIndex )
                );
                table.addColumns( stringColumn );
            }
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
