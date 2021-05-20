package de.embl.cba.mobie2.projectcreator;

import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;
import de.embl.cba.mobie.n5.XmlIoN5S3ImageLoader;
import de.embl.cba.mobie2.Dataset;
import de.embl.cba.mobie2.source.ImageSource;
import de.embl.cba.tables.FileAndUrlUtils;
import ij.IJ;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.SpimDataIOException;
import org.apache.commons.io.FilenameUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static de.embl.cba.mobie2.projectcreator.ProjectCreatorHelper.getBdvFormatFromSpimDataMinimal;
import static de.embl.cba.mobie2.projectcreator.ProjectCreatorHelper.getImageLocationFromSpimDataMinimal;

public class RemoteMetadataCreator {
    ProjectCreator projectCreator;
    String signingRegion;
    String serviceEndpoint;
    String bucketName;

    public RemoteMetadataCreator( ProjectCreator projectCreator ) {
        this.projectCreator = projectCreator;
    }

    private void deleteAllRemoteMetadata() throws IOException {
        for ( String datasetName: projectCreator.getProject().getDatasets() ) {
            if ( !datasetName.equals("") ) {
                Dataset dataset = projectCreator.getDataset( datasetName );
                for ( String imageName: dataset.sources.keySet() ) {
                    deleteRemoteMetadataForImage( datasetName, imageName );
                }
                projectCreator.getDatasetJsonCreator().writeDatasetJson( datasetName, dataset );
            }
        }
    }

    private void deleteRemoteMetadataForImage( String datasetName, String imageName ) throws IOException {
        ImageSource imageSource = projectCreator.getDataset( datasetName ).sources.get( imageName ).get();
        if ( imageSource.imageDataLocations.containsKey("s3store") ) {
            // delete any existing remote metadata
            File currentRemoteXmlLocation = new File( FileAndUrlUtils.combinePath( projectCreator.getDataLocation().getAbsolutePath(),
                    datasetName, imageSource.imageDataLocations.get("s3store") ) );
            if ( currentRemoteXmlLocation.exists() ) {
                if ( !currentRemoteXmlLocation.delete() ) {
                    String errorMessage = "Remote metadata for: " + imageName + " in dataset: " + datasetName + " could not be deleted.";
                    IJ.log(errorMessage);
                    throw new IOException(errorMessage);
                }
            }
            imageSource.imageDataLocations.remove( "s3store" );
        }
    }

    public String getRelativeKey( SpimDataMinimal spimDataMinimal, String datasetName, String imageName,
                                  ProjectCreator.BdvFormat bdvFormat ) throws IOException {
        // check image is within the project folder (if people 'link' to bdv format images they may be outside)
        Path imagePath = Paths.get( getImageLocationFromSpimDataMinimal( spimDataMinimal, bdvFormat ).getAbsolutePath() ).normalize();
        Path projectDataFolder = Paths.get( projectCreator.getDataLocation().getAbsolutePath() ).normalize();

        if ( !imagePath.startsWith( projectDataFolder )) {
            String errorMesage = "Image: " + imageName + " for dataset:" + datasetName + " is not in project folder. \n" +
                    "You can't 'link' to bdv images outside the project folder, when uploading to s3";
            IJ.log( errorMesage );
            throw new IOException( errorMesage );
        }

        Path relativeKey = projectDataFolder.relativize( imagePath );

        return FilenameUtils.separatorsToUnix( relativeKey.toString() );
    }

    public Element createImageLoaderXmlElement ( SpimDataMinimal spimDataMinimal,
                                                 ProjectCreator.BdvFormat bdvFormat,
                                                 String datasetName, String imageName ) throws IOException {
        String key = null;
        switch (bdvFormat) {
            case n5:
                key = getRelativeKey( spimDataMinimal, datasetName, imageName, bdvFormat );
        }

         return new XmlIoN5S3ImageLoader().toXml( serviceEndpoint, signingRegion, bucketName, key );
    }

    public void saveXml( final SpimDataMinimal spimData, String datasetName, String imagename,
                         final String xmlFile, ProjectCreator.BdvFormat bdvFormat ) throws SpimDataException, IOException {
        XmlIoSpimDataMinimal io = new XmlIoSpimDataMinimal();
        final File xmlFileDirectory = new File( xmlFile ).getParentFile();
        final Document doc = new Document( io.toXml( spimData, xmlFileDirectory ) );
        // remove default image loader, and replace with custom one
        Element imageLoaderElement = createImageLoaderXmlElement( spimData, bdvFormat, datasetName, imagename );
        Element baseElement = (Element) doc.getContent( 0 );
        ((Element) baseElement.getContent( 1 )).setContent( 0, imageLoaderElement );
        final XMLOutputter xout = new XMLOutputter( Format.getPrettyFormat() );
        try( FileOutputStream outputStream = new FileOutputStream( xmlFile ))
        {
            xout.output( doc, outputStream );
        }
        catch ( final IOException e )
        {
            throw new SpimDataIOException( e );
        }
    }

    private void addRemoteMetadataForImage( String datasetName, String imageName ) throws SpimDataException, IOException {
        ImageSource imageSource = projectCreator.getDataset( datasetName ).sources.get( imageName ).get();
        String localXmlLocation = FileAndUrlUtils.combinePath( projectCreator.getDataLocation().getAbsolutePath(),
                datasetName, imageSource.imageDataLocations.get("fileSystem") );

        deleteRemoteMetadataForImage( datasetName, imageName );

        String remoteXmlLocation = FileAndUrlUtils.combinePath( projectCreator.getDataLocation().getAbsolutePath(),
                datasetName, "images", "remote" );

        SpimDataMinimal spimDataMinimal = new XmlIoSpimDataMinimal().load( localXmlLocation );
        ProjectCreator.BdvFormat bdvFormat = getBdvFormatFromSpimDataMinimal( spimDataMinimal );

        if ( bdvFormat == null ) {
            String errorMesage = "Image:" + imageName + " in dataset:" + datasetName + " is of an unsupported format";
            IJ.log( errorMesage );
            throw new IOException( errorMesage );
        } else {
            spimDataMinimal.setBasePath( new File( remoteXmlLocation ) );
            saveXml( spimDataMinimal, datasetName, imageName,
                    new File(remoteXmlLocation, imageName + ".xml").getAbsolutePath(),
                    bdvFormat );
            imageSource.imageDataLocations.put( "s3store", "images/remote/" + imageName + ".xml" );
        }

    }

    private void addRemoteMetadataForDataset( String datasetName ) throws SpimDataException, IOException {
        Dataset dataset = projectCreator.getDataset( datasetName );
        for ( String imageName: dataset.sources.keySet() ) {
            if ( !imageName.equals("") ) {
                IJ.log("Adding metadata for image: " + imageName );
                addRemoteMetadataForImage( datasetName, imageName );
            }
        }

        projectCreator.getDatasetJsonCreator().writeDatasetJson( datasetName, dataset );
    }

    private void addAllRemoteMetadata() throws SpimDataException, IOException {
        for ( String datasetName: projectCreator.getProject().getDatasets() ) {
            if ( !datasetName.equals("") ) {
                IJ.log("Adding metadata for dataset: " + datasetName );
                addRemoteMetadataForDataset( datasetName );
            }
        }
    }

    public void createRemoteMetadata( String signingRegion, String serviceEndpoint, String bucketName ) {
        this.signingRegion = signingRegion;
        this.serviceEndpoint = serviceEndpoint;
        this.bucketName = bucketName;

        try {
            // clean any old remote metadata
            deleteAllRemoteMetadata();

            try {
               addAllRemoteMetadata();
            } catch (SpimDataException | IOException e) {
                IJ.log( "Error - aborting, and removing all remote metadata" );
                deleteAllRemoteMetadata();
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}