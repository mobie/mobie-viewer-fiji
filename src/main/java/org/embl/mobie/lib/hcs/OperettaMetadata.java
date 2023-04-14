package org.embl.mobie.lib.hcs;

import ij.gui.PointRoi;
import ij.gui.Roi;
import mpicbg.spim.data.sequence.FinalVoxelDimensions;
import mpicbg.spim.data.sequence.VoxelDimensions;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;


// one could extract an interface here for the
// getter methods if this is useful for other data
public class OperettaMetadata
{
	private HashMap< String, Element > filenameToMetadata;
	private HashMap< String, Integer > filenameToIndex;
	private double dx;
	private double dy;
	private String spatialUnit;

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

		filenameToMetadata = new LinkedHashMap<>();
		filenameToIndex = new LinkedHashMap<>();

		final NodeList imageFileNames = doc.getElementsByTagName( "URL" );
		final int numImages = imageFileNames.getLength();
		for ( int imageIndex = 0; imageIndex < numImages; imageIndex++ )
		{
			final Node item = imageFileNames.item( imageIndex );
			final Element parentNode = (Element) item.getParentNode();
			filenameToMetadata.put( item.getTextContent(), parentNode );
			filenameToIndex.put( item.getTextContent(), imageIndex );
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

	private Element getElement( String path )
	{
		final String filename = new File( path ).getName();
		final Element element = filenameToMetadata.get( filename );
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
		return filenameToMetadata.containsKey( filename );
	}

	public double[] getRealPosition( String path )
	{
		final Element element = getElement( path );
		return new double[]{
				getDouble( element, "PositionX" ),
				-getDouble( element, "PositionY" )
		  };
	}

	public int getImageIndex( String path )
	{
		return filenameToIndex.get( new File( path ).getName() );
	}
}
