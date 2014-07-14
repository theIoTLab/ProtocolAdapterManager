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

import java.util.List;

/**
 * Interface implemented by classes defining devices.<br>
 * A device definition is used to represent a device and all its properties, such as sensors.
 *<br>
 * Examples of Devices are<br>
 * - HDP Device<br>
 * - Smart BT Device<br>
 * - Proprietary Devices
 *
 * @author Marcello Morena
 * @author Alexandru Serbanati
 */
public interface IDeviceDescription {

    /**
     * Returns the device unique identifier
     *
     * @return
     *      The unique identifier for that device
     */
    public String getDeviceID();

    /**
     * Returns the model number of the device
     *
     * @return
     *      The model number of the device
     */
    public String getModelNumber();

    /**
     * Returns the model name of the device
     *
     * @return
     *      The model name of the device
     */
    public String getModelName();

    /**
     * Returns the manufacturer name of the device
     *
     * @return
     *      The manufacturer name of the device
     */
    public String getManufacturerName();

    /**
     * Returns the list of sensors belonging to the device
     *
     * @return
     *      The list of sensors belonging to the device
     */
    public List<ISensorDescription> getSensorList();

    /**
     * Returns the physical address of the device
     *
     * @return
     *      The physical address of the device
     */
    public String getAddress();

    /**
     * Used by Device Adapters to mark a device as registered or unregistered
     *
     * @param registered
     *      The boolean value used to mark the device as registered or unregistered
     */
    public void setRegistered(boolean registered);

    /**
     * Returns a boolean value indicating whether the device is registered in the Device Adapter or not
     *
     * @return
     *      The boolean value indicating whether the device is registered or not
     */
    public boolean isRegistered();
}
