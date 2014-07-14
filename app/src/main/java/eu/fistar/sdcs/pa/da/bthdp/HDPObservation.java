package eu.fistar.sdcs.pa.da.bthdp;

import java.util.Arrays;
import java.util.List;

import eu.fistar.sdcs.pa.IObservation;

/**
 * This class represents an Observation, that is a a collection of the values observed for the
 * property, taken in a certain amount of time
 */
public class HDPObservation implements IObservation {

    private String propertyName;    // Property name (same as propertyName in ISensorDescription)
    private String measurementUnit; // Unit of measure of the property
    private List<String> values;    // Values observed for the property
    private long phenomenonTime;    // Timestamp associated with the measurement
    private long duration;          // Duration of the measurement [WARNING: for now it's always 0]

    public HDPObservation(HDPSensor mSensor, String[] mValues) {
        propertyName = mSensor.getPropertyName();
        measurementUnit = mSensor.getMeasurementUnit();
        phenomenonTime = System.currentTimeMillis();
        duration = 0;
        values = Arrays.asList(mValues);
    }

    @Override
    public String getPropertyName() {
        return propertyName;
    }

    @Override
    public String[] getValues() {
        return values.toArray(new String[values.size()]);
    }

    @Override
    public long getPhenomenonTime() {
        return phenomenonTime;
    }

    @Override
    public long getDuration() {
        return duration;
    }

    @Override
    public String getMeasurementUnit() {
        return measurementUnit;
    }

    @Override
    public void setProperty(String mPropertyName) {
        propertyName = mPropertyName;
    }

    @Override
    public void setDuration(long mDuration) {
        duration = mDuration;
    }

    @Override
    public void setMeasurementUnit(String mMeasurementUnit) {
        measurementUnit = mMeasurementUnit;
    }

    @Override
    public void setValues(String[] mValues) {
        values = Arrays.asList(mValues);
    }

    @Override
    public void setPhenomenonTime(long mPhenomenonTime) {
        phenomenonTime = mPhenomenonTime;
    }

    /**
     * Returns a read-friendly String representing the object
     *
     * @return
     *      The String representing the object
     */
    public String toString() {
        return "Property Name: "+propertyName+"\nMeasurement Unit: "+measurementUnit+"\nTime: "+
                phenomenonTime+"\nDuration: "+duration+"\nValue: "+values.get(0)+"\n";
    }
}
