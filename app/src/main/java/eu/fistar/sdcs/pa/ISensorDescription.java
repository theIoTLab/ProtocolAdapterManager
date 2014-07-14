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
 * Interface implemented by classes defining sensors.<br>
 * A sensor definition is used to represent a sensor of a device and its characteristics.
 *
 * @author Marcello Morena
 * @author Alexandru Serbanati
 */
public interface ISensorDescription {

    /**
     * Returns the sensor name
     *
     * @return
     *      The sensor name
     */
    public String getSensorName();

    /**
     * Returns the unit of measure used by the sensor
     *
     * @return
     *      The unit of measure used by the sensor
     */
    public String getMeasurementUnit();

    /**
     * Returns the property name of the sensor
     *
     * @return
     *      The property name of the sensor
     */
    public String getPropertyName();
}
