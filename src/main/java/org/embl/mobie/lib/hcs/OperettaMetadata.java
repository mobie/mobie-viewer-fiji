package org.embl.mobie.lib.hcs;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;

public class OperettaMetadata
{
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

		final double dx = Double.parseDouble( doc.getElementsByTagName( "ImageResolutionX" ).item( 0 ).getTextContent() );
		final double dy = Double.parseDouble( doc.getElementsByTagName( "ImageResolutionY" ).item( 0 ).getTextContent() );
		final String unit = doc.getElementsByTagName( "ImageResolutionX" ).item( 0 ).getAttributes().item( 0 ).getTextContent();

		// TODO Build a HashMap< FileName, Element >
		final NodeList fileNames = doc.getElementsByTagName( "URL" );
		final int numFiles = fileNames.getLength();
		for ( int i = 0; i < numFiles; i++ )
		{
			final Node item = fileNames.item( i );
			System.out.println( item.getTextContent() );
			final Element parentNode = (Element) item.getParentNode();
			parentNode.getElementsByTagName( "PositionX" ).item( 0 ).getTextContent();
		}
	}

}
