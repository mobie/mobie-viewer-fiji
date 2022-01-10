package org.embl.mobie.viewer.source;

import bdv.img.imaris.Imaris;
import bdv.spimdata.SpimDataMinimal;
import bdv.util.volatiles.SharedQueue;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.bdv.utils.CustomXmlIoSpimData;
import de.embl.cba.tables.FileAndUrlUtils;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.generic.AbstractSpimData;
import net.imglib2.util.Cast;
import org.embl.mobie.io.n5.openers.N5Opener;
import org.embl.mobie.io.n5.openers.N5S3Opener;
import org.embl.mobie.io.ome.zarr.loaders.N5S3OMEZarrImageLoader;
import org.embl.mobie.io.ome.zarr.loaders.xml.XmlN5OmeZarrImageLoader;
import org.embl.mobie.io.ome.zarr.openers.OMEZarrOpener;
import org.embl.mobie.io.ome.zarr.openers.OMEZarrS3Opener;
import org.embl.mobie.io.openorganelle.OpenOrganelleS3Opener;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Collectors;

import static mpicbg.spim.data.XmlKeys.IMGLOADER_TAG;
import static mpicbg.spim.data.XmlKeys.SEQUENCEDESCRIPTION_TAG;

public class SpimDataOpener {

    public SpimDataOpener() {}

    public AbstractSpimData openSpimData( String imagePath, ImageDataFormat imageDataFormat ) {
        switch ( imageDataFormat ) {
            case Imaris:
                return openImaris( imagePath );
            case BdvHDF5:
            case BdvN5:
            case BdvN5S3:
                return openBdvHdf5AndBdvN5AndBdvN5S3( imagePath );
            case OmeZarr:
                return openOmeZarr( imagePath );
            case OmeZarrS3:
                return openOmeZarrS3( imagePath );
            case BdvOmeZarr:
                return openBdvOmeZarr( imagePath );
            case BdvOmeZarrS3:
                return openBdvOmeZarrS3( imagePath );
            case OpenOrganelleS3:
                return openOpenOrganelleS3( imagePath );
            default:
                throw new UnsupportedOperationException("Opening of " + imageDataFormat + " is not supported.");
        }
    }

    public AbstractSpimData openSpimData(String imagePath, ImageDataFormat imageDataFormat, SharedQueue sharedQueue ) {
        switch ( imageDataFormat ) {
            case BdvN5:
                return openBdvN5( imagePath, sharedQueue );
            case BdvN5S3:
                return openBdvN5S3( imagePath, sharedQueue );
            case OmeZarr:
                return openOmeZarr( imagePath, sharedQueue );
            case OmeZarrS3:
                return openOmeZarrS3( imagePath, sharedQueue );
            default:
                throw new UnsupportedOperationException("Shared queues for " + imageDataFormat + " are not yet supported!" );
        }
    }

    @NotNull
    private SpimDataMinimal openImaris( String imagePath )
    {
        try
        {
            return Imaris.openIms( imagePath );
        } catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }

    private SpimData openBdvHdf5AndBdvN5AndBdvN5S3( String path )
    {
        try
        {
            InputStream stream = FileAndUrlUtils.getInputStream( path );
            SpimData spimData = new CustomXmlIoSpimData().loadFromStream( stream, path );
            return spimData;
        }
        catch ( SpimDataException | IOException e )
        {
            System.out.println( path );
            e.printStackTrace();
            return null;
        }
    }


    private SpimData openBdvN5( String path, SharedQueue queue)
    {
        try {
            return N5Opener.openFile( path, queue );
        } catch ( IOException e ) {
            e.printStackTrace();
        }
        return null;

    }
    private SpimData openBdvN5S3( String path, SharedQueue queue)
    {
        try {
            return N5S3Opener.readURL( path, queue );
        } catch ( IOException e ) {
            e.printStackTrace();
        }
        return null;

    }

    private SpimData openOmeZarr( String path )
    {
        try {
            return OMEZarrOpener.openFile( path );
        } catch ( IOException e ) {
            e.printStackTrace();
        }
        return null;
    }

    private SpimData openOmeZarr( String path, SharedQueue sharedQueue )
    {
        try {
            return OMEZarrOpener.openFile( path, sharedQueue);
        } catch ( IOException e ) {
            e.printStackTrace();
        }
        return null;
    }

    private SpimData openOmeZarrS3( String path )
    {
        try {
            return OMEZarrS3Opener.readURL( path);
        } catch ( IOException e ) {
            e.printStackTrace();
        }
        return null;
    }

    private SpimData openOmeZarrS3( String path, SharedQueue sharedQueue  )
    {
        try {
            return OMEZarrS3Opener.readURL( path, sharedQueue);
        } catch ( IOException e ) {
            e.printStackTrace();
        }
        return null;
    }

    private SpimData openOpenOrganelleS3( String path )
    {
        try {
            return OpenOrganelleS3Opener.readURL( path );
        } catch ( IOException e ) {
            e.printStackTrace();
        }
        return null;
    }

    private SpimData openBdvOmeZarrS3( String path )
    {
        try {
            final SAXBuilder sax = new SAXBuilder();
            InputStream stream = FileAndUrlUtils.getInputStream(path);
            final Document doc = sax.build(stream);
            final Element imgLoaderElem = doc.getRootElement().getChild(SEQUENCEDESCRIPTION_TAG).getChild(IMGLOADER_TAG);
            String bucketAndObject = imgLoaderElem.getChild( "BucketName").getText() + "/" + imgLoaderElem.getChild( "Key" ).getText();
            final String[] split = bucketAndObject.split("/");
            String bucket = split[0];
            String object = Arrays.stream( split ).skip( 1 ).collect( Collectors.joining( "/") );
            N5S3OMEZarrImageLoader imageLoader = new N5S3OMEZarrImageLoader(imgLoaderElem.getChild( "ServiceEndpoint" ).getText(), imgLoaderElem.getChild( "SigningRegion" ).getText(),bucket, object, ".");

            // TODO: Add explanation to what is happening!
            SpimData spimData = new SpimData(null, Cast.unchecked(imageLoader.getSequenceDescription()), imageLoader.getViewRegistrations());
            SpimData spimData1 = openBdvHdf5AndBdvN5AndBdvN5S3( path );
            spimData1.setBasePath(null);
            spimData1.getSequenceDescription().setImgLoader( spimData.getSequenceDescription().getImgLoader() );
            spimData1.getSequenceDescription().getAllChannels().putAll( spimData.getSequenceDescription().getAllChannels() );
            return spimData1;
        } catch ( IOException | JDOMException e ) {
            e.printStackTrace();
        }
        return null;
    }

    private SpimData openBdvOmeZarr( String path) {
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
                    SpimData sp1 = openBdvHdf5AndBdvN5AndBdvN5S3( path );
                    sp1.setBasePath( new File( imagesFile ) );
                    sp1.getSequenceDescription().setImgLoader( spim.getSequenceDescription().getImgLoader() );
                    sp1.getSequenceDescription().getAllChannels().putAll( spim.getSequenceDescription().getAllChannels() );
                    return sp1;
                } else
                {
                    SpimData spim = OMEZarrS3Opener.readURL( imagesFile );
                    SpimData sp1 = openBdvHdf5AndBdvN5AndBdvN5S3( path );
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
