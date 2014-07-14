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

package eu.fistar.sdcs.pa.da.bthdp.hdpservice;

import eu.fistar.sdcs.pa.da.bthdp.hdpservice.HealthAgentAPI;

/**
 * This is the AIDL interface implemented by the ISO 11073 Manager and used by the client to
 * interact with this service

 * @author Marcello Morena
 * @author Alexandru Serbanati
 */
interface HealthServiceAPI {

    /**
     * Register an agent inside the ISO 11073 Manager
     *
     * @param agt
     *      The API endpoint of the agent
     *
     * @param specs
     *      An array of MDEP Data Type Codes that the ISO 11073 Manager have to listen for
     */
	void ConfigurePassive(HealthAgentAPI agt, in int[] specs);

    /**
     * Returns the configuration of the device passed by argument
     *
     * @param dev
     *      The device to use in the operation
     */
	String GetConfiguration(String dev);

    /**
     * Request the retrieval of the device configuration
     *
     * @param dev
     *      The device to use in the operation
     */
	void RequestDeviceAttributes(String dev);

    /**
     * Unconfigure an agent inside the ISO 11073 Manager
     *
     * @param agt
     *      The API endpoint of the agent
     */
	void Unconfigure(HealthAgentAPI agt);
}
