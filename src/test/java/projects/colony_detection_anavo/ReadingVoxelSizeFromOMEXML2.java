package projects.colony_detection_anavo;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import java.io.File;

public class ReadingVoxelSizeFromOMEXML2
{
    public static void main( String[] args )
    {
        String filePath = "/Users/tischer/Desktop/moritz/MeasurementResult.ome.xml";

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

            System.out.println("PhysicalSizeX: " + physicalSizeX + " " + unit);
            System.out.println("PhysicalSizeY: " + physicalSizeY + " " + unit);
            System.out.println("PhysicalSizeZ: " + physicalSizeZ + " " + unit);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

