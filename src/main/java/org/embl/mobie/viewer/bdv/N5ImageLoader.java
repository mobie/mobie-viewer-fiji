/*
 * #%L
 * BigDataViewer core classes with minimal dependencies
 * %%
 * Copyright (C) 2012 - 2016 Tobias Pietzsch, Stephan Saalfeld, Stephan Preibisch,
 * Jean-Yves Tinevez, HongKee Moon, Johannes Schindelin, Curtis Rueden, John Bogovic
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
package org.embl.mobie.viewer.bdv;

import bdv.AbstractViewerSetupImgLoader;
import bdv.ViewerImgLoader;
import bdv.cache.CacheControl;
import bdv.img.cache.SimpleCacheArrayLoader;
import bdv.img.cache.VolatileGlobalCellCache;
import bdv.img.catmaid.XmlIoCatmaidImageLoader;
import bdv.img.hdf5.Hdf5ImageLoader;
import bdv.img.hdf5.Partition;
import bdv.img.openconnectome.XmlIoOpenConnectomeImageLoader;
import bdv.img.remote.XmlIoRemoteImageLoader;
import bdv.spimdata.SequenceDescriptionMinimal;
import bdv.util.ConstantRandomAccessible;
import bdv.util.MipmapTransforms;
import mpicbg.spim.data.XmlHelpers;
import mpicbg.spim.data.generic.sequence.*;
import mpicbg.spim.data.registration.ViewRegistration;
import mpicbg.spim.data.registration.ViewRegistrations;
import mpicbg.spim.data.sequence.*;
import net.imglib2.*;
import net.imglib2.cache.queue.BlockingFetchQueues;
import net.imglib2.cache.queue.FetcherThreads;
import net.imglib2.cache.volatiles.CacheHints;
import net.imglib2.cache.volatiles.LoadingStrategy;
import net.imglib2.img.basictypeaccess.volatiles.array.*;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.img.cell.CellImg;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.integer.*;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.type.volatiles.*;
import net.imglib2.util.Cast;
import net.imglib2.view.Views;
import org.embl.mobie.io.ome.zarr.util.OmeZarrMultiscales;
import org.janelia.saalfeldlab.n5.*;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Function;

import static bdv.img.n5.BdvN5Format.*;
import static mpicbg.spim.data.XmlHelpers.loadPath;
import static mpicbg.spim.data.XmlKeys.BASEPATH_TAG;

public class N5ImageLoader implements ViewerImgLoader, MultiResolutionImgLoader
{
    protected final N5Reader n5;

    // TODO: it would be good if this would not be needed
    //       find available setups from the n5
    protected SequenceDescription seq;
    private int sequenceTimepoints = 0;


    /**
     * Maps setup id to {@link SetupImgLoader}.
     */
    private final Map< Integer, SetupImgLoader > setupImgLoaders = new HashMap<>();

    private volatile boolean isOpen = false;
    private FetcherThreads fetchers;
    private VolatileGlobalCellCache cache;
    private BlockingFetchQueues<Callable<?>> queue;
    private final Map<Integer, OmeZarrMultiscales> setupToMultiscale = new HashMap<>();
    private final Map<Integer, DatasetAttributes> setupToAttributes = new HashMap<>();
    private final Map<Integer, String> setupToPathname = new HashMap<>();
    private final Map<Integer, Integer> setupToChannel = new HashMap<>();
    protected ViewRegistrations viewRegistrations;


    public N5ImageLoader( N5Reader n5Reader, SequenceDescription sequenceDescription )
    {
        this.n5 = n5Reader;
        this.seq = sequenceDescription;
    }

    public N5ImageLoader( N5Reader n5Reader, SequenceDescription sequenceDescription, BlockingFetchQueues<Callable<?>> queue )
    {
        this.n5 = n5Reader;
        this.seq = sequenceDescription;
        this.queue = queue;
    }

    public N5ImageLoader( N5Reader n5Reader, BlockingFetchQueues<Callable<?>> queue )
    {
        this.n5 = n5Reader;
        this.queue = queue;
        fetchSequenceDescriptionAndViewRegistrations();
    }

    public AbstractSequenceDescription<?, ?, ?> getSequenceDescription() {
        //open();
        seq.setImgLoader(Cast.unchecked(this));
        return seq;
    }

    public ViewRegistrations getViewRegistrations() {
        return viewRegistrations;
    }

    private DatasetAttributes getDatasetAttributes(String pathName) throws IOException {
        return n5.getDatasetAttributes(pathName);
    }
    private OmeZarrMultiscales getMultiscale(String pathName) throws IOException {
        final String key = "multiscales";
//        if (pathName.isEmpty()) {
//            String path = pathName.split( "." )[ 0 ];
//            pathName = path + ".n5";
//        }

        OmeZarrMultiscales[] multiscales = n5.getAttribute(pathName, key, OmeZarrMultiscales[].class);
        if (multiscales == null) {
            return null;
//            throw new UnsupportedOperationException("Could not find multiscales");
        }
        return multiscales[0];
    }

    private void initSetups() throws IOException {
        int setupId = -1;


        OmeZarrMultiscales multiscale = getMultiscale(""); // returns multiscales[ 0 ]
        DatasetAttributes attributes = null;

        if (multiscale != null) {
            attributes = getDatasetAttributes( multiscale.datasets[ 0 ].path );
        }

        long nC = 1;

        for (int c = 0; c < nC; c++) {
            // each channel is one setup
            setupId++;
            setupToChannel.put(setupId, c);

            // all channels have the same multiscale and attributes
            setupToMultiscale.put(setupId, multiscale);
            setupToAttributes.put(setupId, attributes);
            setupToPathname.put(setupId, "");
        }
        try {
            List<String> labels = n5.getAttribute( "labels", "labels", List.class );
            if ( labels != null ) {
                for ( String label : labels ) {
                    setupId++;
                    setupToChannel.put( setupId, 0 ); // TODO: https://github.com/ome/ngff/issues/19
                    String pathName = "labels/" + label;
                    multiscale = getMultiscale( pathName );
                    attributes = getDatasetAttributes( pathName + "/" + multiscale.datasets[ 0 ].path );

                    setupToMultiscale.put( setupId, multiscale );
                    setupToAttributes.put( setupId, attributes );
                    setupToPathname.put( setupId, pathName );
                }
            }
        } catch ( NoSuchFileException e ) {
            System.out.println("No labels");
        }
    }

    private void fetchSequenceDescriptionAndViewRegistrations() {
        try {
            String xmlFilename = "/home/katerina/Documents/embl/mnt2/kreshuk/pape/Work/mobie/arabidopsis-root-lm-datasets/data/arabidopsis-root/images/local/lm-cells.xml";
            initSetups();
            ArrayList<ViewSetup> viewSetups = new ArrayList<>();
            ArrayList<ViewRegistration> viewRegistrationList = new ArrayList<>();

            int numSetups = setupToMultiscale.size();
            for (int setupId = 0; setupId < numSetups; setupId++) {
                ViewSetup viewSetup = createViewSetup(setupId);
                int setupTimepoints = 1;
                if (setupToAttributes.containsKey( setupId ) && setupToAttributes.get( setupId ) != null) {

                    if ( setupToAttributes.get( setupId ).getNumDimensions() > 4 ) {
                        setupTimepoints = (int) setupToAttributes.get( setupId ).getDimensions()[ 4 ];
                    }
                }

                sequenceTimepoints = Math.max(setupTimepoints, sequenceTimepoints);
                viewSetups.add(viewSetup);
                viewRegistrationList.addAll(createViewRegistrations(setupId, setupTimepoints));
            }

            viewRegistrations = new ViewRegistrations(viewRegistrationList);
            final SAXBuilder sax = new SAXBuilder();
            Document doc;
                doc = sax.build( xmlFilename );
            final Element root = doc.getRootElement();
            final File basePath = loadBasePath( root, new File( xmlFilename ) );

            final TimePoints timepoints = createTimepointsFromXml( root.getChild("SequenceDescription"  ) );
            final Map< Integer, ViewSetup > setups = createViewSetupsFromXml( root.getChild( "SequenceDescription" ) );
            final MissingViews missingViews = null;
            this.seq = new SequenceDescription( timepoints, setups, null, missingViews );
            final ImgLoader imgLoader = createImgLoaderFromXml( root.getChild("SequenceDescription"  ), basePath, this.seq );
            this.seq.setImgLoader( imgLoader );
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch ( JDOMException e ) {
            e.printStackTrace();
        }
    }

    private static ImgLoader createImgLoaderFromXml( final Element sequenceDescriptionElem, final File basePath, final SequenceDescription sequenceDescription  )
    {
        final Element elem = sequenceDescriptionElem.getChild( "ImageLoader" );
        final String classn = elem.getAttributeValue( "class" );
        if (classn != null) {
            if ( classn.equals( "viewer.hdf5.Hdf5ImageLoader" ) || classn.equals( "bdv.img.hdf5.Hdf5ImageLoader" ) ) {
                final String path = loadPath( elem, "hdf5", basePath ).toString();
                final ArrayList<Partition> partitions = new ArrayList<>();
                for ( final Element p : elem.getChildren( "partition" ) )
                    partitions.add( partitionFromXml( p, basePath ) );
                return new Hdf5ImageLoader( new File( path ), partitions, sequenceDescription );
            }
        }
        else {
            try {
                return new N5FSImageLoader(basePath, sequenceDescription );
            } catch ( IOException e ) {
                throw new RuntimeException( "Error while ImageLoader class" );
            }
        }
        throw new RuntimeException( "unknown ImageLoader class" );
    }

    private static Partition partitionFromXml( final Element elem, final File basePath )
    {
        String path;
        try
        {
            path = XmlHelpers.loadPath( elem, "path", basePath ).toString();
        }
        catch ( final Exception e )
        {
            throw new RuntimeException( e );
        }

        final int timepointOffset = Integer.parseInt( elem.getChildText( "timepointOffset" ) );
        final int timepointStart = Integer.parseInt( elem.getChildText( "timepointStart" ) );
        final int timepointLength = Integer.parseInt( elem.getChildText( "timepointLength" ) );
        final int setupOffset = Integer.parseInt( elem.getChildText( "setupOffset" ) );
        final int setupStart = Integer.parseInt( elem.getChildText( "setupStart" ) );
        final int setupLength = Integer.parseInt( elem.getChildText( "setupLength" ) );

        final HashMap< Integer, Integer > timepointIdSequenceToPartition = new HashMap<>();
        for ( int tPartition = timepointStart; tPartition < timepointStart + timepointLength; ++tPartition )
        {
            final int tSequence = tPartition + timepointOffset;
            timepointIdSequenceToPartition.put( tSequence, tPartition );
        }

        final HashMap< Integer, Integer > setupIdSequenceToPartition = new HashMap<>();
        for ( int sPartition = setupStart; sPartition < setupStart + setupLength; ++sPartition )
        {
            final int sSequence = sPartition + setupOffset;
            setupIdSequenceToPartition.put( sSequence, sPartition );
        }

        return new Partition( path, timepointIdSequenceToPartition, setupIdSequenceToPartition );
    }

    private static Map< Integer, ViewSetup > createViewSetupsFromXml( final Element sequenceDescription )
    {
        final HashMap< Integer, ViewSetup > setups = new HashMap<>();
        final HashMap< Integer, Angle > angles = new HashMap<>();
        final HashMap< Integer, Channel > channels = new HashMap<>();
        final HashMap< Integer, Illumination > illuminations = new HashMap<>();
        Element viewSetups = sequenceDescription.getChild( "ViewSetups" );

        for ( final Element elem : viewSetups.getChildren( "ViewSetup" ) )
        {
            final int id = XmlHelpers.getInt( elem, "id" );
            int angleId = 0;
            Angle angle = new Angle( angleId );
            Channel channel = new Channel( angleId );
            Illumination illumination = new Illumination( angleId );
            try {
                 angleId = XmlHelpers.getInt( elem, "angle" );
//            if (angleId != null) {
                angle = angles.get( angleId );
                if ( angle == null ) {
                    angle = new Angle( angleId );
                    angles.put( angleId, angle );
                }
            } catch ( NumberFormatException e ) {
                System.out.println("No ange specified");

            }
            try {
            final int illuminationId = XmlHelpers.getInt( elem, "illumination" );
            illumination = illuminations.get( illuminationId );
            if ( illumination == null )
            {
                illumination = new Illumination( illuminationId );
                illuminations.put( illuminationId, illumination );
            }
            } catch ( NumberFormatException e ) {
                System.out.println("No ange specified");

            }
            try {
            final int channelId = XmlHelpers.getInt( elem, "channel" );
            channel = channels.get( channelId );
            if ( channel == null )
            {
                channel = new Channel( channelId );
                channels.put( channelId, channel );
            }
            } catch ( NumberFormatException e ) {
                System.out.println("No ange specified");

            }
            try {
                final long w = XmlHelpers.getInt( elem, "width" );
                final long h = XmlHelpers.getInt( elem, "height" );
                final long d = XmlHelpers.getInt( elem, "depth" );
                final Dimensions size = new FinalDimensions( w, h, d );
            } catch ( NumberFormatException e ) {
                System.out.println("No ange specified");

            }
                final String sizeString = elem.getChildText( "size" );
                final String[] values = sizeString.split( " " );
//                final long d = XmlHelpers.getInt( elem, "depth" );
                final Dimensions size = new FinalDimensions( Integer.parseInt(  values[0]), Integer.parseInt(  values[1]), Integer.parseInt(  values[2]) );
try {
            final double pw = XmlHelpers.getDouble( elem, "pixelWidth" );
            final double ph = XmlHelpers.getDouble( elem, "pixelHeight" );
            final double pd = XmlHelpers.getDouble( elem, "pixelDepth" );
            final VoxelDimensions voxelSize = new FinalVoxelDimensions( "px", pw, ph, pd );
} catch ( Exception e ) {
    System.out.println("No ange specified");

}
            final Element voxelsizeString = elem.getChild( "voxelSize" );
            final String unit = elem.getChildText( "unit" );
            final String[] voxelValues = elem.getChildText("size").split( " " );
//                final long d = XmlHelpers.getInt( elem, "depth" );
            final VoxelDimensions voxelSize = new FinalVoxelDimensions( "px", Integer.parseInt(  voxelValues[0]), Integer.parseInt(  voxelValues[1]), Integer.parseInt(  voxelValues[2]) );


            final ViewSetup setup = new ViewSetup( id, null, size, voxelSize, channel, angle, illumination );
            setups.put( id, setup );
        }
        return setups;
    }


    private static File loadBasePath( final Element root, final File xmlFile )
    {
        File xmlFileParentDirectory = xmlFile.getParentFile();
        if ( xmlFileParentDirectory == null )
            xmlFileParentDirectory = new File( "." );
        return XmlHelpers.loadPath( root, BASEPATH_TAG, ".", xmlFileParentDirectory );
    }
    private static TimePoints createTimepointsFromXml( final Element sequenceDescription )
    {
        final Element timepoints = sequenceDescription.getChild( "Timepoints" );
        final String type = timepoints.getAttributeValue( "type" );
        if ( type.equals( "range" ) )
        {
            final int first = Integer.parseInt( timepoints.getChildText( "first" ) );
            final int last = Integer.parseInt( timepoints.getChildText( "last" ) );
            final ArrayList< TimePoint > tps = new ArrayList<>();
            for ( int i = first, t = 0; i <= last; ++i, ++t )
                tps.add( new TimePoint( t ) );
            return new TimePoints( tps );
        }
        else
        {
            throw new RuntimeException( "unknown <Timepoints> type: " + type );
        }
    }

    @NotNull
    private ArrayList<ViewRegistration> createViewRegistrations(int setupId, int setupTimepoints) {

        AffineTransform3D transform = new AffineTransform3D();

        ArrayList<ViewRegistration> viewRegistrations = new ArrayList<>();
        for (int t = 0; t < setupTimepoints; t++)
            viewRegistrations.add(new ViewRegistration(t, setupId, transform));

        return viewRegistrations;
    }

    private ViewSetup createViewSetup(int setupId) {
        final DatasetAttributes attributes = setupToAttributes.get(setupId);
        if (attributes != null) {
            FinalDimensions dimensions = new FinalDimensions( attributes.getDimensions() );
            OmeZarrMultiscales multiscale = setupToMultiscale.get( setupId );
            VoxelDimensions voxelDimensions = new DefaultVoxelDimensions( 3 );
            Tile tile = new Tile( 0 );

            Channel channel;
            if ( setupToPathname.get( setupId ).contains( "labels" ) )
                channel = new Channel( setupToChannel.get( setupId ), "labels" );
            else
                channel = new Channel( setupToChannel.get( setupId ) );

            Angle angle = new Angle( 0 );
            Illumination illumination = new Illumination( 0 );
            String name = readName( multiscale, setupId );
            //if ( setupToPathname.get( setupId ).contains( "labels" ))
            //	viewSetup.setAttribute( new ImageType( ImageType.Type.IntensityImage ) );
            return new ViewSetup( setupId, name, dimensions, voxelDimensions, tile, channel, angle, illumination );
        } else {
            return null;
        }
    }

    @NotNull
    private ArrayList<TimePoint> createTimePoints(int sequenceTimepoints) {
        ArrayList<TimePoint> timePoints = new ArrayList<>();
        for (int t = 0; t < sequenceTimepoints; t++) {
            timePoints.add(new TimePoint(t));
        }
        return timePoints;
    }

    private void open()
    {
        if ( !isOpen )
        {
            synchronized ( this )
            {
                if ( isOpen )
                    return;

                try
                {
                    int maxNumLevels = 0;
                    final List< ? extends BasicViewSetup > setups = seq.getViewSetupsOrdered();
                    for ( final BasicViewSetup setup : setups )
                    {
                        final int setupId = setup.getId();
                        final SetupImgLoader setupImgLoader = createSetupImgLoader( setupId );
                        setupImgLoaders.put( setupId, setupImgLoader );
                        maxNumLevels = Math.max( maxNumLevels, setupImgLoader.numMipmapLevels() );
                    }
                    if (queue == null) {
                        final int numFetcherThreads = Math.max(1, Runtime.getRuntime().availableProcessors());
                        queue = new BlockingFetchQueues<>(maxNumLevels, numFetcherThreads);
                        fetchers = new FetcherThreads(queue, numFetcherThreads);
                    }
                    cache = new VolatileGlobalCellCache( queue );
                }
                catch ( IOException e )
                {
                    throw new RuntimeException( e );
                }

                isOpen = true;
            }
        }
    }

    private String readName(OmeZarrMultiscales multiscale, int setupId) {
        if (multiscale.name != null)
            return multiscale.name;
        else
            return "image " + setupId;
    }

    /**
     * Clear the cache. Images that were obtained from
     * this loader before {@link #close()} will stop working. Requesting images
     * after {@link #close()} will cause the n5 to be reopened (with a
     * new cache).
     */
    public void close()
    {
        if ( isOpen )
        {
            synchronized ( this )
            {
                if ( !isOpen )
                    return;
                if (fetchers != null)
                    fetchers.shutdown();
                cache.clearCache();
                isOpen = false;
            }
        }
    }

    @Override
    public SetupImgLoader getSetupImgLoader( final int setupId )
    {
        open();
        return setupImgLoaders.get( setupId );
    }

    private < T extends NativeType< T >, V extends Volatile< T > & NativeType< V > > SetupImgLoader< T, V > createSetupImgLoader( final int setupId ) throws IOException
    {
        final String pathName = getPathName( setupId );
        final DataType dataType = n5.getAttribute( pathName, DATA_TYPE_KEY, DataType.class );
        switch ( dataType )
        {
            case UINT8:
                return Cast.unchecked( new SetupImgLoader<>( setupId, new UnsignedByteType(), new VolatileUnsignedByteType() ) );
            case UINT16:
                return Cast.unchecked( new SetupImgLoader<>( setupId, new UnsignedShortType(), new VolatileUnsignedShortType() ) );
            case UINT32:
                return Cast.unchecked( new SetupImgLoader<>( setupId, new UnsignedIntType(), new VolatileUnsignedIntType() ) );
            case UINT64:
                return Cast.unchecked( new SetupImgLoader<>( setupId, new UnsignedLongType(), new VolatileUnsignedLongType() ) );
            case INT8:
                return Cast.unchecked( new SetupImgLoader<>( setupId, new ByteType(), new VolatileByteType() ) );
            case INT16:
                return Cast.unchecked( new SetupImgLoader<>( setupId, new ShortType(), new VolatileShortType() ) );
            case INT32:
                return Cast.unchecked( new SetupImgLoader<>( setupId, new IntType(), new VolatileIntType() ) );
            case INT64:
                return Cast.unchecked( new SetupImgLoader<>( setupId, new LongType(), new VolatileLongType() ) );
            case FLOAT32:
                return Cast.unchecked( new SetupImgLoader<>( setupId, new FloatType(), new VolatileFloatType() ) );
            case FLOAT64:
                return Cast.unchecked( new SetupImgLoader<>( setupId, new DoubleType(), new VolatileDoubleType() ) );
        }
        return null;
    }

    @Override
    public CacheControl getCacheControl()
    {
        open();
        return cache;
    }

    public class SetupImgLoader< T extends NativeType< T >, V extends Volatile< T > & NativeType< V > >
            extends AbstractViewerSetupImgLoader< T, V >
            implements MultiResolutionSetupImgLoader< T >
    {
        private final int setupId;

        private final double[][] mipmapResolutions;

        private final AffineTransform3D[] mipmapTransforms;

        public SetupImgLoader( final int setupId, final T type, final V volatileType ) throws IOException
        {
            super( type, volatileType );
            this.setupId = setupId;
            final String pathName = getPathName( setupId );
            mipmapResolutions = n5.getAttribute( pathName, DOWNSAMPLING_FACTORS_KEY, double[][].class );
            mipmapTransforms = new AffineTransform3D[ mipmapResolutions.length ];
            for ( int level = 0; level < mipmapResolutions.length; level++ )
                mipmapTransforms[ level ] = MipmapTransforms.getMipmapTransformDefault( mipmapResolutions[ level ] );
        }

        @Override
        public RandomAccessibleInterval< V > getVolatileImage( final int timepointId, final int level, final ImgLoaderHint... hints )
        {
            return prepareCachedImage( timepointId, level, LoadingStrategy.BUDGETED, volatileType );
        }

        @Override
        public RandomAccessibleInterval< T > getImage( final int timepointId, final int level, final ImgLoaderHint... hints )
        {
            return prepareCachedImage( timepointId, level, LoadingStrategy.BLOCKING, type );
        }

        @Override
        public Dimensions getImageSize( final int timepointId, final int level )
        {
            try
            {
                final String pathName = getPathName( setupId, timepointId, level );
                final DatasetAttributes attributes = n5.getDatasetAttributes( pathName );
                FinalDimensions dimensions = new FinalDimensions( attributes.getDimensions() );
                return dimensions;
            }
            catch( Exception e )
            {
                return null;
            }
        }

        @Override
        public double[][] getMipmapResolutions()
        {
            return mipmapResolutions;
        }

        @Override
        public AffineTransform3D[] getMipmapTransforms()
        {
            return mipmapTransforms;
        }

        @Override
        public int numMipmapLevels()
        {
            return mipmapResolutions.length;
        }

        @Override
        public VoxelDimensions getVoxelSize( final int timepointId )
        {
            return null;
        }

        /**
         * Create a {@link CellImg} backed by the cache.
         */
        private < T extends NativeType< T > > RandomAccessibleInterval< T > prepareCachedImage( final int timepointId, final int level, final LoadingStrategy loadingStrategy, final T type )
        {
            try
            {
                final String pathName = getPathName( setupId, timepointId, level );
                final DatasetAttributes attributes = n5.getDatasetAttributes( pathName );
                final long[] dimensions = attributes.getDimensions();
                final int[] cellDimensions = attributes.getBlockSize();
                final CellGrid grid = new CellGrid( dimensions, cellDimensions );

                final int priority = numMipmapLevels() - 1 - level;
                final CacheHints cacheHints = new CacheHints( loadingStrategy, priority, false );

                final SimpleCacheArrayLoader< ? > loader = createCacheArrayLoader( n5, pathName );
                return cache.createImg( grid, timepointId, setupId, level, cacheHints, loader, type );
            }
            catch ( IOException e )
            {
                System.err.println( String.format(
                        "image data for timepoint %d setup %d level %d could not be found.",
                        timepointId, setupId, level ) );
                return Views.interval(
                        new ConstantRandomAccessible<>( type.createVariable(), 3 ),
                        new FinalInterval( 1, 1, 1 ) );
            }
        }
    }

    private static class N5CacheArrayLoader< A > implements SimpleCacheArrayLoader< A >
    {
        private final N5Reader n5;
        private final String pathName;
        private final DatasetAttributes attributes;
        private final Function< DataBlock< ? >, A > createArray;

        N5CacheArrayLoader( final N5Reader n5, final String pathName, final DatasetAttributes attributes, final Function< DataBlock< ? >, A > createArray )
        {
            this.n5 = n5;
            this.pathName = pathName;
            this.attributes = attributes;
            this.createArray = createArray;
        }

        @Override
        public A loadArray( final long[] gridPosition ) throws IOException
        {
            DataBlock< ? > block = null;

            try {
                block = n5.readBlock( pathName, attributes, gridPosition );
            }
            catch ( Exception e )
            {
                System.err.println( "Error loading " + pathName + " at block " + Arrays.toString( gridPosition ) + ": " + e );
            }

//			if ( block != null )
//				System.out.println( pathName + " " + Arrays.toString( gridPosition ) + " " + block.getNumElements() );
//			else
//				System.out.println( pathName + " " + Arrays.toString( gridPosition ) + " NaN" );


            if ( block == null )
            {
                final int[] blockSize = attributes.getBlockSize();
                final int n = blockSize[ 0 ] * blockSize[ 1 ] * blockSize[ 2 ];
                switch ( attributes.getDataType() )
                {
                    case UINT8:
                    case INT8:
                        return createArray.apply( new ByteArrayDataBlock( blockSize, gridPosition, new byte[ n ] ) );
                    case UINT16:
                    case INT16:
                        return createArray.apply( new ShortArrayDataBlock( blockSize, gridPosition, new short[ n ] ) );
                    case UINT32:
                    case INT32:
                        return createArray.apply( new IntArrayDataBlock( blockSize, gridPosition, new int[ n ] ) );
                    case UINT64:
                    case INT64:
                        return createArray.apply( new LongArrayDataBlock( blockSize, gridPosition, new long[ n ] ) );
                    case FLOAT32:
                        return createArray.apply( new FloatArrayDataBlock( blockSize, gridPosition, new float[ n ] ) );
                    case FLOAT64:
                        return createArray.apply( new DoubleArrayDataBlock( blockSize, gridPosition, new double[ n ] ) );
                    default:
                        throw new IllegalArgumentException();
                }
            }
            else
            {
                return createArray.apply( block );
            }
        }
    }

    public static SimpleCacheArrayLoader< ? > createCacheArrayLoader( final N5Reader n5, final String pathName ) throws IOException
    {
        final DatasetAttributes attributes = n5.getDatasetAttributes( pathName );
        switch ( attributes.getDataType() )
        {
            case UINT8:
            case INT8:
                return new N5CacheArrayLoader<>( n5, pathName, attributes,
                        dataBlock -> new VolatileByteArray( Cast.unchecked( dataBlock.getData() ), true ) );
            case UINT16:
            case INT16:
                return new N5CacheArrayLoader<>( n5, pathName, attributes,
                        dataBlock -> new VolatileShortArray( Cast.unchecked( dataBlock.getData() ), true ) );
            case UINT32:
            case INT32:
                return new N5CacheArrayLoader<>( n5, pathName, attributes,
                        dataBlock -> new VolatileIntArray( Cast.unchecked( dataBlock.getData() ), true ) );
            case UINT64:
            case INT64:
                return new N5CacheArrayLoader<>( n5, pathName, attributes,
                        dataBlock -> new VolatileLongArray( Cast.unchecked( dataBlock.getData() ), true ) );
            case FLOAT32:
                return new N5CacheArrayLoader<>( n5, pathName, attributes,
                        dataBlock -> new VolatileFloatArray( Cast.unchecked( dataBlock.getData() ), true ) );
            case FLOAT64:
                return new N5CacheArrayLoader<>( n5, pathName, attributes,
                        dataBlock -> new VolatileDoubleArray( Cast.unchecked( dataBlock.getData() ), true ) );
            default:
                throw new IllegalArgumentException();
        }
    }
}
