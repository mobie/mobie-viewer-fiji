package de.embl.cba.mobie.projects.projectsCreator;

import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;
import de.embl.cba.mobie.n5.S3Authentication;
import de.embl.cba.mobie.n5.XmlIoN5S3ImageLoader;
import de.embl.cba.mobie.utils.Utils;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.SpimDataIOException;
import org.apache.commons.io.FileUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static de.embl.cba.mobie.utils.ExportUtils.getBdvFormatFromSpimDataMinimal;

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
        switch (bdvFormat) {
            case n5:
                key = datasetName + "/images/local/" + imageName + ".n5";
        }

         return new XmlIoN5S3ImageLoader().toXml( serviceEndpoint, signingRegion, bucketName, key, authentication );
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

    private void addRemoteMetadataForImage( String datasetName, String imageName ) throws SpimDataException, IOException {
        String localXmlLocation = project.getLocalImageXmlPath( datasetName, imageName );
        String remoteXmlLocation = project.getRemoteImagesDirectoryPath( datasetName );

        SpimDataMinimal spimDataMinimal = new XmlIoSpimDataMinimal().load( localXmlLocation );
        ProjectsCreator.BdvFormat bdvFormat = getBdvFormatFromSpimDataMinimal( spimDataMinimal );

        if ( bdvFormat == null ) {
            String errorMesage = "Image: " + imageName + " in dataset:" + datasetName + " is of an unsupported format";
            Utils.log( errorMesage );
            throw new IOException( errorMesage );
        } else {
            spimDataMinimal.setBasePath( new File( remoteXmlLocation ) );
            saveXml( spimDataMinimal, datasetName, imageName,
                    new File(remoteXmlLocation, imageName + ".xml").getAbsolutePath(),
                    bdvFormat );
        }

    }

    private void addRemoteMetadataForDataset( String datasetName ) throws SpimDataException, IOException {
        for ( String imageName: project.getDataset( datasetName ).getImageNames() ) {
            if ( !imageName.equals("") ) {
                Utils.log("Adding metadata for image: " + imageName );
                addRemoteMetadataForImage( datasetName, imageName );
            }
        }
    }

    private void addAllRemoteMetadata() throws SpimDataException, IOException {
        for ( String datasetName: project.getDatasetNames() ) {
            if ( !datasetName.equals("") ) {
                Utils.log("Adding metadata for dataset: " + datasetName );
                addRemoteMetadataForDataset( datasetName );
            }
        }
    }

    public void createRemoteMetadata( String signingRegion, String serviceEndpoint, String bucketName,
                                      S3Authentication authentication ) {
        this.signingRegion = signingRegion;
        this.serviceEndpoint = serviceEndpoint;
        this.bucketName = bucketName;
        this.authentication = authentication;

        try {
            // clean any old remote metadata
            deleteAllRemoteMetadata();

            try {
               addAllRemoteMetadata();
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
