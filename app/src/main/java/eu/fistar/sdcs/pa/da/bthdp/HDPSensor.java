package eu.fistar.sdcs.pa.da.bthdp;

import java.util.HashMap;
import java.util.Map;

import eu.fistar.sdcs.pa.ISensorDescription;

/**
 * This class represents a HDP Sensor and implements the Sensor Interface. It is used by the
 * Protocol Adapter to describe a sensor and the related property.
 */
public class HDPSensor implements ISensorDescription {

    private static final String UNKNOWN = "Unknown";

    private String sensorName;          // The sensor name (i.e. pulsimeter) taken from the hashmap using the ISO 11073 ID as the key
    private String measurementUnit;     // The measurement unit of the property, i.e. bpm
    private String propertyName;        // The name of the property (i.e. pulse)
    private String ieee11073ID;          // The numeric ID used in ISO 11073 taken from the hashmap using the ISO 11073 ID as the key

    private Map<String, String[]> propertyDb = new HashMap<String, String[]>();


    public HDPSensor(String mIeee11073ID) {

        createPropertyDb();

        String[] tmpNames = propertyDb.get(mIeee11073ID);

        sensorName = tmpNames!=null ? tmpNames[0] : UNKNOWN;
        propertyName = tmpNames!=null ? tmpNames[1] : UNKNOWN;
        measurementUnit = tmpNames!=null ? tmpNames[2] : UNKNOWN;
        ieee11073ID = mIeee11073ID;

    }

    /**
     * Since the only info contained in the HDP XML data is the metric ID of the property as
     * listed in the ISO 10073 nomenclature, we need to create a database of parameters associated
     * with the property
     */
    private void createPropertyDb() {
        // TODO Expand the HashMap with more codes from the ISO 11073 Nomenclature
        propertyDb.put("19384", new String[] {"oximeter", "oximetry", "%"});
        propertyDb.put("18458", new String[] {"pulsimeter", "pulse", "bpm"});
        propertyDb.put("18474", new String[] {"pulsimeter", "pulse", "bpm"});
        propertyDb.put("18949", new String[] {"blood pressure monitor", "systolic pressure", "mmHg"});
        propertyDb.put("18950", new String[] {"blood pressure monitor", "diastolic pressure", "mmHg"});
        propertyDb.put("18951", new String[] {"blood pressure monitor", "mean pressure", "mmHg"});
        propertyDb.put("64000", new String[] {UNKNOWN, UNKNOWN, UNKNOWN});
    }

    /**
     * Returns the name of the sensor
     *
     * @return
     *      The sensor name
     */
    @Override
    public String getSensorName() {
        return sensorName;
    }

    /**
     * Returns the unit of measure of the property
     *
     * @return
     *      The unit of measure of the property
     */
    @Override
    public String getMeasurementUnit() {
        return measurementUnit;
    }

    /**
     * Returns the name of the property
     *
     * @return
     *      The property name
     */
    @Override
    public String getPropertyName() {
        return propertyName;
    }

    /**
     * Returns the ID of the property as listed in the ISO 11073 nomenclature
     *
     * @return
     *      The ID of the property
     */
    public String getIEEE11073ID() {
        return ieee11073ID;
    }

    /**
     * Returns a read-friendly String representing the object
     *
     * @return
     *      The String representing the object
     */
    public String toString() {
        return "Sensor Name: "+sensorName+"\nProperty Name: "+propertyName+"\nMeasurement Unit: "+
                measurementUnit+"\nID 11073: "+ieee11073ID+"\n";
    }
}
