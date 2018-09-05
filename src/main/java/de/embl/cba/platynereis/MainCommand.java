package de.embl.cba.platynereis;

import bdv.img.imaris.Imaris;
import bdv.spimdata.SpimDataMinimal;
import bdv.util.*;
import bdv.viewer.Interpolation;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.registration.ViewTransformAffine;
import mpicbg.spim.data.sequence.FinalVoxelDimensions;
import net.imagej.ImageJ;
import net.imglib2.RealPoint;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.Scale;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.util.Util;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.command.Interactive;
import org.scijava.log.LogService;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;
import org.scijava.widget.Button;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

@Plugin(type = Command.class, menuPath = "Plugins>Registration>EMBL>Platynereis", initializer = "init")
public class MainCommand extends DynamicCommand implements Interactive
{
    private static final String BDV_XML_SUFFIX = ".xml";
    private static final String IMARIS_SUFFIX = ".ims";
    private static final double PROSPR_SCALING_IN_MICROMETER = 0.5;
    private static final String EM_RAW_FILE_DEFAULT_ID = "em-raw-10nm-10nm-25nm"; //"em-raw-100nm"; //"em-raw-10nm-10nm-25nm"; //"em-raw-100nm"; //
    private static final String EM_RAW_FILE_ID = "em-raw-"; //"em-raw-100nm"; //"em-raw-10nm-10nm-25nm"; //"em-raw-100nm"; //
    private static final String EM_SEGMENTED_FILE_ID = "em-segmented";
    private static final String SELECTION_UI = "Data sources";
    private static final String POSITION_UI = "Move to position";
    private static final Color DEFAULT_GENE_COLOR = new Color( 255, 0, 255, 255 );
    private static final Color DEFAULT_EM_RAW_COLOR = new Color( 255, 255, 255, 255 );
    private static final Color DEFAULT_EM_SEGMENTATION_COLOR = new Color( 255, 0, 0, 255 );
    public static final double ZOOM_REGION_SIZE = 50.0;

    @Parameter
    public LogService logService;

    Bdv bdv;

    Map< String, PlatynereisDataSource > dataSourcesMap;

    String emRawDataID;
    AffineTransform3D emRawDataTransform;
    DataSourcesUI legend;


    public void init()
    {
        //File directory = new File( IJ.getDirectory( "Select Platynereis directory" ) );

        //File directory = new File( "/Users/tischer/Documents/detlev-arendt-clem-registration--data" );

        //File directory = new File( "/Volumes/cba/tischer/projects/detlev-arendt-clem-registration--data/data/em-raw/bdv" );

        //File directory = new File( "/Volumes/arendt/EM_6dpf_segmentation/bigdataviewer" );

        String dir = IJ.getDirectory( "Please choose Platynereis directory" );

        File directory = new File( dir );

        initDataSources( directory );

        initBdvWithEmRawData();

        addBehaviors();

        createLegend();

        new MainUI( bdv, this );

    }

    private void createLegend()
    {
        legend = new DataSourcesUI( this );
        legend.addSource( dataSourcesMap.get( emRawDataID ) );
    }

    private void addOverlay()
    {
        //bdv.getViewer().addTransformListener( lo );
        //bdv.getViewer().getDisplay().addOverlayRenderer( lo );
        //bdv.getViewerFrame().setVisible( true );
        //bdv.getViewer().requestRepaint();
        //https://github.com/PreibischLab/BigStitcher/blob/master/src/main/java/net/preibisch/stitcher/gui/overlay/LinkOverlay.java
    }

    private void addBehaviors()
    {
        Behaviours behaviours = new Behaviours( new InputTriggerConfig() );
        behaviours.install( bdv.getBdvHandle().getTriggerbindings(), "my-new-behaviours" );

        behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {
            printCoordinates();
        }, "print global pos", "P" );

    }

    private void printCoordinates()
    {

        final RealPoint posInBdvInMicrometer = new RealPoint( 3 );
        bdv.getBdvHandle().getViewerPanel().getGlobalMouseCoordinates( posInBdvInMicrometer );

        final RealPoint posInverse = new RealPoint( 3 );
        ProSPrRegistration.getTransformationFromEmToProsprInMicrometerUnits().inverse().apply( posInBdvInMicrometer, posInverse );

        IJ.log( "coordinates in raw em data set [micrometer] : " + Util.printCoordinates( new RealPoint( posInverse ) ) );

    }

    public void run()
    {

    }

    private void print( String text )
    {
        logService.info( text );
        IJ.log( text );
    }

    private void printLegend()
    {
        print( "Currently shown genes: " );

        for ( String gene : dataSourcesMap.keySet() )
        {
            if ( dataSourcesMap.get( gene ).isActive )
            {
                String color = dataSourcesMap.get( gene ).color.toString();
                print( gene + ", color " + color );
            }
        }
    }

    public void addDataSourceToBdv( String name )
    {
        PlatynereisDataSource dataSource = dataSourcesMap.get( name );

        if ( dataSource.bdvSource == null )
        {
            switch ( BDV_XML_SUFFIX )
            {
                case ".tif":
                    addSourceFromTiffFile( name );
                    break;
                case ".xml":
                    loadAndShowSourceInBdv( name );
                    break;
                default:
                    logService.error( "Unsupported format: " + BDV_XML_SUFFIX );
            }
        }

        dataSource.bdvSource.setActive( true );
        dataSource.isActive = true;
        dataSource.bdvSource.setColor( asArgbType( dataSource.color ) );
        dataSource.name = name;

        legend.addSource( dataSource );
    }

    public void hideDataSource( String dataSourceName )
    {
        if ( dataSourcesMap.get( dataSourceName ).bdvSource != null )
        {
            dataSourcesMap.get( dataSourceName ).bdvSource.setActive( false );
            dataSourcesMap.get( dataSourceName ).isActive = false;
        }
    }


    public void setDataSourceColor( String sourceName, Color color )
    {
        dataSourcesMap.get( sourceName ).bdvSource.setColor( asArgbType( color ) );
        dataSourcesMap.get( sourceName ).color = color;
    }


    public void setBrightness( String sourceName )
    {
        GenericDialog gd = new GenericDialog( "LUT max value" );
        gd.addNumericField( "LUT max value: ", dataSourcesMap.get( sourceName ).maxLutValue, 0 );
        gd.showDialog();
        if ( gd.wasCanceled() ) return;

        int max = ( int ) gd.getNextNumber();

        dataSourcesMap.get( sourceName ).bdvSource.setDisplayRange( 0.0, max );
        dataSourcesMap.get( sourceName ).maxLutValue = max;
    }


    private void loadAndShowSourceInBdv( String dataSourceName )
    {

        PlatynereisDataSource source = dataSourcesMap.get( dataSourceName );

        if ( source.isSpimDataMinimal )
        {
            setName( dataSourceName, source );

            source.bdvSource = BdvFunctions.show( source.spimDataMinimal, BdvOptions.options().addTo( bdv ) ).get( 0 );
            source.bdvSource.setColor( asArgbType( source.color ) );
            source.bdvSource.setDisplayRange( 0.0, source.maxLutValue );

            bdv = source.bdvSource.getBdvHandle();
        }
        else
        {
            if ( source.spimData == null )
            {
                // spimData can be null for the fluorescence data, which is only initialized on demand
                source.spimData = openSpimData( source.file );
            }

            setName( dataSourceName, source );

            source.bdvSource = BdvFunctions.show( source.spimData, BdvOptions.options().addTo( bdv ) ).get( 0 );

            source.bdvSource.setColor( asArgbType( source.color ) );
            source.bdvSource.setDisplayRange( 0.0, source.maxLutValue );

            bdv = source.bdvSource.getBdvHandle();
        }

    }

    private void setName( String name, PlatynereisDataSource source )
    {
        source.spimData.getSequenceDescription().getViewSetupsOrdered().get( 0 ).getChannel().setName( name );
    }

    private void addSourceFromTiffFile( String gene )
    {
        ImagePlus imp = IJ.openImage( dataSourcesMap.get( gene ).file.toString() );
        Img img = ImageJFunctions.wrap( imp );

        AffineTransform3D prosprScaling = new AffineTransform3D();
        prosprScaling.scale( PROSPR_SCALING_IN_MICROMETER );

        final BdvSource source = BdvFunctions.show( img, gene, Bdv.options().addTo( bdv ).sourceTransform( prosprScaling ) );

        source.setColor( asArgbType( DEFAULT_GENE_COLOR ) );

        dataSourcesMap.get( gene ).color = DEFAULT_GENE_COLOR;

        dataSourcesMap.get( gene ).bdvSource = source;

    }

    private ARGBType asArgbType( Color color )
    {
        return new ARGBType( ARGBType.rgba( color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha() ) );
    }

    private void initBdvWithEmRawData(  )
    {
        System.setProperty( "apple.laf.useScreenMenuBar", "true" );

        loadAndShowSourceInBdv( emRawDataID );

        bdv.getBdvHandle().getViewerPanel().setInterpolation( Interpolation.NLINEAR );

        //bdv.getBdvHandle().getViewerPanel().setCurrentViewerTransform( new AffineTransform3D() );
    }

    private SpimData openSpimData( File file )
    {

        try
        {
            SpimData spimData = new XmlIoSpimData().load( file.toString() );
            return spimData;
        }
        catch ( SpimDataException e )
        {
            e.printStackTrace();
            return null;
        }
    }

    private SpimDataMinimal openImaris( File file, double[] calibration )
    {
        SpimDataMinimal spimDataMinimal;

        try
        {
            spimDataMinimal = Imaris.openIms( file.toString() );
        }
        catch ( IOException e )
        {
            e.printStackTrace();
            return null;
        }

        setScale( calibration, spimDataMinimal );

        return spimDataMinimal;

    }

    private void setScale( double[] calibration, SpimDataMinimal spimDataMinimal )
    {
        final AffineTransform3D affineTransform3D = new AffineTransform3D();
        final Scale scale = new Scale( calibration );
        affineTransform3D.preConcatenate( scale );
        final ViewTransformAffine calibrationTransform = new ViewTransformAffine( "calibration", affineTransform3D );
        spimDataMinimal.getViewRegistrations().getViewRegistration( 0,0 ).identity();
        spimDataMinimal.getViewRegistrations().getViewRegistration( 0,0 ).preconcatenateTransform( calibrationTransform );

        final FinalVoxelDimensions voxelDimensions = new FinalVoxelDimensions( "micrometer", calibration );
        BasicViewSetup basicViewSetup = new BasicViewSetup(  0, "view", null, voxelDimensions );
        spimDataMinimal.getSequenceDescription().getViewSetupsOrdered().set( 0, basicViewSetup);
    }

    private void initDataSources( File directory )
    {

        dataSourcesMap = new TreeMap<>(  );

        File[] files = directory.listFiles();

        for ( File file : files )
        {
            if ( file.getName().endsWith( BDV_XML_SUFFIX ) || file.getName().endsWith( IMARIS_SUFFIX )  )
            {
                String dataSourceName = getDataSourceName( file );

                PlatynereisDataSource source = new PlatynereisDataSource();
                source.file = file;
                source.maxLutValue = 255;

                if ( file.getName().contains( EM_RAW_FILE_ID ) || file.getName().contains( EM_SEGMENTED_FILE_ID ) )
                {
                    if ( file.getName().endsWith( BDV_XML_SUFFIX ) )
                    {
                        source.spimData = openSpimData( file );
                    }
                    else if ( file.getName().contains( IMARIS_SUFFIX ) )
                    {
                        double[] calibration = new double[] { 0.01, 0.01, 0.025 };
                        source.spimDataMinimal = openImaris( file, calibration );
                        source.isSpimDataMinimal = true;
                    }

                    if ( file.getName().contains( EM_RAW_FILE_DEFAULT_ID ) )
                    {
                        emRawDataID = dataSourceName;
                        ProSPrRegistration.setEmSimilarityTransform( source );
                        source.name = EM_RAW_FILE_DEFAULT_ID;
                    }

                    if ( file.getName().contains( EM_RAW_FILE_ID )  )
                    {
                        source.color = DEFAULT_EM_RAW_COLOR;
                    }

                    if ( file.getName().contains( EM_SEGMENTED_FILE_ID ) )
                    {
                        source.color = DEFAULT_EM_SEGMENTATION_COLOR;
                    }
                }
                else // mainCommand gene
                {
                    source.color = DEFAULT_GENE_COLOR;
                }

                dataSourcesMap.put( dataSourceName, source );

            }
        }

    }

    private String getDataSourceName( File file )
    {
        String dataSourceName = null;

        if ( file.getName().endsWith( BDV_XML_SUFFIX ) )
		{
			dataSourceName = file.getName().replaceAll( BDV_XML_SUFFIX, "" );
		}
		else if ( file.getName().endsWith( IMARIS_SUFFIX ) )
		{
			dataSourceName = file.getName().replaceAll( IMARIS_SUFFIX, "" );
		}

        return dataSourceName;
    }


    public static void main( String... args )
    {
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();
        ij.command().run( MainCommand.class, true );
    }


}