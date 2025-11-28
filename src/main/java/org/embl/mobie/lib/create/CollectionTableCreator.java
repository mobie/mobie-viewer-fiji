package org.embl.mobie.lib.create;

import ij.IJ;
import loci.common.services.ServiceFactory;
import loci.formats.FormatTools;
import loci.formats.IFormatReader;
import loci.formats.ImageReader;
import loci.formats.meta.IMetadata;
import loci.formats.services.OMEXMLService;
import ome.units.quantity.Length;
import ome.units.unit.Unit;
import ome.xml.model.primitives.Color;
import org.apache.commons.io.FilenameUtils;
import org.ejml.equation.IntegerSequence;
import org.embl.mobie.lib.util.MoBIEHelper;
import org.jetbrains.annotations.NotNull;
import tech.tablesaw.api.FloatColumn;
import tech.tablesaw.api.IntColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

import java.io.File;
import java.nio.*;
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

        // Create table
        Table table = Table.create( "MoBIE collection table" );
        StringColumn uriCol = StringColumn.create( "uri" );
        IntColumn channelCol = IntColumn.create( "channel" );
        StringColumn viewCol = StringColumn.create( "view" );
        StringColumn displayCol = StringColumn.create( "display" );
        StringColumn colorCol = StringColumn.create( "color" );
        StringColumn gridCol = StringColumn.create( "grid" );
        StringColumn gridPos = StringColumn.create( "grid_position" );
        FloatColumn pixelSizeXCol = FloatColumn.create( "pixel_size_x" );
        FloatColumn pixelSizeYCol = FloatColumn.create( "pixel_size_y" );
        FloatColumn pixelSizeZCol = FloatColumn.create( "pixel_size_z" );
        FloatColumn imageSizeXCol = FloatColumn.create( "image_size_x" );
        FloatColumn imageSizeYCol = FloatColumn.create( "image_size_y" );
        FloatColumn imageSizeZCol = FloatColumn.create( "image_size_z" );
        StringColumn pixelUnitCol = StringColumn.create( "pixel_unit" );
        StringColumn contrastCol = StringColumn.create( "contrast_limits" );

        table.addColumns( uriCol, pixelUnitCol, imageSizeXCol, imageSizeYCol, imageSizeZCol, pixelSizeXCol, pixelSizeYCol, pixelSizeZCol,  channelCol, viewCol, displayCol, colorCol, contrastCol );

        if ( doGrid )
        {
            table.addColumns( gridCol, gridPos );
        }

        ArrayList< ArrayList< String > > regExpValuesList = new ArrayList<>();
        for ( int groupIndex = 0; groupIndex < groupCount; groupIndex++ )
            regExpValuesList.add( new ArrayList<>() );

        double gridSize = Math.ceil( Math.sqrt( imageFiles.length ) );

        for ( int fileIndex = 0; fileIndex < imageFiles.length; fileIndex++ )
        {
            File imageFile = imageFiles[ fileIndex ];
            IJ.log("\nAnalysing " + imageFile );

            try
            {
//                ImageDataFormat imageDataFormat = ImageDataFormat.fromPath( imageFile.getAbsolutePath() );
//                IJ.log( "Image format: " + imageDataFormat );
//                ImageData< ? > imageData = ImageDataOpener.open( imageFile.getAbsolutePath(), imageDataFormat, new SharedQueue( 1 ) );
//                int numDatasets = imageData.getNumDatasets();
//                IJ.log( "numDatasets: " + numDatasets );

                // Create reader with metadata support
                ServiceFactory factory = new ServiceFactory();
                OMEXMLService service = factory.getInstance( OMEXMLService.class );
                IMetadata metadata = service.createOMEXMLMetadata();

                IFormatReader reader = new ImageReader();
                reader.setMetadataStore( metadata );
                reader.setId( imageFile.getAbsolutePath() );
                if( reader.getSeriesCount() > 1 )
                    IJ.log( "WARNING: contains " + reader.getSeriesCount() + " datasets; only the first one will be considered!" );

                // Analyse image dimensions
                int numChannels = reader.getSizeC();
                IJ.log( "numChannels: " + numChannels );
                Unit< Length > pixelUnit = metadata.getPixelsPhysicalSizeX( 0 ).unit();
                Number pixelSizeX = metadata.getPixelsPhysicalSizeX( 0 ).value();
                Number pixelSizeY = metadata.getPixelsPhysicalSizeY( 0 ).value();
                Number pixelSizeZ = metadata.getPixelsPhysicalSizeZ( 0 ).value();
                float imageSizeX = metadata.getPixelsSizeX( 0 ).getNumberValue().intValue() * pixelSizeX.floatValue();
                float imageSizeY = metadata.getPixelsSizeY( 0 ).getNumberValue().intValue() * pixelSizeX.floatValue();
                float imageSizeZ = metadata.getPixelsSizeZ( 0 ).getNumberValue().intValue() * pixelSizeX.floatValue();
                FormatTools.getBytesPerPixel( reader.getPixelType() ); // FIXME
                String pixelUnitSymbol = pixelUnit.getSymbol();
                IJ.log( "Pixel unit: " + pixelUnitSymbol );
                IJ.log( "Pixel size (x, y, z, unit): " + pixelSizeX + ", " + pixelSizeY + ", " + pixelSizeZ + ", " + pixelUnit );

                // Append image dimensions to table
                for ( int c = 0; c < numChannels; c++ )
                {
                    // Same for all channels
                    uriCol.append( imageFile.getAbsolutePath() );
                    channelCol.append( c );
                    pixelSizeXCol.append( pixelSizeX.floatValue() );
                    pixelSizeYCol.append( pixelSizeY.floatValue() );
                    pixelSizeZCol.append( pixelSizeZ.floatValue() );
                    pixelUnitCol.append( pixelUnitSymbol );
                    imageSizeXCol.append( imageSizeX );
                    imageSizeYCol.append( imageSizeY );
                    imageSizeZCol.append( imageSizeZ );
                }

                // Analyse image contrast
                // Load central z-slice pixels for each channel
                int sizeZ = reader.getSizeZ();
                int centralZ = Math.max( 0, sizeZ / 2 );
                //IJ.log( "Central z-slice index: " + centralZ + " (sizeZ=" + sizeZ + ")" );
                for ( int c = 0; c < numChannels; c++ )
                {
                    int planeIndex = reader.getIndex( centralZ, c, 0 ); // z, c, t
                    byte[] sliceBytes = reader.openBytes( planeIndex );
                    float[] floats = bytesToFloatArray(reader, sliceBytes);
                    float[] quantiles = calculateQuantiles(floats, 0.05, 0.95);
                    contrastCol.append( "(" + quantiles[0] + "," + quantiles[1] + ")" );
                }

                // Extract metadata from file names
                if ( MoBIEHelper.notNullOrEmpty( regExp ) )
                {
                    final Matcher matcher = pattern.matcher( imageFile.getName() );
                    if ( ! matcher.matches() )
                    {
                        for ( int groupIndex = 0; groupIndex < groupCount; groupIndex++ )
                        {
                            IJ.log( regExpGroups.get( groupIndex ) + ": ??? (could not match regular expression)");
                            for ( int channelIndex = 0; channelIndex < numChannels; channelIndex++ )
                            {
                                regExpValuesList.get( groupIndex ).add( "???" );
                            }
                        }
                    }
                    else
                    {
                        for ( int groupIndex = 0; groupIndex < groupCount; groupIndex++ )
                        {
                            IJ.log( regExpGroups.get( groupIndex ) + ": " + matcher.group( groupIndex + 1 ) );
                            for ( int channelIndex = 0; channelIndex < numChannels; channelIndex++ )
                            {
                                regExpValuesList.get( groupIndex ).add( matcher.group( groupIndex + 1 ) );
                            }
                        }
                    }
                }

                // Channels
                for ( int c = 0; c < numChannels; c++ )
                {
                    String channelName = getChannelName( metadata, c );
                    String colorString = getColorString( metadata, c, numChannels );

                    // Add to table lists
                    if ( viewLayout.equals( TOGETHER ) || viewLayout.equals( GRID )  )
                        viewCol.append( "all data" );
                    else if ( viewLayout.equals( INDIVIDUAL ) )
                        viewCol.append( FilenameUtils.removeExtension( imageFile.getName() ) );

                    displayCol.append( channelName );
                    colorCol.append( colorString );

                    if ( doGrid )
                    {
                        gridCol.append( "grid" );
                        int x = ( int ) (fileIndex % gridSize);
                        int y = ( int ) (fileIndex / gridSize);
                        gridPos.append( "("+x+","+y+")");
                    }

                    IJ.log( "Channel " + c + "; name: " + channelName + "; color: " + colorString  );
                }

                reader.close();
            }
            catch ( Exception e )
            {
                e.printStackTrace();
            }
        } // file loop


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

        IJ.log( "\nCollection table creation: Done!" );
        return table;
    }

    /**
     * Calculate quantiles from pixel values, ignoring NaN values.
     *
     * @param values The pixel values
     * @param lowerQuantile Lower quantile (e.g., 0.01 for 1st percentile)
     * @param upperQuantile Upper quantile (e.g., 0.99 for 99th percentile)
     * @return Array containing [lowerValue, upperValue], or null if no valid values
     */
    private static float[] calculateQuantiles(float[] values, double lowerQuantile, double upperQuantile) {
        if (values == null || values.length == 0) {
            return null;
        }

        // Filter out NaN values and sort
        List<Float> validValues = new ArrayList<>();
        for (float v : values) {
            if (!Float.isNaN(v)) {
                validValues.add(v);
            }
        }

        if (validValues.isEmpty()) {
            return null;
        }

        validValues.sort(Float::compareTo);

        int lowerIndex = (int) Math.round(lowerQuantile * (validValues.size() - 1));
        int upperIndex = (int) Math.round(upperQuantile * (validValues.size() - 1));

        return new float[] {
                validValues.get(lowerIndex),
                validValues.get(upperIndex)
        };
    }


    private static float[] bytesToFloatArray( IFormatReader reader, byte[] sliceBytes )
    {
        if ( sliceBytes == null ) return new float[0];

        int pixelType = reader.getPixelType();
        boolean littleEndian = reader.isLittleEndian();
        ByteBuffer bb = ByteBuffer.wrap( sliceBytes );
        bb.order( littleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN );

        switch ( pixelType )
        {
            case FormatTools.UINT8:
            case FormatTools.INT8: {
                float[] f = new float[sliceBytes.length];
                for ( int i = 0; i < sliceBytes.length; i++ ) f[i] = sliceBytes[i] & 0xFF;
                return f;
            }
            case FormatTools.UINT16:
            case FormatTools.INT16: {
                ShortBuffer sb = bb.asShortBuffer();
                float[] f = new float[sb.remaining()];
                for ( int i = 0; i < f.length; i++ )
                {
                    short s = sb.get();
                    f[i] = ( pixelType == FormatTools.UINT16 ) ? ( s & 0xFFFF ) : s;
                }
                return f;
            }
            case FormatTools.UINT32:
            case FormatTools.INT32: {
                IntBuffer ib = bb.asIntBuffer();
                float[] f = new float[ib.remaining()];
                for ( int i = 0; i < f.length; i++ )
                {
                    int v = ib.get();
                    f[i] = ( pixelType == FormatTools.UINT32 ) ? (float)( v & 0xFFFFFFFFL ) : v;
                }
                return f;
            }
            case FormatTools.FLOAT: {
                FloatBuffer fb = bb.asFloatBuffer();
                float[] f = new float[fb.remaining()];
                fb.get( f );
                return f;
            }
            case FormatTools.DOUBLE: {
                DoubleBuffer db = bb.asDoubleBuffer();
                float[] f = new float[db.remaining()];
                for ( int i = 0; i < f.length; i++ ) f[i] = (float) db.get();
                return f;
            }
            default: {
                float[] f = new float[sliceBytes.length];
                for ( int i = 0; i < f.length; i++ ) f[i] = sliceBytes[i];
                return f;
            }
        }
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
                colorString = String.format( "a%d-r%d-g%d-b%d",
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
