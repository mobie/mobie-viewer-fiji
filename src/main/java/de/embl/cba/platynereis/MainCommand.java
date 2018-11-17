package de.embl.cba.platynereis;

import bdv.img.imaris.Imaris;
import bdv.spimdata.SpimDataMinimal;
import bdv.tools.brightness.ConverterSetup;
import bdv.util.*;
import bdv.viewer.Source;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.bdv.utils.labels.luts.*;
import de.embl.cba.platynereis.ui.BdvSourcesPanel;
import de.embl.cba.platynereis.ui.MainFrame;
import ij.IJ;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.registration.ViewTransformAffine;
import mpicbg.spim.data.sequence.FinalVoxelDimensions;
import net.imagej.ImageJ;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.Scale;
import net.imglib2.type.volatiles.VolatileARGBType;
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

import static de.embl.cba.bdv.utils.BdvUserInterfaceUtils.showBrightnessDialog;

@Plugin(type = Command.class, menuPath = "Plugins>Registration>EMBL>Platynereis", initializer = "init")
public class MainCommand extends DynamicCommand implements Interactive
{

    @Parameter
    public LogService logService;

    Bdv bdv;

    public Map< String, PlatynereisDataSource > dataSources;
    String emRawDataName;
    AffineTransform3D emRawDataTransform;
    BdvSourcesPanel legend;


    public void init()
    {
        System.setProperty( "apple.laf.useScreenMenuBar", "true" );

        String dir = IJ.getDirectory( "Please choose Platynereis directory" );

        File[] files = new File( dir ).listFiles();
        Arrays.sort( files );

        dataSources = Collections.synchronizedMap( new LinkedHashMap() );

        initDataSources( files );

        preFetchProsprDataSourcesInSeparateThread();

        initBdvWithEmRawData();

        MainFrame mainFrame = new MainFrame( bdv, this );

        legend = mainFrame.getBdvSourcesPanel();

    }


    public String getEmRawDataName()
    {
        return emRawDataName;
    }


    private void preFetchProsprDataSourcesInSeparateThread( )
    {
        (new Thread(new Runnable(){
            public void run(){
                preFetchProsprDataSources( );
            }
        })).start();
    }

    public void run()
    {

    }

    public void removeDataSource( String dataSourceName )
    {
        // TODO: extra class for dataSources ?
        if ( dataSources.get( dataSourceName ).bdvStackSource != null && dataSources.get( dataSourceName ).bdvStackSource.getBdvHandle() != null )
        {
            dataSources.get( dataSourceName ).bdvStackSource.removeFromBdv();
        }
    }

    public void setDataSourceColor( String sourceName, Color color )
    {
        // TODO: extra class for dataSources ?
        dataSources.get( sourceName ).bdvStackSource.setColor( Utils.asArgbType( color ) );
        dataSources.get( sourceName ).color = color;
    }


    public void setBrightness( String sourceName )
    {

        final int sourceId = BdvUtils.getSourceIndex( bdv, sourceName );
        final ConverterSetup converterSetup = bdv.getBdvHandle().getSetupAssignments().getConverterSetups().get( sourceId );

		showBrightnessDialog( sourceName, converterSetup );

//		GenericDialog gd = new GenericDialog( "LUT max value" );
//        gd.addNumericField( "LUT max value: ", dataSources.get( sourceName ).maxLutValue, 0 );
//        gd.showDialog();
//        if ( gd.wasCanceled() ) return;
//
//        int max = ( int ) gd.getNextNumber();
//
//        dataSources.get( sourceName ).bdvStackSource.setDisplayRange( 0.0, max );
//        dataSources.get( sourceName ).maxLutValue = max;

    }


    private void setName( String name, PlatynereisDataSource source )
    {
        if ( source.spimData != null )
        {
            source.spimData.getSequenceDescription().getViewSetupsOrdered().get( 0 ).getChannel().setName( name );
        }
    }

    private void initBdvWithEmRawData(  )
    {
        bdv = Utils.showSourceInBdv( dataSources.get( emRawDataName ), bdv );

        // bdv.getBdvHandle().getViewerPanel().setInterpolation( Interpolation.NLINEAR );

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

        setScale( spimDataMinimal, calibration );

        return spimDataMinimal;

    }

    private void setScale( SpimDataMinimal spimDataMinimal, double[] calibration )
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
            final String fileName = file.getName();

            if ( ! fileName.endsWith( Constants.BDV_XML_SUFFIX ) && ! fileName.endsWith( Constants.IMARIS_SUFFIX ) )
            {
                continue;
            }

//            if ( ! fileName.contains( Constants.EM_FILE_ID ) && ! ( fileName.contains( Constants.NEW_PROSPR ) || fileName.contains( Constants.AVG_PROSPR ) ) )
//            {
//                continue;
//            }

            if ( fileName.contains( "AcTub" ) ) continue;

            String dataSourceName = getDataSourceName( file );
            PlatynereisDataSource source = new PlatynereisDataSource();

            dataSources.put( dataSourceName, source );
            source.file = file;

            if ( fileName.contains( Constants.EM_FILE_ID ) )
            {
                source.maxLutValue = 255;
            }
            else
            {
                source.maxLutValue = 1000; // to render the binary prospr more transparent
            }

            source.name = dataSourceName;

            if ( fileName.contains( Constants.EM_FILE_ID ) )
            {
                if ( fileName.endsWith( Constants.BDV_XML_SUFFIX ) )
                {
                    source.spimData = Utils.openSpimData( file );
                }
                else if ( fileName.contains( Constants.IMARIS_SUFFIX ) )
                {
                    double[] calibration = new double[] { 0.01, 0.01, 0.025 };
                    source.spimDataMinimal = openImaris( file, calibration );
                    source.isSpimDataMinimal = true;
                }

                if ( fileName.contains( Constants.EM_RAW_FILE_DEFAULT_ID ) )
                {
                    emRawDataName = dataSourceName;
                    source.name = Constants.EM_RAW_FILE_DEFAULT_ID;
                }

                if ( fileName.contains( Constants.EM_RAW_FILE_ID )  )
                {
                    source.color = Constants.DEFAULT_EM_RAW_COLOR;
                }

                if ( fileName.contains( Constants.EM_SEGMENTED_FILE_ID ) )
                {
                    source.color = Constants.DEFAULT_EM_SEGMENTATION_COLOR;
                }

                if ( fileName.contains( Constants.LABELS_ID ) ) // labels
                {
                    Source< VolatileARGBType > labelSource = null;

                    if ( fileName.contains( Constants.CELLULAR_MODELS ))
                    {
                        source.labelSource = new ARGBConvertedUnsignedShortTypeLabelsSource( source.spimData, 0 );
                        source.isLabelSource = true;
                        source.spimData = null;
                        source.maxLutValue = 255;
                    }
                    else
                    {
                        source.labelSource = new ARGBConvertedUnsignedLongTypeLabelsSource( source.spimData, 0 );
                        source.isLabelSource = true;
                        source.spimData = null;
                        source.maxLutValue = 255;
                    }

                }
            }
            else // gene
            {
                source.color = Constants.DEFAULT_GENE_COLOR;
            }

        }

    }


    private void preFetchProsprDataSources( )
    {
        Set< String > names = dataSources.keySet();

        for (  String name : names )
        {
            PlatynereisDataSource source = dataSources.get( name );

            if ( source.file.getName().contains( Constants.EM_FILE_ID ) ) continue;

            if ( source.file.getName().endsWith( Constants.BDV_XML_SUFFIX ) )
            {
                if ( source.spimData == null )
                {
                    source.spimData = Utils.openSpimData( source.file );

//                    if ( labelSource.file.getName().contains( Constants.NEW_PROSPR ) )
//                    {
                    if ( ! source.file.getName().contains( Constants.NEW_PROSPR )
                            && ! source.file.getName().contains( Constants.AVG_PROSPR ) )
                    {
                        ProSPrRegistration.setInverseEmSimilarityTransform( source );
                    }

//                    }
                }
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

//		if ( file.getName().contains( Constants.NEW_PROSPR ) )
//        {
//            dataSourceName = dataSourceName.replace(  Constants.NEW_PROSPR, "" );
//        }

        return dataSourceName;
    }


    public static void main( String... args )
    {
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();
        ij.command().run( MainCommand.class, true );
    }


}