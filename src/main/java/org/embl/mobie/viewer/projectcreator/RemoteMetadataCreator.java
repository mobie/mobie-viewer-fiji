package org.embl.mobie.viewer.projectcreator;

import mpicbg.spim.data.generic.AbstractSpimData;
import org.embl.mobie.io.ImageDataFormat;
import org.embl.mobie.io.SpimDataOpener;
import org.embl.mobie.io.n5.loaders.xml.XmlIoN5S3ImageLoader;
import org.embl.mobie.io.ome.zarr.loaders.xml.XmlN5S3OmeZarrImageLoader;
import org.embl.mobie.viewer.Dataset;
import org.embl.mobie.viewer.source.ImageSource;
import org.embl.mobie.viewer.source.StorageLocation;
import org.embl.mobie.io.util.FileAndUrlUtils;
import ij.IJ;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.SpimDataIOException;
import mpicbg.spim.data.XmlIoSpimData;
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

public class RemoteMetadataCreator {

    static { net.imagej.patcher.LegacyInjector.preinit(); }

    ProjectCreator projectCreator;
    String signingRegion;
    String serviceEndpoint;
    String bucketName;
    ImageDataFormat remoteImageDataFormat;
    ImageDataFormat localImageDataFormat;

    /**
     * Make a remoteMetadataCreator - includes all functions for creation/modification of remote metadata
     * @param projectCreator projectCreator
     */
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

        projectCreator.getProjectJsonCreator().removeImageDataFormat( remoteImageDataFormat );
    }

    private void deleteRemoteMetadataForImage( String datasetName, String imageName ) throws IOException {
        ImageSource imageSource = projectCreator.getDataset( datasetName ).sources.get( imageName ).get();
        if ( imageSource.imageData.containsKey( remoteImageDataFormat ) ) {

            if ( remoteImageDataFormat.hasXml() ) {
                // delete any existing remote metadata .xml files
                File currentRemoteXmlLocation = new File(FileAndUrlUtils.combinePath(projectCreator.getDataLocation().getAbsolutePath(),
                        datasetName, imageSource.imageData.get(remoteImageDataFormat).relativePath));
                if (currentRemoteXmlLocation.exists()) {
                    if (!currentRemoteXmlLocation.delete()) {
                        String errorMessage = "Remote metadata for: " + imageName + " in dataset: " + datasetName + " could not be deleted.";
                        IJ.log(errorMessage);
                        throw new IOException(errorMessage);
                    }
                }
            }
            imageSource.imageData.remove( remoteImageDataFormat );
        }
    }

    private String getRelativeKey( SpimData spimData, String datasetName, String imageName,
                                  ImageDataFormat imageFormat ) throws IOException {
        // check image is within the project folder (if people 'link' to bdv format images they may be outside)
        Path imagePath = Paths.get( ProjectCreatorHelper.getImageLocationFromSequenceDescription( spimData.getSequenceDescription(),
                imageFormat ).getAbsolutePath() ).normalize();
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

    private Element createImageLoaderXmlElement ( SpimData spimData,
                                                 ImageDataFormat imageFormat,
                                                 String datasetName, String imageName ) throws IOException {
        Element element = null;
        String key = getRelativeKey( spimData, datasetName, imageName, localImageDataFormat );
        switch ( imageFormat ) {
            case BdvN5S3:
                element = new XmlIoN5S3ImageLoader().toXml( serviceEndpoint, signingRegion, bucketName, key );
                break;
            case BdvOmeZarrS3:
                element = new XmlN5S3OmeZarrImageLoader().toXml( serviceEndpoint, signingRegion, bucketName, key );
                break;
        }

        return element;
    }

    private void saveXml( final SpimData spimData, String datasetName, String imagename, final String xmlFile, ImageDataFormat imageFormat ) throws SpimDataException, IOException {
        XmlIoSpimData io = new XmlIoSpimData();
        final File xmlFileDirectory = new File( xmlFile ).getParentFile();
        final Document doc = new Document( io.toXml( spimData, xmlFileDirectory ) );
        // remove default image loader, and replace with custom one
        Element imageLoaderElement = createImageLoaderXmlElement( spimData, imageFormat, datasetName, imagename );
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
        if ( !imageSource.imageData.containsKey( localImageDataFormat ) ) {
            IJ.log( "No images of format " + localImageDataFormat + " for " + imageName +
                    " in dataset:" + datasetName + ". Skipping this image." );
            return;
        }

        if ( localImageDataFormat.hasXml() ) {

            // make new xml containing bucket name etc, and give relative path
            String localXmlLocation = FileAndUrlUtils.combinePath(projectCreator.getDataLocation().getAbsolutePath(),
                    datasetName, imageSource.imageData.get(localImageDataFormat).relativePath);

            String remoteXmlLocation = FileAndUrlUtils.combinePath(projectCreator.getDataLocation().getAbsolutePath(),
                    datasetName, "images", ProjectCreatorHelper.imageFormatToFolderName( remoteImageDataFormat ));

            // make directory for that image file format, if doesn't exist already
            File remoteDir = new File( remoteXmlLocation );
            if ( !remoteDir.exists() ) {
                remoteDir.mkdirs();
            }

            AbstractSpimData spimData = new SpimDataOpener().openSpimData(localXmlLocation, localImageDataFormat);
            spimData.setBasePath(new File(remoteXmlLocation));
            saveXml( ( SpimData ) spimData, datasetName, imageName,
                    new File(remoteXmlLocation, imageName + ".xml").getAbsolutePath(),
                    remoteImageDataFormat);

            StorageLocation storageLocation = new StorageLocation();
            storageLocation.relativePath = "images/" + ProjectCreatorHelper.imageFormatToFolderName( remoteImageDataFormat ) + "/" + imageName + ".xml";
            imageSource.imageData.put( remoteImageDataFormat, storageLocation );
        } else {
            // give absolute s3 path to ome.zarr file
            StorageLocation storageLocation = new StorageLocation();
            String relativePath = imageSource.imageData.get(localImageDataFormat).relativePath;
            storageLocation.s3Address = serviceEndpoint + bucketName + "/" + datasetName + "/" + relativePath;
            imageSource.imageData.put( remoteImageDataFormat, storageLocation );
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

        IJ.log( "Adding metadata to project json." );
        projectCreator.getProjectJsonCreator().addImageDataFormat( remoteImageDataFormat );
        IJ.log( "Done." );
    }

    /**
     * Add remote metadata. Note this will overwrite any existing remote metadata for the given image format.
     * @param signingRegion signing region e.g. us-west-2
     * @param serviceEndpoint service endpoint e.g. https://s3.embl.de
     * @param bucketName bucket name
     * @param imageDataFormat image format
     */
    public void createRemoteMetadata( String signingRegion, String serviceEndpoint, String bucketName, ImageDataFormat imageDataFormat ) {

        if ( !imageDataFormat.isRemote() ) {
            IJ.log( "Creating remote metadata aborted - provided image data format is not remote." );
        }

        this.signingRegion = signingRegion;
        this.serviceEndpoint = serviceEndpoint;
        this.bucketName = bucketName;
        this.remoteImageDataFormat = imageDataFormat;

        if ( !this.serviceEndpoint.endsWith("/") ) {
            this.serviceEndpoint = this.serviceEndpoint + "/";
        }

        switch( remoteImageDataFormat ) {
            case BdvN5S3:
                localImageDataFormat = ImageDataFormat.BdvN5;
                break;
            case BdvOmeZarrS3:
                localImageDataFormat = ImageDataFormat.BdvOmeZarr;
                break;
            case OmeZarrS3:
                localImageDataFormat = ImageDataFormat.OmeZarr;
                break;
        }

        try {
            // clean any old remote metadata
            deleteAllRemoteMetadata();

            try {
               addAllRemoteMetadata();
            } catch (SpimDataException | IOException e) {
                IJ.log( "Error - aborting, and removing all " + remoteImageDataFormat + " remote metadata" );
                deleteAllRemoteMetadata();
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
