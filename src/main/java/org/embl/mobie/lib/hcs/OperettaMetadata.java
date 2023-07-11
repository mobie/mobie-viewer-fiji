/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2023 EMBL
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
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


// one could extract an interface here for the
// getter methods if this is useful for other data
public class OperettaMetadata
{
	private HashMap< String, Element > filenameToMetadata;

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

		filenameToMetadata = new HashMap<>();
		final NodeList fileNames = doc.getElementsByTagName( "URL" );
		final int numFiles = fileNames.getLength();
		for ( int i = 0; i < numFiles; i++ )
		{
			final Node item = fileNames.item( i );
			final Element parentNode = (Element) item.getParentNode();
			filenameToMetadata.put( item.getTextContent(), parentNode );
		}
	}

	public VoxelDimensions getVoxelDimensions( String path )
	{
		final Element element = getElement( path );
		final double imageResolutionX = getDouble( element, "ImageResolutionX" );
		final double imageResolutionY = getDouble( element, "ImageResolutionY" );
		final String unit = element.getElementsByTagName( "ImageResolutionX" ).item( 0 ).getAttributes().item( 0 ).getTextContent();

		return new FinalVoxelDimensions( unit, imageResolutionX, imageResolutionY, 1.0 );
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
}
