package org.embl.mobie.lib.create;

import bdv.cache.SharedQueue;
import bdv.viewer.Source;
import ij.IJ;
import loci.formats.FormatTools;
import loci.formats.IFormatReader;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Volatile;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Cast;
import net.imglib2.util.Intervals;
import net.imglib2.util.ValuePair;
import net.imglib2.view.Views;
import org.apache.commons.io.FilenameUtils;
import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.io.ImageDataOpener;
import org.embl.mobie.io.imagedata.ImageData;
import org.embl.mobie.lib.util.MoBIEHelper;
import org.embl.mobie.lib.util.ThreadHelper;
import org.jetbrains.annotations.NotNull;
import tech.tablesaw.api.*;

import java.io.File;
import java.nio.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
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

    // TODO: add count of highest pixel value (saturation)
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
        StringColumn nameCol = StringColumn.create( "channel_name" );
        StringColumn colorCol = StringColumn.create( "color" );
        StringColumn gridCol = StringColumn.create( "grid" );
        StringColumn gridPos = StringColumn.create( "grid_position" );
        FloatColumn pixelSizeXCol = FloatColumn.create( "pixel_size_x" );
        FloatColumn pixelSizeYCol = FloatColumn.create( "pixel_size_y" );
        FloatColumn pixelSizeZCol = FloatColumn.create( "pixel_size_z" );
        LongColumn imageSizeXCol = LongColumn.create( "num_pixels_x" );
        LongColumn imageSizeYCol = LongColumn.create( "num_pixels_y" );
        LongColumn imageSizeZCol = LongColumn.create( "num_pixels_z" );
        StringColumn pixelUnitCol = StringColumn.create( "pixel_unit" );
        StringColumn contrastCol = StringColumn.create( "contrast_limits" );

        table.addColumns( uriCol, nameCol, pixelUnitCol, pixelSizeXCol, pixelSizeYCol, pixelSizeZCol, imageSizeXCol, imageSizeYCol, imageSizeZCol, channelCol, viewCol, displayCol, colorCol, contrastCol );

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

            ImageDataFormat imageDataFormat = ImageDataFormat.fromPath( imageFile.getAbsolutePath() );
            IJ.log( "Image format: " + imageDataFormat );
            ImageData< ? > imageData = ImageDataOpener.open( imageFile.getAbsolutePath(), imageDataFormat, new SharedQueue( ThreadHelper.getNumThreads() ) );
            int numDatasets = imageData.getNumDatasets();
            IJ.log( "Number of datasets: " + numDatasets );

            // Grid
            //
            if ( doGrid )
            {
                for ( int datasetIndex = 0; datasetIndex < numDatasets; datasetIndex++ )
                {
                    gridCol.append( "grid" );
                    int x = ( int ) ( fileIndex % gridSize );
                    int y = ( int ) ( fileIndex / gridSize );
                    gridPos.append( "(" + x + "," + y + ")" );
                }
            }

            // Image dimensions
            //
            for ( int datasetIndex = 0; datasetIndex < numDatasets; datasetIndex++ )
            {
                Source< ? extends Volatile< ? > > source = imageData.getSourcePair( 0 ).getB();
                VoxelDimensions voxelDimensions = source.getVoxelDimensions();

                uriCol.append( imageFile.getAbsolutePath() );
                channelCol.append( datasetIndex );
                pixelSizeXCol.append( (float) voxelDimensions.dimension( 0 ) );
                pixelSizeYCol.append( (float) voxelDimensions.dimension( 1 ) );
                pixelSizeZCol.append( (float) voxelDimensions.dimension( 2 ) );
                pixelUnitCol.append( voxelDimensions.unit() );
                imageSizeXCol.append( source.getSource( 0, 0 ).dimension( 0 ) );
                imageSizeYCol.append( source.getSource( 0, 0 ).dimension( 1 ) );
                imageSizeZCol.append( source.getSource( 0, 0 ).dimension( 2 ) );

                if ( viewLayout.equals( TOGETHER ) || viewLayout.equals( GRID )  )
                    viewCol.append( "all data" );
                else if ( viewLayout.equals( INDIVIDUAL ) )
                    viewCol.append( FilenameUtils.removeExtension( imageFile.getName() ) );
            }

            // Extract metadata from file names
            //
            if ( MoBIEHelper.notNullOrEmpty( regExp ) )
            {
                final Matcher matcher = pattern.matcher( imageFile.getName() );
                if ( ! matcher.matches() )
                {
                    for ( int groupIndex = 0; groupIndex < groupCount; groupIndex++ )
                    {
                        IJ.log( regExpGroups.get( groupIndex ) + ": ??? (could not match regular expression)");
                        for ( int datasetIndex = 0; datasetIndex < numDatasets; datasetIndex++ )
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
                        for ( int datasetIndex = 0; datasetIndex < numDatasets; datasetIndex++ )
                        {
                            regExpValuesList.get( groupIndex ).add( matcher.group( groupIndex + 1 ) );
                        }
                    }
                }
            }

            // Channel metadata
            //
            for ( int datasetIndex = 0; datasetIndex < numDatasets; datasetIndex++ )
            {
                String datasetName = getDatasetName( imageData, datasetIndex );
                String colorString = getColorString( imageData, datasetIndex );
                ValuePair< Double, Double > contrastLimits = getContrastLimits( imageData, datasetIndex );

                nameCol.append( datasetName );
                displayCol.append( "Channel_" + datasetIndex );
                colorCol.append( colorString );
                contrastCol.append( "(" + contrastLimits.getA() + "," + contrastLimits.getB() + ")" );
            }

        } // file loop


        // Add regexp columns to table
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

    private static ValuePair< Double, Double > getContrastLimits( ImageData< ? > imageData, int datasetIndex )
    {
        try
        {
            ValuePair< Double, Double > valuePair = new ValuePair<>( imageData.getMetadata( datasetIndex ).minIntensity(),
                    imageData.getMetadata( datasetIndex ).maxIntensity() );
            IJ.log("Fetched contrast limits from metadata.");
            return valuePair;
        }
        catch ( Exception e )
        {
            ValuePair< Double, Double > minMax = getMinMax( imageData.getSourcePair( 0 ).getA() );
            IJ.log("Fetched contrast limits from data.");
            return minMax;
        }
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

    private static @NotNull String getDatasetName( ImageData imageData, int datasetIndex )
    {
        return imageData.getName( datasetIndex );
    }

    private static @NotNull String getColorString( ImageData imageData, int datasetIndex )
    {
        try
        {
            ARGBType argbType = imageData.getMetadata( datasetIndex ).getColor();
            return String.format( "a%d-r%d-g%d-b%d",
                    ARGBType.alpha( argbType.get() ),
                    ARGBType.red( argbType.get() ),
                    ARGBType.green( argbType.get() ),
                    ARGBType.blue( argbType.get() )
            );
        }
        catch ( Exception e )
        {
            return "White";
        }
    }

    //taken from LabKit
    //https://github.com/juglab/labkit-ui/blob/01a5c8058459a0d1a2eedc10f7212f64e021f893/src/main/java/sc/fiji/labkit/ui/bdv/BdvAutoContrast.java#L51
    private static ValuePair<Double, Double> getMinMax(final Source<?> src)
    {
        int level = src.getNumMipmapLevels() - 1;
        RandomAccessibleInterval<?> source = src.getSource(0, level);
        if ( source.getType() instanceof RealType )
            return getMinMaxForRealType( Cast.unchecked(source));
        return new ValuePair<>(0.0, 255.0);
    }

    private static ValuePair<Double, Double> getMinMaxForRealType(
            RandomAccessibleInterval<? extends RealType<?>> source)
    {
        Cursor<? extends RealType<?>> cursor = Views.iterable(source).cursor();
        if (!cursor.hasNext()) return new ValuePair<>(0.0, 255.0);
        long stepSize = Intervals.numElements(source) / 10000 + 1;
        int randomLimit = (int) Math.min(Integer.MAX_VALUE, stepSize);
        Random random = new Random(42);
        double min = cursor.next().getRealDouble();
        double max = min;
        while (cursor.hasNext()) {
            double value = cursor.get().getRealDouble();
            cursor.jumpFwd(stepSize + random.nextInt(randomLimit));
            min = Math.min(min, value);
            max = Math.max(max, value);
        }
        return new ValuePair<>(min, max);
    }
}
