package de.embl.cba.mobie.projects.projectsCreator;

import bdv.img.n5.N5ImageLoader;
import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;
import de.embl.cba.mobie.n5.N5S3ImageLoader;
import de.embl.cba.mobie.n5.S3Authentication;
import de.embl.cba.mobie.n5.XmlIoN5S3ImageLoader;
import de.embl.cba.mobie.utils.Utils;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.SpimDataIOException;
import mpicbg.spim.data.sequence.ImgLoader;
import org.apache.commons.io.FileUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import static de.embl.cba.mobie.n5.XmlIoN5S3ImageLoader.*;
import static mpicbg.spim.data.XmlKeys.IMGLOADER_FORMAT_ATTRIBUTE_NAME;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static de.embl.cba.mobie.utils.ExportUtils.getBdvFormatFromSpimDataMinimal;
import static de.embl.cba.mobie.utils.ExportUtils.getN5FileFromXmlPath;
import static mpicbg.spim.data.XmlKeys.IMGLOADER_FORMAT_ATTRIBUTE_NAME;

public class RemoteMetadataCreator {
    Project project;
    String signingRegion;
    String serviceEndpoint;
    String bucketName;
    S3Authentication authentication;

    public RemoteMetadataCreator( Project project ) {
        this.project = project;
    }



    private void deleteAllRemoteMetadata() throws IOException {
        for ( String datasetName: project.getDatasetNames() ) {
            if ( !datasetName.equals("") ) {
                File remoteDir = new File( project.getRemoteImagesDirectoryPath( datasetName ) );
                FileUtils.cleanDirectory( remoteDir );
            }
        }
    }

    public Element createImageLoaderXmlElement ( ProjectsCreator.BdvFormat bdvFormat, String datasetName, String imageName )
    {
        String key = null;
        String format = null;
        switch (bdvFormat) {
            case n5:
                key = datasetName + "/images/local/" + imageName + ".n5";
                format = "bdv.n5.s3";
        }

        final Element elem = new Element("ImageLoader");
        elem.setAttribute(IMGLOADER_FORMAT_ATTRIBUTE_NAME, format);

        elem.addContent( new Element(KEY).addContent( key ));
        elem.addContent( new Element(SIGNING_REGION).addContent( signingRegion ));
        elem.addContent( new Element( SERVICE_ENDPOINT ).addContent( serviceEndpoint ) );
        elem.addContent( new Element(BUCKET_NAME).addContent( bucketName ));
        elem.addContent( new Element(AUTHENTICATION).addContent( authentication.toString() ));

        return elem;
    }

    public void saveXml( final SpimDataMinimal spimData, String datasetName, String imagename,
                         final String xmlFile, ProjectsCreator.BdvFormat bdvFormat ) throws SpimDataException
    {
        XmlIoSpimDataMinimal io = new XmlIoSpimDataMinimal();
        final File xmlFileDirectory = new File( xmlFile ).getParentFile();
        final Document doc = new Document( io.toXml( spimData, xmlFileDirectory ) );
        // remove default image loader, and replace with custom one
        Element imageLoaderElement = createImageLoaderXmlElement( bdvFormat, datasetName, imagename );
        Element baseElement = (Element) doc.getContent( 0 );
        ((Element) baseElement.getContent( 1 )).setContent( 0, imageLoaderElement );
        final XMLOutputter xout = new XMLOutputter( Format.getPrettyFormat() );
        try
        {
            xout.output( doc, new FileOutputStream( xmlFile ) );
        }
        catch ( final IOException e )
        {
            throw new SpimDataIOException( e );
        }
    }

    private boolean addRemoteMetadataForImage( String datasetName, String imageName ) throws SpimDataException, IOException {
        String localXmlLocation = project.getLocalImageXmlPath( datasetName, imageName );
        String remoteXmlLocation = project.getRemoteImagesDirectoryPath( datasetName );

        SpimDataMinimal spimDataMinimal = new XmlIoSpimDataMinimal().load( localXmlLocation );
        ProjectsCreator.BdvFormat bdvFormat = getBdvFormatFromSpimDataMinimal( spimDataMinimal );

        if ( bdvFormat == null ) {
            Utils.log( "Image: " + imageName + " in dataset:" + datasetName + " is of an unsupported format. \n" +
                    "Aborting, and removing all remote metadata" );
            return false;
        } else {
            spimDataMinimal.setBasePath( new File( remoteXmlLocation ) );
            saveXml( spimDataMinimal, datasetName, imageName,
                    new File(remoteXmlLocation, imageName + ".xml").getAbsolutePath(),
                    bdvFormat );
        }

        return true;
    }

    private boolean addRemoteMetadataForDataset( String datasetName ) throws SpimDataException, IOException {
        for ( String imageName: project.getDataset( datasetName ).getImageNames() ) {
            if ( !imageName.equals("") ) {
                Utils.log("Adding metadata for image: " + imageName );
                boolean addedSuccessfully = addRemoteMetadataForImage( datasetName, imageName );
                if ( !addedSuccessfully ) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean addAllRemoteMetadata() throws SpimDataException, IOException {
        for ( String datasetName: project.getDatasetNames() ) {
            if ( !datasetName.equals("") ) {
                Utils.log("Adding metadata for dataset: " + datasetName );
                boolean addedSuccessfully = addRemoteMetadataForDataset( datasetName );
                if ( !addedSuccessfully ) {
                    return false;
                }
            }
        }
        return true;
    }

    public void createRemoteMetadata( String signingRegion, String serviceEndpoint, String bucketName,
                                      S3Authentication authentication ) {
        this.signingRegion = signingRegion;
        this.serviceEndpoint = serviceEndpoint;
        this.bucketName = bucketName;
        this.authentication = authentication;

        try {
            deleteAllRemoteMetadata();

            try {
                // If some issue occurs, delete the remote metadata
                if (!addAllRemoteMetadata()) {
                    deleteAllRemoteMetadata();
                }
            } catch (SpimDataException | IOException e) {
                Utils.log( "Error - aborting, and removing all remote metadata" );
                deleteAllRemoteMetadata();
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
