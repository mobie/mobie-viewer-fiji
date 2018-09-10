package de.embl.cba.platynereis;

import bdv.img.imaris.Imaris;
import bdv.spimdata.SpimDataMinimal;
import bdv.util.*;
import bdv.viewer.Interpolation;
import com.sun.source.tree.SynchronizedTree;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.registration.ViewTransformAffine;
import mpicbg.spim.data.sequence.FinalVoxelDimensions;
import net.imagej.ImageJ;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.Scale;
import net.imglib2.type.numeric.ARGBType;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.command.Interactive;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Plugin(type = Command.class, menuPath = "Plugins>Registration>EMBL>Platynereis", initializer = "init")
public class MainCommand extends DynamicCommand implements Interactive
{

    @Parameter
    public LogService logService;

    Bdv bdv;

    public Map< String, PlatynereisDataSource > dataSourcesMap;

    String emRawDataID;
    AffineTransform3D emRawDataTransform;
    LegendUI legend;


    public void init()
    {
        System.setProperty( "apple.laf.useScreenMenuBar", "true" );

        String dir = IJ.getDirectory( "Please choose Platynereis directory" );

        File[] files = new File( dir ).listFiles();
        Arrays.sort( files );

        dataSourcesMap = Collections.synchronizedMap( new LinkedHashMap() );

        initDataSources( files );

        loadProSPrDataSourcesInSeparateThread();

        initBdvWithEmRawData();

        createLegend();

        new MainUI( bdv, this );
    }

    private void loadProSPrDataSourcesInSeparateThread( )
    {
        (new Thread(new Runnable(){
            public void run(){
                loadProSPrDataSources( );
            }
        })).start();
    }

    private void createLegend()
    {
        legend = new LegendUI( this );
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

    public void run()
    {

    }

    private void print( String text )
    {
        Utils.log( text );
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

    public void addSourceToBdv( String name )
    {
        PlatynereisDataSource source = dataSourcesMap.get( name );

        if ( source.bdvSource == null )
        {
            switch ( Constants.BDV_XML_SUFFIX ) // TODO: makes no sense...
            {
                case ".tif":
                    addSourceFromTiffFile( name );
                    break;
                case ".xml":
                    if ( source.spimData == null )
                    {
                        source.spimData = Utils.openSpimData( source.file );
                    }
                    showSourceInBdv( name );
                    break;
                default:
                    logService.error( "Unsupported format: " + Constants.BDV_XML_SUFFIX );
            }
        }

        source.bdvSource.setActive( true );
        source.isActive = true;
        source.bdvSource.setColor( asArgbType( source.color ) );
        source.name = name;

        legend.addSource( source );
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


    private void showSourceInBdv( String dataSourceName )
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
            setName( dataSourceName, source );

            source.bdvSource = BdvFunctions.show( source.spimData, BdvOptions.options().addTo( bdv ) ).get( 0 );

            source.bdvSource.setColor( asArgbType( source.color ) );
            source.bdvSource.setDisplayRange( 0.0, source.maxLutValue );

            bdv = source.bdvSource.getBdvHandle();
        }

    }

    private void setName( String name, PlatynereisDataSource source )
    {
        if ( source.spimData != null )
        {
            source.spimData.getSequenceDescription().getViewSetupsOrdered().get( 0 ).getChannel().setName( name );
        }
    }

    private void addSourceFromTiffFile( String gene )
    {
        ImagePlus imp = IJ.openImage( dataSourcesMap.get( gene ).file.toString() );
        Img img = ImageJFunctions.wrap( imp );

        AffineTransform3D prosprScaling = new AffineTransform3D();
        prosprScaling.scale( Constants.PROSPR_SCALING_IN_MICROMETER );

        final BdvSource source = BdvFunctions.show( img, gene, Bdv.options().addTo( bdv ).sourceTransform( prosprScaling ) );
        source.setColor( asArgbType( Constants.DEFAULT_GENE_COLOR ) );
        dataSourcesMap.get( gene ).color = Constants.DEFAULT_GENE_COLOR;
        dataSourcesMap.get( gene ).bdvSource = source;

    }

    private ARGBType asArgbType( Color color )
    {
        return new ARGBType( ARGBType.rgba( color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha() ) );
    }

    private void initBdvWithEmRawData(  )
    {
        showSourceInBdv( emRawDataID );

        bdv.getBdvHandle().getViewerPanel().setInterpolation( Interpolation.NLINEAR );

        //bdv.getBdvHandle().getViewerPanel().setCurrentViewerTransform( new AffineTransform3D() );
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

    private void initDataSources( File[] files )
    {
        for ( File file : files )
        {
            if ( file.getName().endsWith( Constants.BDV_XML_SUFFIX ) || file.getName().endsWith( Constants.IMARIS_SUFFIX )  )
            {
                String dataSourceName = getDataSourceName( file );

                PlatynereisDataSource source = new PlatynereisDataSource();
                dataSourcesMap.put( dataSourceName, source );
                source.file = file;
                source.maxLutValue = 255;

                if ( file.getName().contains( Constants.EM_RAW_FILE_ID ) || file.getName().contains( Constants.EM_SEGMENTED_FILE_ID ) )
                {
                    if ( file.getName().endsWith( Constants.BDV_XML_SUFFIX ) )
                    {
                        source.spimData = Utils.openSpimData( file );
                    }
                    else if ( file.getName().contains( Constants.IMARIS_SUFFIX ) )
                    {
                        double[] calibration = new double[] { 0.01, 0.01, 0.025 };
                        source.spimDataMinimal = openImaris( file, calibration );
                        source.isSpimDataMinimal = true;
                    }

                    if ( file.getName().contains( Constants.EM_RAW_FILE_DEFAULT_ID ) )
                    {
                        emRawDataID = dataSourceName;
                        ProSPrRegistration.setEmSimilarityTransform( source );
                        source.name = Constants.EM_RAW_FILE_DEFAULT_ID;
                    }

                    if ( file.getName().contains( Constants.EM_RAW_FILE_ID )  )
                    {
                        source.color = Constants.DEFAULT_EM_RAW_COLOR;
                    }

                    if ( file.getName().contains( Constants.EM_SEGMENTED_FILE_ID ) )
                    {
                        source.color = Constants.DEFAULT_EM_SEGMENTATION_COLOR;
                    }
                }
                else // gene
                {
                    source.color = Constants.DEFAULT_GENE_COLOR;
                }
            }
        }
    }


    private void loadProSPrDataSources( )
    {
        Set< String > names = dataSourcesMap.keySet();

        for (  String name : names )
        {
            PlatynereisDataSource source = dataSourcesMap.get( name );

            if ( source.file.getName().contains( Constants.EM_FILE_ID ) ) continue;

            if ( source.file.getName().endsWith( Constants.BDV_XML_SUFFIX ) )
            {
                source.spimData = Utils.openSpimData( source.file );
                Utils.log( "Added: " + name );
            }
        }
    }


    private String getDataSourceName( File file )
    {
        String dataSourceName = null;

        if ( file.getName().endsWith( Constants.BDV_XML_SUFFIX ) )
		{
			dataSourceName = file.getName().replaceAll( Constants.BDV_XML_SUFFIX, "" );
		}
		else if ( file.getName().endsWith( Constants.IMARIS_SUFFIX ) )
		{
			dataSourceName = file.getName().replaceAll( Constants.IMARIS_SUFFIX, "" );
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