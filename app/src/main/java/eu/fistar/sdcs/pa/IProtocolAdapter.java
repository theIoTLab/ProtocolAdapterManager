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

import android.os.Binder;

/**
 * Interface implemented by classes that implements a Protocol Adapter.<br>
 * This interface is used to define some methods used by Device Adapters to communicate with the PA.
 * @author Marcello Morena
 * @author Alexandru Serbanati
 */
public interface IProtocolAdapter {

    /**
     * Register a new device with the protocol adapter
     *
     * @param dev_desc
     *      The device to register
     */
    public void register_Device(IDeviceDescription dev_desc);

    /**
     * Push new measurements data coming from the device
     *
     * @param observations
     *      The data to push
     *
     * @param dev_desc
     *      The device who supplies the data
     */
    public void push_Data(IObservation[] observations, IDeviceDescription dev_desc);

    /**
     * Deregister a device with the protocol adapter when it is not available anymore
     *
     * @param dev_desc
     *      The device to deregister
     */
    public void deregister_Device(IDeviceDescription dev_desc);

    /**
     * Register a new property for a device
     * Not used at the moment, since all the job is done with register_Device
     *
     * @param dev_desc
     *      The device that has the property to register
     */
    @Deprecated
    public void register_Device_Properties(IDeviceDescription dev_desc);

    /**
     * This class extends the Binder class and is required in order to facilitate the
     * communication with Device Adapters
     */
    public abstract class PABinder extends Binder {

        /**
         * Used by Device Adapters to get the instance of the Protocol Adapter
         *
         * @return
         *      The instance of the Protocol Adapter associated with this Binder
         */
        public abstract IProtocolAdapter getProtocolAdapter();
    }
}
