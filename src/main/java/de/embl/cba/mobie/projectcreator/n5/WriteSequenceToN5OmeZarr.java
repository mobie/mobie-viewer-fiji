package de.embl.cba.mobie.projectcreator.n5;

import bdv.export.ExportMipmapInfo;
import bdv.export.ProgressWriter;
import bdv.export.ProgressWriterNull;
import bdv.export.SubTaskProgressWriter;
import bdv.img.cache.SimpleCacheArrayLoader;
import bdv.img.n5.N5ImageLoader;
import com.google.gson.GsonBuilder;
import de.embl.cba.mobie.n5.zarr.*;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicImgLoader;
import mpicbg.spim.data.generic.sequence.BasicSetupImgLoader;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.sequence.TimePoint;
import mpicbg.spim.data.sequence.ViewId;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.cache.img.ReadOnlyCachedCellImgFactory;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Cast;
import org.janelia.saalfeldlab.n5.*;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

import static de.embl.cba.mobie.n5.zarr.OmeZarrMultiscales.MULTI_SCALE_KEY;
import static net.imglib2.cache.img.ReadOnlyCachedCellImgOptions.options;

public class WriteSequenceToN5OmeZarr {

    private static final String ARRAY_DIMENSIONS_KEY = "_ARRAY_DIMENSIONS";

    /**
     * Create a ome zarr group containing image data from all views and all
     * timepoints in a chunked, mipmaped representation.
     *
     * @param seq
     *            description of the sequence to be stored as ome-zarr. (The
     *            {@link AbstractSequenceDescription} contains the number of
     *            setups and timepoints as well as an {@link BasicImgLoader}
     *            that provides the image data, Registration information is not
     *            needed here, that will go into the accompanying xml).
     * @param perSetupMipmapInfo
     *            this maps from setup {@link BasicViewSetup#getId() id} to
     *            {@link ExportMipmapInfo} for that setup. The
     *            {@link ExportMipmapInfo} contains for each mipmap level, the
     *            subsampling factors and subdivision block sizes.
     * @param compression
     *            n5 compression scheme.
     * @param zarrFile
     *            zarr file root.
     * @param loopbackHeuristic
     *            heuristic to decide whether to create each resolution level by
     *            reading pixels from the original image or by reading back a
     *            finer resolution level already written to the ome-zarr. may be
     *            null (in this case always use the original image).
     * @param afterEachPlane
     *            this is called after each "plane of chunks" is written, giving
     *            the opportunity to clear caches, etc.
     * @param numCellCreatorThreads
     *            The number of threads that will be instantiated to generate
     *            cell data. Must be at least 1. (In addition the cell creator
     *            threads there is one writer thread that saves the generated
     *            data to HDF5.)
     * @param progressWriter
     *            completion ratio and status output will be directed here.
     */
    public static void writeOmeZarrFile(
            final AbstractSequenceDescription< ?, ?, ? > seq,
            final Map< Integer, ExportMipmapInfo> perSetupMipmapInfo,
            final DownsampleBlock.DownsamplingMethod downsamplingMethod,
            final Compression compression,
            final File zarrFile,
            final ExportScalePyramid.LoopbackHeuristic loopbackHeuristic,
            final ExportScalePyramid.AfterEachPlane afterEachPlane,
            final int numCellCreatorThreads,
            ProgressWriter progressWriter ) throws IOException
    {
        if ( progressWriter == null )
            progressWriter = new ProgressWriterNull();
        progressWriter.setProgress( 0 );

        final BasicImgLoader imgLoader = seq.getImgLoader();

        for ( final BasicViewSetup setup : seq.getViewSetupsOrdered() )
        {
            final Object type = imgLoader.getSetupImgLoader( setup.getId() ).getImageType();
            if ( !( type instanceof RealType &&
                    type instanceof NativeType &&
                    N5Utils.dataType( Cast.unchecked( type ) ) != null ) )
                throw new IllegalArgumentException( "Unsupported pixel type: " + type.getClass().getSimpleName() );
        }

        final List< Integer > timepointIds = seq.getTimePoints().getTimePointsOrdered().stream()
                .map( TimePoint::getId )
                .collect( Collectors.toList() );
        final List< Integer > setupIds = seq.getViewSetupsOrdered().stream()
                .map( BasicViewSetup::getId )
                .collect( Collectors.toList() );

        N5OMEZarrWriter zarrWriter = new N5OMEZarrWriter( zarrFile.getAbsolutePath(), new GsonBuilder(), "/" );

        ZarrAxes axes;
        if ( timepointIds.size() > 1 && setupIds.size() > 1 ) {
            axes = ZarrAxes.TCZYX;
        } else if ( timepointIds.size() > 1 ) {
            axes = ZarrAxes.TZYX;
        } else if ( setupIds.size() > 1 ) {
            axes = ZarrAxes.CZYX;
        } else {
            axes = ZarrAxes.ZYX;
        }

        // create group for top directory & add multiscales
        // Currently we write v0.3 ome-zarr
        // Assumes persetupmipmapinfo is the same for every setup
        OmeZarrMultiscales[] multiscales = new OmeZarrMultiscales[1];
        multiscales[0] = new OmeZarrMultiscales(axes, zarrFile.getName().split("\\.")[0], downsamplingMethod.name(),
                new N5Reader.Version(0, 3, 0), perSetupMipmapInfo.get(0).getNumLevels() );

        zarrWriter.createGroup("");
        zarrWriter.setAttribute("", MULTI_SCALE_KEY, multiscales );

        // calculate number of tasks for progressWriter
        int numTasks = 0; // first task is for writing mipmap descriptions etc...
        for ( final int timepointIdSequence : timepointIds )
            for ( final int setupIdSequence : setupIds )
                if ( seq.getViewDescriptions().get( new ViewId( timepointIdSequence, setupIdSequence ) ).isPresent() )
                    numTasks++;
        int numCompletedTasks = 0;

        final ExecutorService executorService = Executors.newFixedThreadPool( numCellCreatorThreads );
        try
        {
            // write image data for all views
            final int numTimepoints = timepointIds.size();
            int timepointIndex = 0;
            for ( final int timepointId : timepointIds )
            {
                progressWriter.out().printf( "proccessing timepoint %d / %d\n", ++timepointIndex, numTimepoints );

                // assemble the viewsetups that are present in this timepoint
                final ArrayList< Integer > setupsTimePoint = new ArrayList<>();
                for ( final int setupId : setupIds )
                    if ( seq.getViewDescriptions().get( new ViewId( timepointId, setupId ) ).isPresent() )
                        setupsTimePoint.add( setupId );

                final int numSetups = setupsTimePoint.size();
                int setupIndex = 0;
                for ( final int setupId : setupsTimePoint )
                {
                    progressWriter.out().printf( "proccessing setup %d / %d\n", ++setupIndex, numSetups );

                    final ExportMipmapInfo mipmapInfo = perSetupMipmapInfo.get( setupId );
                    final double startCompletionRatio = ( double ) numCompletedTasks++ / numTasks;
                    final double endCompletionRatio = ( double ) numCompletedTasks / numTasks;
                    final ProgressWriter subProgressWriter = new SubTaskProgressWriter( progressWriter, startCompletionRatio, endCompletionRatio );
                    writeScalePyramid(
                            zarrWriter, compression, downsamplingMethod,
                            imgLoader, setupId, timepointId, numSetups, numTimepoints, axes,
                            mipmapInfo,
                            executorService, numCellCreatorThreads,
                            loopbackHeuristic, afterEachPlane, subProgressWriter );
                }
            }
        }
        finally
        {
            executorService.shutdown();
        }

        progressWriter.setProgress( 1.0 );
    }

    static < T extends RealType< T > & NativeType< T > > void writeScalePyramid(
            final N5OMEZarrWriter zarrWriter,
            final Compression compression,
            final DownsampleBlock.DownsamplingMethod downsamplingMethod,
            final BasicImgLoader imgLoader,
            final int setupId,
            final int timepointId,
            final int totalNSetups,
            final int totalNTimepoints,
            final ZarrAxes axes,
            final ExportMipmapInfo mipmapInfo,
            final ExecutorService executorService,
            final int numThreads,
            final ExportScalePyramid.LoopbackHeuristic loopbackHeuristic,
            final ExportScalePyramid.AfterEachPlane afterEachPlane,
            ProgressWriter progressWriter ) throws IOException
    {
        final BasicSetupImgLoader< T > setupImgLoader = Cast.unchecked( imgLoader.getSetupImgLoader( setupId ) );
        final RandomAccessibleInterval< T > img = setupImgLoader.getImage( timepointId );
        final T type = setupImgLoader.getImageType();
        final OmeZarrDatasetIO< T > io = new OmeZarrDatasetIO<>( zarrWriter, compression, setupId, timepointId, type,
                totalNSetups, totalNTimepoints, axes );
        ExportScalePyramid.writeScalePyramid(
                img, type, mipmapInfo, downsamplingMethod, io,
                executorService, numThreads,
                loopbackHeuristic, afterEachPlane, progressWriter );
    }

    static class OmeZarrDataset
    {
        final String pathName;
        final DatasetAttributes attributes;

        public OmeZarrDataset(final String pathName, final DatasetAttributes attributes )
        {
            this.pathName = pathName;
            this.attributes = attributes;
        }
    }

    static class OmeZarrDatasetIO< T extends RealType< T > & NativeType< T > > implements ExportScalePyramid.DatasetIO<OmeZarrDataset, T >
    {
        private final N5OMEZarrWriter zarrWriter;
        private final Compression compression;
        private final int setupId;
        private final int timepointId;
        private final DataType dataType;
        private final T type;
        private final Function< ExportScalePyramid.Block< T >, DataBlock< ? > > getDataBlock;
        private final int totalNSetups;
        private final int totalNTimepoints;
        private final ZarrAxes axes;

        public OmeZarrDatasetIO(final N5OMEZarrWriter zarrWriter, final Compression compression, final int setupId,
                                final int timepointId, final T type,
                                final int totalNSetups, final int totalNTimepoints, ZarrAxes axes )
        {
            this.zarrWriter = zarrWriter;
            this.compression = compression;
            this.setupId = setupId;
            this.timepointId = timepointId;
            this.dataType = N5Utils.dataType( type );
            this.type = type;
            this.totalNSetups = totalNSetups;
            this.totalNTimepoints = totalNTimepoints;
            this.axes = axes;

            switch ( dataType )
            {
                case UINT8:
                    getDataBlock = b -> new ByteArrayDataBlock( b.getSize(), b.getGridPosition(), Cast.unchecked( b.getData().getStorageArray() ) );
                    break;
                case UINT16:
                    getDataBlock = b -> new ShortArrayDataBlock( b.getSize(), b.getGridPosition(), Cast.unchecked( b.getData().getStorageArray() ) );
                    break;
                case UINT32:
                    getDataBlock = b -> new IntArrayDataBlock( b.getSize(), b.getGridPosition(), Cast.unchecked( b.getData().getStorageArray() ) );
                    break;
                case UINT64:
                    getDataBlock = b -> new LongArrayDataBlock( b.getSize(), b.getGridPosition(), Cast.unchecked( b.getData().getStorageArray() ) );
                    break;
                case INT8:
                    getDataBlock = b -> new ByteArrayDataBlock( b.getSize(), b.getGridPosition(), Cast.unchecked( b.getData().getStorageArray() ) );
                    break;
                case INT16:
                    getDataBlock = b -> new ShortArrayDataBlock( b.getSize(), b.getGridPosition(), Cast.unchecked( b.getData().getStorageArray() ) );
                    break;
                case INT32:
                    getDataBlock = b -> new IntArrayDataBlock( b.getSize(), b.getGridPosition(), Cast.unchecked( b.getData().getStorageArray() ) );
                    break;
                case INT64:
                    getDataBlock = b -> new LongArrayDataBlock( b.getSize(), b.getGridPosition(), Cast.unchecked( b.getData().getStorageArray() ) );
                    break;
                case FLOAT32:
                    getDataBlock = b -> new FloatArrayDataBlock( b.getSize(), b.getGridPosition(), Cast.unchecked( b.getData().getStorageArray() ) );
                    break;
                case FLOAT64:
                    getDataBlock = b -> new DoubleArrayDataBlock( b.getSize(), b.getGridPosition(), Cast.unchecked( b.getData().getStorageArray() ) );
                    break;
                default:
                    throw new IllegalArgumentException();
            }
        }

        private String getPathName( int level ) {
            if ( totalNTimepoints > 1 && totalNSetups > 1 ) {
                return String.format("s%d/%d/%d", level, timepointId, setupId);
            } else if ( totalNSetups > 1 ) {
                return String.format("s%d/%d", level, setupId);
            } else if ( totalNTimepoints > 1 ) {
                return String.format("s%d/%d", level, timepointId);
            } else {
                return String.format("s%d", level);
            }
        }

        private int[] addSingletonDimensionsToChunks( int[] zyxChunks ) {
            // add any required dimensions for time or channels (we enforce a chunk size of 1 for these axes)
            int[] chunks;

            int nAxes;
            if (totalNSetups > 1 && totalNTimepoints > 1) {
                nAxes = 5;
            } else if (totalNSetups > 1 || totalNTimepoints > 1) {
                nAxes = 4;
            } else {
                nAxes = 3;
            }

            if (nAxes > 3) {
                chunks = new int[nAxes];
                for ( int i=0; i<nAxes; i++) {
                    if ( i < 3 ) {
                        chunks[i] = zyxChunks[i];
                    } else {
                        chunks[i] = 1;
                    }
                }
            } else {
                chunks = zyxChunks;
            }

            return chunks;
        }

        private long[] addSetupAndTimeToShape( long[] zyxShape ) {
            // add any required dimensions for time or channels
            long[] shape;

            if ( totalNSetups > 1 && totalNTimepoints > 1 ) {
                shape = new long[5];
                shape[3] = totalNSetups;
                shape[4] = totalNTimepoints;
            } else if ( totalNSetups > 1 ) {
                shape = new long[4];
                shape[3] = totalNSetups;
            } else if ( totalNTimepoints > 1 ) {
                shape = new long[4];
                shape[3] = totalNTimepoints;
            } else {
                shape = new long[3];
            }

            for (int i = 0; i < 3; i++) {
                shape[i] = zyxShape[i];
            }

            return shape;
        }

        @Override
        public OmeZarrDataset createDataset(final int level, final long[] zyxDimensions, final int[] zyxBlockSize ) throws IOException
        {
            // create dataset directory + metadata
            final String pathName = "s" + level;
            zarrWriter.createDataset( pathName, addSetupAndTimeToShape(zyxDimensions),
                    addSingletonDimensionsToChunks(zyxBlockSize), dataType, compression );

            // TODO - ideally this would go inside zarrWriter.createDataset(), but it's a bit complicated to get it there
            zarrWriter.setAttribute(pathName, ARRAY_DIMENSIONS_KEY, axes );

            // here we have to get the zarr attributes that were written, and re-set the shape/chunks to just zyx, as
            // all the chunking etc operates only in 3D
            final ZarrDatasetAttributes zarrDatasetAttributes = (ZarrDatasetAttributes) zarrWriter.getDatasetAttributes( pathName );
            final DatasetAttributes datasetAttributes = new ZarrDatasetAttributes( zyxDimensions, zyxBlockSize,
                    zarrDatasetAttributes.getDType(), compression,
                    zarrDatasetAttributes.isRowMajor(),
                    zarrDatasetAttributes.getFillValue() );

            // we provide the full path, including any time or channels to actually write blocks
            return new OmeZarrDataset( getPathName(level), datasetAttributes );
        }

        @Override
        public void writeBlock(final OmeZarrDataset dataset, final ExportScalePyramid.Block< T > dataBlock ) throws IOException
        {
            zarrWriter.writeBlock( dataset.pathName, dataset.attributes, getDataBlock.apply( dataBlock ) );
        }

        @Override
        public void flush( final OmeZarrDataset dataset )
        {}

        @Override
        public RandomAccessibleInterval< T > getImage( final int level ) throws IOException
        {
            final String pathName = getPathName( level );
            final DatasetAttributes attributes = zarrWriter.getDatasetAttributes( "s" + level );
            final long[] dimensions = attributes.getDimensions();
            final int[] cellDimensions = attributes.getBlockSize();
            final CellGrid grid = new CellGrid( dimensions, cellDimensions );
            final SimpleCacheArrayLoader< ? > cacheArrayLoader = N5ImageLoader.createCacheArrayLoader(zarrWriter, pathName );
            return new ReadOnlyCachedCellImgFactory().createWithCacheLoader(
                    dimensions, type,
                    key -> {
                        final int n = grid.numDimensions();
                        final long[] cellMin = new long[ n ];
                        final int[] cellDims = new int[ n ];
                        final long[] cellGridPosition = new long[ n ];
                        grid.getCellDimensions( key, cellMin, cellDims );
                        grid.getCellGridPositionFlat( key, cellGridPosition );
                        return new Cell<>( cellDims, cellMin, cacheArrayLoader.loadArray( cellGridPosition ) );
                    },
                    options().cellDimensions( cellDimensions ) );
        }
    }
}
