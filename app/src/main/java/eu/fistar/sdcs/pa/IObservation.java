/**
 * Copyright (C) 2014 Consorzio Roma Ricerche
 * All rights reserved
 *
 * This file is part of the Protocol Adapter software, available at
 * https://github.com/theIoTLab/ProtocolAdapter .
 *
 * The Protocol Adapter is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://opensource.org/licenses/LGPL-3.0
 *
 * Contact Consorzio Roma Ricerche (protocoladapter@gmail.com)
 */

package eu.fistar.sdcs.pa;

/**
 * Interface implemented by classes defining observations.<br>
 * An observation is used to encapsulate one or more measurements coming from a sensor
 * and carrying some meta data together with the measurements.
 *
 * @author Marcello Morena
 * @author Alexandru Serbanati
 */
public interface IObservation {

    /**
     * Returns the property name (same as propertyName in ISensorDescription)
     *
     * @return
     *      The property name
     */
    public String getPropertyName();

    /**
     * Returns the values observed for the property
     *
     * @return
     *      The values observed for the property
     */
    public String[] getValues();

    /**
     * Returns the timestamp associated with the measurement
     *
     * @return
     *      The timestamp associated with the measurement
     */
    public long getPhenomenonTime();

    /**
     * Returns the duration of the measurement, 0 for now
     *
     * @return
     *      The duration of the measurement
     */
    public long getDuration();

    /**
     * Returns the unit of measure of the property
     *
     * @return
     *      The unit of measure of the property
     */
    public String getMeasurementUnit();

    public void setProperty(String mPropertyName);
    public void setDuration(long mDuration);
    public void setMeasurementUnit(String mMeasurementUnit);
    public void setValues(String[] mValues);
    public void setPhenomenonTime(long mPhenomenonTime);

}
