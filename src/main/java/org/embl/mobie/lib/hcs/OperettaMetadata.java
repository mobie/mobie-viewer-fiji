package org.embl.mobie.lib.hcs;

import ch.epfl.biop.bdv.img.opener.ChannelProperties;
import mpicbg.spim.data.sequence.FinalVoxelDimensions;
import mpicbg.spim.data.sequence.VoxelDimensions;
import org.embl.mobie.lib.color.ColorHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;


// one could extract an interface here for the
// getter methods if this is useful for other data
public class OperettaMetadata
{
	private HashMap< String, Element > filenameToImageElement;
	private HashMap< String, Element > channelIDToElement;
	private HashMap< String, Integer > filenameToImageIndex;
	private double dx;
	private double dy;
	private String spatialUnit;
	private int imageSizeX;
	private int imageSizeY;
	private int maxIntensity;

	public OperettaMetadata( File xml )
	{
		tryParse( xml );
	}

	private void tryParse( File xml )
	{
		try
		{
			parse( xml );
		}
		catch ( Exception e )
		{
			e.printStackTrace();
			throw new RuntimeException( e );
		}
	}

	private void parse( File xml ) throws ParserConfigurationException, SAXException, IOException
	{
		DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document doc = builder.parse( xml );
		doc.getDocumentElement().normalize();

		dx = Double.parseDouble( doc.getElementsByTagName( "ImageResolutionX" ).item( 0 ).getTextContent() );
		dy = Double.parseDouble( doc.getElementsByTagName( "ImageResolutionY" ).item( 0 ).getTextContent() );
		spatialUnit = doc.getElementsByTagName( "ImageResolutionX" ).item( 0 ).getAttributes().item( 0 ).getTextContent();

		// could be channel specific
		//
		imageSizeX = Integer.parseInt( doc.getElementsByTagName( "ImageSizeX" ).item( 0 ).getTextContent() );
		imageSizeY = Integer.parseInt( doc.getElementsByTagName( "ImageSizeY" ).item( 0 ).getTextContent() );

		// could be channel specific
		//
		maxIntensity = Integer.parseInt( doc.getElementsByTagName( "MaxIntensity" ).item( 0 ).getTextContent() );

		filenameToImageElement = new LinkedHashMap<>();
		filenameToImageIndex = new LinkedHashMap<>();
		channelIDToElement = new LinkedHashMap<>();

		final NodeList imageFileNames = doc.getElementsByTagName( "URL" );
		final int numImages = imageFileNames.getLength();
		for ( int imageIndex = 0; imageIndex < numImages; imageIndex++ )
		{
			final Node item = imageFileNames.item( imageIndex );
			final Element parentNode = (Element) item.getParentNode();
			filenameToImageElement.put( item.getTextContent(), parentNode );
			filenameToImageIndex.put( item.getTextContent(), imageIndex );
		}

		final NodeList channelIDs = doc.getElementsByTagName( "MaxIntensity" );
		final int numChannels = channelIDs.getLength();
		for ( int channelIndex = 0; channelIndex < numChannels; channelIndex++ )
		{
			final Node item = channelIDs.item( channelIndex );
			final Element parentNode = (Element) item.getParentNode();
			final String channelID = parentNode.getAttributes().item( 0 ).getTextContent();
			channelIDToElement.put( channelID, parentNode );
		}
	}

	public VoxelDimensions getVoxelDimensions( String path )
	{
		// In Operetta 4 and 5 this is not consistently at the same position
		// thus we just fetch it once globally. Hopefully it is the same for all
		// images anyway.

		// This only works in Operetta 4
		//		final Element element = getElement( path );
		//		final double imageResolutionX = getDouble( element, "ImageResolutionX" );
		//		final double imageResolutionY = getDouble( element, "ImageResolutionY" );
		//		final String unit = element.getElementsByTagName( "ImageResolutionX" ).item( 0 ).getAttributes().item( 0 ).getTextContent();

		return new FinalVoxelDimensions( spatialUnit, dx, dy, 1.0 );
	}

	private double getDouble( Element element, String tag )
	{
		try
		{
			return Double.parseDouble( element.getElementsByTagName( tag ).item( 0 ).getTextContent() );
		}
		catch ( Exception e )
		{
			e.printStackTrace();
			throw new RuntimeException( e );
		}
	}

	private int getInteger( Element element, String tag )
	{
		try
		{
			return Integer.parseInt( element.getElementsByTagName( tag ).item( 0 ).getTextContent() );
		}
		catch ( Exception e )
		{
			e.printStackTrace();
			throw new RuntimeException( e );
		}
	}

	private String getString( Element element, String tag )
	{
		try
		{
			return element.getElementsByTagName( tag ).item( 0 ).getTextContent();
		}
		catch ( Exception e )
		{
			e.printStackTrace();
			throw new RuntimeException( e );
		}
	}

	private Element getImageElement( String path )
	{
		final String filename = new File( path ).getName();
		final Element element = filenameToImageElement.get( filename );
		if ( element == null )
		{
			System.err.println("Could not find operetta metadata for " + filename );
			throw new RuntimeException();
		}
		return element;
	}

	public boolean contains( String path )
	{
		final String filename = new File( path ).getName();
		return filenameToImageElement.containsKey( filename );
	}

	public double[] getRealPosition( String path )
	{
		final Element imageElement = getImageElement( path );
		return new double[]{
				getDouble( imageElement, "PositionX" ),
				-getDouble( imageElement, "PositionY" )
		  };
	}

	public String getColor( String path )
	{
		final Element imageElement = getImageElement( path );
		final String channelID = getString( imageElement, "ChannelID" );
		final Element channelElement = channelIDToElement.get( channelID );
		final int mainEmissionWavelength = getInteger( channelElement, "MainEmissionWavelength" );

		final Color color = ChannelProperties.getColorFromWavelength( mainEmissionWavelength );
		final String string = ColorHelper.getString( ColorHelper.getARGBType( color ) );
		return string;
	}

	public int getImageIndex( String path )
	{
		return filenameToImageIndex.get( new File( path ).getName() );
	}

	public double[] getContrastLimits( String path )
	{
		// TODO: fetch per channel via channelID of image
		return new double[]{ 0, maxIntensity };
	}

	public int[] getSiteDimensions( String path )
	{
		// TODO: fetch per channel via channelID of image
		return new int[]{ imageSizeX, imageSizeY };
	}
}
