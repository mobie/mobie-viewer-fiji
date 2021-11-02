package org.embl.mobie.viewer.bdv;

import bdv.spimdata.SequenceDescriptionMinimal;
import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.legacy.XmlIoSpimDataMinimalLegacy;
import bdv.util.volatiles.SharedQueue;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.XmlHelpers;
import mpicbg.spim.data.registration.ViewRegistration;
import mpicbg.spim.data.registration.ViewRegistrations;
import mpicbg.spim.data.sequence.*;
import net.imglib2.Dimensions;
import net.imglib2.FinalDimensions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Cast;
import org.embl.mobie.io.util.openers.S3Opener;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class N5S3Opener extends S3Opener
{
    public static final String SERVICE_ENDPOINT = "ServiceEndpoint";
    public static final String SIGNING_REGION = "SigningRegion";
    public static final String BUCKET_NAME = "BucketName";
    public static final String KEY = "Key";
    public N5S3Opener(String url) {
        super(url);
    }

    public static SpimData readURL(String url, SharedQueue sharedQueue) throws IOException {
        final N5S3Opener reader = new N5S3Opener(url);
        return reader.readKey(url, sharedQueue);
    }

    public SpimData readKey(String url, SharedQueue sharedQueue) throws IOException {
        final SAXBuilder sax = new SAXBuilder();
        Document doc;
        try {
            doc = sax.build( url );
            final Element root = doc.getRootElement();
            final Element ell = root.getChild( "SequenceDescription" );
            final Element elem = ell.getChild( "ImageLoader" );
            final String serviceEndpoint = XmlHelpers.getText(elem, SERVICE_ENDPOINT);
            final String signingRegion = XmlHelpers.getText(elem, SIGNING_REGION);
            final String bucketName = XmlHelpers.getText(elem, BUCKET_NAME);
            final String key = XmlHelpers.getText(elem, KEY);
            final TimePoints timepoints = createTimepointsFromXml( root.getChild("SequenceDescription"  ) );
            final Map< Integer, ViewSetup> setups = createViewSetupsFromXml( root.getChild( "SequenceDescription" ) );
            final MissingViews missingViews = null;
            final Element viewRegistrations = root.getChild( "ViewRegistrations" );
            final ArrayList< ViewRegistration > regs = new ArrayList<>();
            for ( final Element vr : viewRegistrations.getChildren( "ViewRegistration" ) )
            {
                final int timepointId = Integer.parseInt( vr.getAttributeValue( "timepoint" ) );
                final int setupId = Integer.parseInt( vr.getAttributeValue( "setup" ));
                final AffineTransform3D transform = new AffineTransform3D();
                transform.set(XmlHelpers.getDoubleArray(vr.getChild( "ViewTransform" ), "affine"));
                regs.add( new ViewRegistration( timepointId, setupId, transform ) );
            }
            SequenceDescription sequenceDescription =  new SequenceDescription( timepoints, setups, null, missingViews );
            N5S3ImageLoader imageLoader = new N5S3ImageLoader(serviceEndpoint, signingRegion, bucketName, key, sequenceDescription, sharedQueue);
            sequenceDescription.setImgLoader( imageLoader );
            imageLoader.viewRegistrations = new ViewRegistrations( regs );
            imageLoader.seq = sequenceDescription;
            return new SpimData(null, Cast.unchecked(imageLoader.getSequenceDescription()), imageLoader.getViewRegistrations());
        } catch ( JDOMException e ) {
            e.printStackTrace();
        }
        return null;
    }

    private static TimePoints createTimepointsFromXml( final Element sequenceDescription )
    {
        final Element timepoints = sequenceDescription.getChild( "Timepoints" );
        final String type = timepoints.getAttributeValue( "type" );
        if ( type.equals( "range" ) )
        {
            final int first = Integer.parseInt( timepoints.getChildText( "first" ) );
            final int last = Integer.parseInt( timepoints.getChildText( "last" ) );
            final ArrayList<TimePoint> tps = new ArrayList<>();
            for ( int i = first, t = 0; i <= last; ++i, ++t )
                tps.add( new TimePoint( t ) );
            return new TimePoints( tps );
        }
        else
        {
            throw new RuntimeException( "unknown <Timepoints> type: " + type );
        }
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
}
