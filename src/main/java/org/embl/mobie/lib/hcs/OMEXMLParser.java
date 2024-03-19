/*-
 * #%L
 * Fiji viewer for MoBIE projects
 * %%
 * Copyright (C) 2018 - 2024 EMBL
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

import ij.IJ;
import mpicbg.spim.data.sequence.FinalVoxelDimensions;
import mpicbg.spim.data.sequence.VoxelDimensions;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;

public class OMEXMLParser
{
    public static VoxelDimensions readVoxelDimensions( File omeXml )
    {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(omeXml);
            doc.getDocumentElement().normalize();

            // Assuming VoxelDimensions is a class you have that can store these values
            String physicalSizeX = "1.0"; // Default value
            String physicalSizeY = "1.0"; // Default value
            String physicalSizeZ = "1.0"; // Default value
            String unit = "µm"; // Default unit

            NodeList pixelsList = doc.getElementsByTagName("Pixels");

            for (int temp = 0; temp < pixelsList.getLength(); temp++) {
                Node nNode = pixelsList.item(temp);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    if (eElement.hasAttribute("PhysicalSizeX")) {
                        physicalSizeX = eElement.getAttribute("PhysicalSizeX");
                        if (eElement.hasAttribute("PhysicalSizeY")) {
                            physicalSizeY = eElement.getAttribute("PhysicalSizeY");
                        }
                        if (eElement.hasAttribute("PhysicalSizeZ")) {
                            physicalSizeZ = eElement.getAttribute("PhysicalSizeZ");
                        }
                        if (eElement.hasAttribute("PhysicalSizeXUnit")) {
                            unit = eElement.getAttribute("PhysicalSizeXUnit");
                        }
                        break; // Found the first instance with PhysicalSizeX, break the loop
                    }
                }
            }

            IJ.log( "PhysicalSizeX: " + physicalSizeX + " " + unit );
            IJ.log("PhysicalSizeY: " + physicalSizeY + " " + unit);
            IJ.log("PhysicalSizeZ: " + physicalSizeZ + " " + unit);

            return new FinalVoxelDimensions(
                    unit,
                    Double.parseDouble( physicalSizeX ),
                    Double.parseDouble( physicalSizeY ),
                    Double.parseDouble( physicalSizeZ )
                );
        }
        catch (Exception e)
        {
            throw new RuntimeException( e );
        }
    }
}
