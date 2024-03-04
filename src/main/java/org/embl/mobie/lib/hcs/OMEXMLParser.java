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
    public static VoxelDimensions readVoxelDimensions( String filePath )
    {
        try {
            File inputFile = new File(filePath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
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
