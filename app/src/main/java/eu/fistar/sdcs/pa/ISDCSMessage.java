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

import org.json.JSONObject;

/**
 * Interface implemented by classes defining the Messages to send to the SDCS.
 *
 * @author Marcello Morena
 * @author Alexandru Serbanati
 */
public interface ISDCSMessage {

    /**
     * Sets the method to post data to the SDCS
     *
     * @param method
     *      The method used to post data to the SDCS
     */
    public void setPOSTMethod(String method);

    /**
     * Sets the resource path used inside the SDCS
     *
     * @param path
     *      The resource pat used inside the SDCS
     */
    public void setPath(String path);

    /**
     * Sets the reply action that will be used by the SDCS when sending an intent
     *
     * @param replyAction
     *      The reply action used by the SDCS
     */
    public void setReplyAction(String replyAction);

    /**
     * Adds an @Observation to the message to send to SDSC
     *
     * @param observation
     *      The observation to add to the message
     */
    public void addObservation(IObservation observation);

    /**
     * Sets the content of the message in JSON format
     *
     * @param cont
     *      The content of the message
     */
    public void setJSONContent(JSONObject cont);

    /**
     * Sends the message to the SDCS as an Intent specifying a message ID
     *
     * @param messageID
     *      The message ID to use
     */
    public void send(int messageID);

    /**
     * Returns the message ID of the message
     *
     * @return
     *      The message ID
     */
    public int getMessageID();

    /**
     * Returns the timestamp associated with the message
     *
     * @return
     *      The timestamp associated with the message
     */
    public long getTimestamp();

}
