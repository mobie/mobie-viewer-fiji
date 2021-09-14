package org.embl.mobie.viewer.source;

import bdv.util.volatiles.SharedQueue;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.n5.ome.zarr.loaders.N5S3OMEZarrImageLoader;
import de.embl.cba.n5.ome.zarr.loaders.xml.XmlN5OmeZarrImageLoader;
import de.embl.cba.n5.ome.zarr.openers.OMEZarrOpener;
import de.embl.cba.n5.ome.zarr.openers.OMEZarrS3Opener;
import de.embl.cba.n5.openorganelle.OpenOrganelleS3Opener;
import de.embl.cba.tables.FileAndUrlUtils;
import mpicbg.spim.data.SpimData;
import net.imglib2.util.Cast;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;

import static mpicbg.spim.data.XmlKeys.IMGLOADER_TAG;
import static mpicbg.spim.data.XmlKeys.SEQUENCEDESCRIPTION_TAG;

public class SpimDataOpener {

    public SpimDataOpener() {}

    public SpimData openSpimData( String imagePath, ImageDataFormat imageDataFormat ) {
        SpimData spimData = null;
        switch ( imageDataFormat ) {
            case BdvN5:
            case BdvN5S3:
                spimData = BdvUtils.openSpimData( imagePath );
                break;
            case OmeZarr:
                spimData = openOmeZarrData( imagePath );
                break;
            case OmeZarrS3:
                spimData = openOmeZarrS3Data( imagePath );
                break;
            case BdvOmeZarrS3:
                spimData = openBdvOmeZarrS3Data( imagePath );
                break;
            case BdvOmeZarr:
                spimData = openBdvZarrData( imagePath );
                break;
            case OpenOrganelleS3:
                spimData = openOpenOrganelleData( imagePath );
        }

        return spimData;
    }

    public SpimData openSpimData(String imagePath, ImageDataFormat imageDataFormat, SharedQueue sharedQueue ) {
        SpimData spimData = null;
        switch ( imageDataFormat ) {
            case BdvN5:
            case BdvN5S3:
                spimData = BdvUtils.openSpimData( imagePath );
                break;
            case OmeZarr:
                spimData = openOmeZarrData( imagePath, sharedQueue );
                break;
            case OmeZarrS3:
                spimData = openOmeZarrS3Data( imagePath );
                break;
            case BdvOmeZarrS3:
                spimData = openBdvOmeZarrS3Data( imagePath );
                break;
            case BdvOmeZarr:
                spimData = openBdvZarrData( imagePath );
                break;
            case OpenOrganelleS3:
                spimData = openOpenOrganelleData( imagePath );
        }

        return spimData;
    }

    private SpimData openOmeZarrData( String path )
    {
        try {
            return OMEZarrOpener.openFile( path );
        } catch ( IOException e ) {
            e.printStackTrace();
        }
        return null;
    }

    private SpimData openOmeZarrData(String path, SharedQueue sharedQueue )
    {
        try {
            return OMEZarrOpener.openFile( path, sharedQueue);
        } catch ( IOException e ) {
            e.printStackTrace();
        }
        return null;
    }

    private SpimData openOmeZarrS3Data( String path )
    {
        try {
            return OMEZarrS3Opener.readURL( path );
        } catch ( IOException e ) {
            e.printStackTrace();
        }
        return null;
    }

    private SpimData openOpenOrganelleData( String path )
    {
        try {
            return OpenOrganelleS3Opener.readURL( path );
        } catch ( IOException e ) {
            e.printStackTrace();
        }
        return null;
    }

    private SpimData openBdvOmeZarrS3Data( String path )
    {
        try {
            final SAXBuilder sax = new SAXBuilder();
            InputStream stream = FileAndUrlUtils.getInputStream(path);
            final Document doc = sax.build(stream);
            final Element imgLoaderElem = doc.getRootElement().getChild(SEQUENCEDESCRIPTION_TAG).getChild(IMGLOADER_TAG);
            HashMap<String, Integer> axesMap = new HashMap<>();
            String bucketAndObject = imgLoaderElem.getChild( "BucketName").getText() + "/" + imgLoaderElem.getChild( "Key" ).getText();
            final String[] split = bucketAndObject.split("/");
            String bucket = split[0];
            String object = Arrays.stream( split ).skip( 1 ).collect( Collectors.joining( "/") );
            N5S3OMEZarrImageLoader imageLoader = new N5S3OMEZarrImageLoader(imgLoaderElem.getChild( "ServiceEndpoint" ).getText(), imgLoaderElem.getChild( "SigningRegion" ).getText(),bucket, object, ".");
            SpimData spim = new SpimData(null, Cast.unchecked(imageLoader.getSequenceDescription()), imageLoader.getViewRegistrations());
            SpimData sp1 = BdvUtils.openSpimData( path );
            sp1.setBasePath(null);
            sp1.getSequenceDescription().setImgLoader( spim.getSequenceDescription().getImgLoader() );
            sp1.getSequenceDescription().getAllChannels().putAll( spim.getSequenceDescription().getAllChannels() );
            return sp1;
        } catch ( IOException | JDOMException e ) {
            e.printStackTrace();
        }
        return null;
    }

    private SpimData openBdvZarrData(String path) {
        try
        {
            final SAXBuilder sax = new SAXBuilder();
            InputStream stream = FileAndUrlUtils.getInputStream(path);
            final Document doc = sax.build(stream);
            final Element imgLoaderElem = doc.getRootElement().getChild(SEQUENCEDESCRIPTION_TAG).getChild(IMGLOADER_TAG);
            String imagesFile = XmlN5OmeZarrImageLoader.getDatasetsPathFromXml(imgLoaderElem, path);
            if(imagesFile != null)
            {
                if ((imagesFile.equals(Paths.get(imagesFile).toString())))
                {
                    SpimData spim =  OMEZarrOpener.openFile( imagesFile );
                    SpimData sp1 = BdvUtils.openSpimData( path );
                    sp1.setBasePath( new File( imagesFile ) );
                    sp1.getSequenceDescription().setImgLoader( spim.getSequenceDescription().getImgLoader() );
                    sp1.getSequenceDescription().getAllChannels().putAll( spim.getSequenceDescription().getAllChannels() );
                    return sp1;
                } else
                {
                    SpimData spim = OMEZarrS3Opener.readURL( imagesFile );
                    SpimData sp1 = BdvUtils.openSpimData( path );
                    sp1.setBasePath(null);
                    sp1.getSequenceDescription().setImgLoader( spim.getSequenceDescription().getImgLoader() );
                    sp1.getSequenceDescription().getAllChannels().putAll( spim.getSequenceDescription().getAllChannels() );
                    return sp1;
                }
            }
        } catch (JDOMException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
