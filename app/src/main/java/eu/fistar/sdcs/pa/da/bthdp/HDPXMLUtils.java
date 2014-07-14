package eu.fistar.sdcs.pa.da.bthdp;

import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import eu.fistar.sdcs.pa.ISensorDescription;

/**
 * This class contains some static methods to handle XML data. These are used to extract information
 * from raw XML data returned by @HDPHealthManagerService
 */
public class HDPXMLUtils {

    // XML Related Constants
    private final static String NODE_NAME_SIMPLE = "simple";
    private final static String NODE_NAME_NAME = "name";
    private final static String NODE_NAME_VALUE = "value";
    private static final String NODE_NAME_META = "meta";
    private final static String ELEMENT_MANUFACTURER_NAME = "manufacturer";
    private final static String ELEMENT_MODEL_NUMBER = "model-number";
    private final static String ELEMENT_SPECIALIZATION = "Dev-Configuration-Id";
    private final static String ELEMENT_SYSTEM_ID = "System-Id";
    private final static String ELEMENT_CODE = "code";
    private final static String ATTRIBUTE_NAME_NAME = "name";
    private final static String ATTRIBUTE_VALUE_METRIC_ID ="metric-id";


    /**
     * Extract all the measurements and create an HDPObservation for everyone of them
     *
     * @param hdpDev
     *      The HDPDevice to update with the attributes
     *
     * @param measurementXmlData
     *      The raw XML data returned by antidote
     *
     * @return
     *      The array of HDPObservations
     */
    public static HDPObservation[] parseMeasurementData(HDPDevice hdpDev, String measurementXmlData) {

        // Create the Document
        Document d = createDocument(measurementXmlData);

        // Create the List of HDPObservation
        List<HDPObservation> observations = new ArrayList<HDPObservation>();

        // Retrieve the list of sensors from the device description
        List<ISensorDescription> sensors = hdpDev.getSensorList();

        // Search for all available sensor
        for (ISensorDescription tmpSens : sensors) {

            // Retrieve the value of measurement for the specified sensor
            String ieee11073Id = ((HDPSensor) tmpSens).getIEEE11073ID();
            String tmpValue = getMeasurementInfo(d, ieee11073Id);
            String[] tmpValues = new String[] {tmpValue};

            // Create a new Observation
            HDPObservation tmpObs = new HDPObservation((HDPSensor) tmpSens, tmpValues);

            // Add the Observation to the List
            observations.add(tmpObs);
        }

        // Return the Observations List
        return observations.toArray(new HDPObservation[observations.size()]);
    }

    /**
     * Extract all the interesting information from the Attributes to better define the device
     *
     * @param hdpDev
     *      The HDPDevice to update with the attributes
     *
     * @param attrXmlData
     *      The raw XML data returned by antidote
     */
    public static void parseAttrData(HDPDevice hdpDev, String attrXmlData) {

        String deviceId;
        String modelNumber = ""; // Not really used
        String modelName;
        String manufacturerName;
        String ieeeDevSpecId;

        // Create the Document
        Document d = createDocument(attrXmlData);

        // Retrieve all the Attributes
        deviceId = getAttrInfo(d, ELEMENT_SYSTEM_ID);
        modelName = getAttrInfo(d, ELEMENT_MODEL_NUMBER);
        manufacturerName = getAttrInfo(d, ELEMENT_MANUFACTURER_NAME);
        ieeeDevSpecId = getAttrInfo(d, ELEMENT_SPECIALIZATION);

        // Set the Attributes into the device
        hdpDev.setAttributes(deviceId, modelNumber, modelName, manufacturerName, ieeeDevSpecId);

    }

    /**
     * Extract all the configuration data from the Configuration and update the device
     *
     * @param hdpDev
     *      The HDPDevice to update with configuration info
     *
     * @param confXmlData
     *      The raw XML data returned by antidote
     */
    public static void parseConfData(HDPDevice hdpDev, String confXmlData) {

        // Create the Document
        Document d = createDocument(confXmlData);

        // Retrieve all properties
        List<String> properties = getConfInfo(d);

        // Insert all properties inside the device description
        for (String tmpProp : properties) {

            // TODO Remove this workaround added to handle the A&D Compound Property
            if ("18948".equals(tmpProp)) {
                hdpDev.addProperty("18949");
                hdpDev.addProperty("18950");
                hdpDev.addProperty("18951");
                break;
            }

            // Add the property to the device description
            hdpDev.addProperty(tmpProp);
        }
    }

    /**
     * Create and normalize a Document starting from raw XML data
     *
     * @param xmlData
     *      The raw XML data returned by antidote
     *
     * @return
     *      The Document created from the raw XML
     */
    private static Document createDocument(String xmlData) {
        Document d = null;

        // Create a new Document using the XML String
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            d = db.parse(new ByteArrayInputStream(xmlData.getBytes("UTF-8")));
        } catch (ParserConfigurationException e) {
        } catch (SAXException e) {
        } catch (IOException e) {
        }

        // Normalize the document
        d.getDocumentElement().normalize();

        // Return the Document
        return d;

    }

    /**
     * Extract the measurement value related to the metric ID passed as argument
     *
     * @param d
     *      The Document generated from raw XML data
     *
     * @param ieee11073Id
     *      The metric ID according to the ISO 11073 nomenclature
     *
     * @return
     *      The value of the measurament
     */
    private static String getMeasurementInfo(Document d, String ieee11073Id) {

        // Get all the "meta" Nodes
        NodeList nl = d.getElementsByTagName(NODE_NAME_META);

        for (int i = 0; i < nl.getLength(); i++) {

            Node n = nl.item(i);

            if (n.getNodeType() == Node.ELEMENT_NODE) {

                // Find the "meta" Node with attribute "name"="metric-id" and value ieee11073Id
                Element e = (Element) n;
                String attributeValue = e.getAttribute(ATTRIBUTE_NAME_NAME);
                String nodeValue = e.getTextContent();

                if (ATTRIBUTE_VALUE_METRIC_ID.equals(attributeValue) && ieee11073Id.equals(nodeValue)) {
                    try {
                        // Get the parent Node of the parent Node
                        Element parent = (Element) n.getParentNode().getParentNode();

                        // Return the value of the "value" Node
                        return parent.getElementsByTagName(NODE_NAME_VALUE).item(0).getTextContent();
                    } catch (NullPointerException exc) {
                        continue;
                    }

                }
            }
        }

        return null;
    }

    /**
     * Extract the value of the attribute of the specified type
     *
     * @param d
     *      The Document generated from raw XML data
     *
     * @param type
     *      The type of attribute
     *
     * @return
     *      The value of the attribute
     */
    private static String getAttrInfo(Document d, String type) {

        // Get all the "simple" Nodes
        NodeList nl = d.getElementsByTagName(NODE_NAME_SIMPLE);

        // Find the right node
        for (int i = 0; i < nl.getLength(); i++) {

            Node n = nl.item(i);

            if (n.getNodeType() == Node.ELEMENT_NODE) {
                Element e = (Element) n;

                // The right node has the right type inside the "name" sub-Node
                String name = e.getElementsByTagName(NODE_NAME_NAME).item(0).getTextContent();
                String value = e.getElementsByTagName(NODE_NAME_VALUE).item(0).getTextContent();
                if (type != null && type.equals(name) && value != null) {
                    return value;
                }
            }
        }

        return "Unknown";
    }

    /**
     * Extract all the IDs of the measurements offered by a device
     *
     * @param d
     *      The Document generated from raw XML data
     *
     * @return
     *      The List of all the measurements IDs
     */
    private static List<String> getConfInfo(Document d) {

        ArrayList<String> measurements = new ArrayList<String>();

        // Get all the "entry" Nodes: there's one of them for every measurement
        NodeList nl = d.getDocumentElement().getChildNodes();

        // Visit all the "entry" node, one by one
        for (int i = 0; i < nl.getLength(); i++) {

            Node n = nl.item(i);

            if (n.getNodeType() == Node.ELEMENT_NODE) {

                // Get all the "simple" nodes
                NodeList nlInt = ((Element) n).getElementsByTagName(NODE_NAME_SIMPLE);

                // Find the right "simple" Node, the one that contains the measurement ID
                for (int j = 0; j < nlInt.getLength(); j++) {

                    Element e = (Element) nlInt.item(j);

                    // The right node has the right type inside the "name" sub-Node
                    String name = e.getElementsByTagName(NODE_NAME_NAME).item(0).getTextContent();
                    String value = e.getElementsByTagName(NODE_NAME_VALUE).item(0).getTextContent();
                    if (ELEMENT_CODE.equals(name) && value != null) {
                        measurements.add(value);
                        break;
                    }
                }
            }
        }

        // return all the measurements found
        return measurements;
    }

}
