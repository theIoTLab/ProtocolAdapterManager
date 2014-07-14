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

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import eu.fistar.sdcs.pa.PAAndroidConstants.SDCS_MESSAGES;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;


/**
 * This class represents the message to be sent to the SDCS.
 *
 * @author Marcello Morena
 * @author Alexandru Serbanati
 */
public class SDCSIntentMessage extends Intent implements ISDCSMessage {


    private static final String LOGTAG = "INTENT >>>";

    private final String deviceName;

    private final short type;
    private final Context baseContext;
    private int messID = 0;

    private boolean isSetContentType = false;
    private long timestamp;


    public static class SUPPORTED_CONTENT_TYPES {
        public static String APPLICATION_JSON = "application/json";
    }

    /**
     * Perform the initialization of the Intent setting the right Action, Package, Issuer, Reply
     * Action and other properties depending on the message type
     *
     * @param context
     *      The context of the application creating the message
     *
     * @param messageType
     *      The type of the message
     *
     * @param basePath
     *      The base path for applications
     */
    private void init(Context context, short messageType, String basePath) {

        // Set the action to the SDCS action
        this.setAction(PAAndroidConstants.SDCS.ACTION);

        // Set the package to the SDCS one
        this.setPackage(PAAndroidConstants.SDCS.PACKAGE);

        // Set the name of the package that is sending the message
        this.putExtra(SDCS_MESSAGES.EXTRA_NAME_ISSUER, PAAndroidConstants.PACKAGE);

        // Set the reply action to let the SDCS know who reply to
        setReplyAction(PAAndroidConstants.PACKAGE);

        // Check the type of the message and perform the right actions
        switch (messageType) {
            case (SDCS_MESSAGES.MSG_TYPE_DEV_REGISTRATION):
                setPOSTMethod("create");
                setPath(basePath);
                break;
            case (SDCS_MESSAGES.MSG_TYPE_DEV_PROPERTIES_REGISTRATION):
                setPOSTMethod("create");
                break;
            case (SDCS_MESSAGES.MSG_TYPE_DATA_PUSH):
                setPOSTMethod("create");
                //This must be followed by a call to setPath and to addObservation to properly set
                // the message up
                break;
            case (SDCS_MESSAGES.MSG_TYPE_DEV_DEREGISTRATION):
                setPOSTMethod("delete");
                break;
            default:
                throw new IllegalArgumentException("SDCSIntentMessage: Wrong message type. Device Adapter Description not expected in SDCSMessages of type " + messageType + ".");
        }

    }

    /**
     * Create a new SDCSIntentMessage specifying the context, the message type and the sender of
     * the message
     *
     * @param context
     *      The context of the application creating the message
     *
     * @param messageType
     *      The type of the message
     *
     * @param source_device
     *      The device that creates the message
     *
     * @param basePath
     *      The base path for applications
     */
    public SDCSIntentMessage(Context context, short messageType, String source_device, String basePath) {
        super();

        type = messageType;
        baseContext = context;
        deviceName = source_device;

        // Perform the initialization of the new message
        init(baseContext, type, basePath);

    }


    /**
     * Add to the message all the information related to the measurements
     *
     * @param observation
     *      The object containing the information related to the measurements
     */
    public void addObservation(IObservation observation) {

        try {
            String[] values = observation.getValues();
            int len = values.length;
            JSONObject measurement = new JSONObject();
            String val;

            if (len > 1) {

                // If there are multiple measurements, put them inside a JSONArray
                JSONArray valArray = new JSONArray();

                for (int j = 0; j < len; j++) {
                    valArray.put(values[j]);
                }

                val = valArray.toString();

            } else if (len == 1) {

                // If there is only one value just set it
                val = values[0];
            } else {
                throw new IllegalArgumentException("SDCSIntentMessage: measurement with no values found for property \"" + observation.getPropertyName() + "\".");
            }

            // Put the measurements inside the JSONObject
            measurement.put(observation.getPropertyName(), val);

            // Set the JSONObject as the content of the message
            setJSONContent(measurement);


        } catch (JSONException e) {
            throw new IllegalArgumentException("JSON error on pushing the data for the PA: ", e);
        }
    }

    /**
     * Set the post method for the message
     *
     * @param method
     *      The post method for the message
     */
    public void setPOSTMethod(String method) {
        this.putExtra(SDCS_MESSAGES.EXTRA_NAME_METHOD, method);
    }

    /**
     * Set the path for the message
     *
     * @param path
     *      The path for the message
     */
    public void setPath(String path) {
        this.putExtra(SDCS_MESSAGES.EXTRA_NAME_PATH, path);
    }

    /**
     * Set the reply action field of the Intent, so the receiver knows who send the reply to
     *
     * @param replyAction
     *      The entity to send the reply to (usually the sender)
     */
    public void setReplyAction(String replyAction) {
        this.putExtra(SDCS_MESSAGES.EXTRA_NAME_REPLY_ACTION, replyAction);
    }

    /**
     * Set the content of this intent to the supplied parameter, taking care of appending
     * the content passed as input to the content that is already present. In this case check if
     * <code>content-type</code> match. If <code>content-type</code> is not set, then set it to
     * <code>application/json</code>.
     *
     * @param cont
     *      The JSONObject to set as the content of the message
     */
     public void setJSONContent(JSONObject cont) {

        if (!isSetContentType) {
            // Set the content if there's no content yet and if the content-type is not set
            this.putExtra(SDCS_MESSAGES.EXTRA_NAME_CONTENT_TYPE, SUPPORTED_CONTENT_TYPES.APPLICATION_JSON);
            isSetContentType = true;
            this.putExtra(SDCS_MESSAGES.EXTRA_NAME_CONTENT, cont.toString());
        } else {
            // In case of content-type mismatch, just return
            if (!this.getStringExtra(SDCS_MESSAGES.EXTRA_NAME_CONTENT_TYPE).equalsIgnoreCase(SUPPORTED_CONTENT_TYPES.APPLICATION_JSON)) {
                return;
            }

            // Append the content passed as input to the content already present
            this.putExtra(SDCS_MESSAGES.EXTRA_NAME_CONTENT, this.getStringExtra(SDCS_MESSAGES.EXTRA_NAME_CONTENT) + cont.toString());

        }

    }

    /**
     * Specify an ID for the message, create a timestamp and send the message to the SDCS
     * broadcasting the Intent
     *
     * @param messageID
     *      The ID of the message to be sent
     */
    @Override
    public void send(int messageID) {
        this.putExtra(SDCS_MESSAGES.EXTRA_NAME_REQID, Integer.toString(messageID));
        this.messID = messageID;
        baseContext.sendBroadcast(this);
        timestamp = new Date().getTime();

        Log.d(LOGTAG, this.toString());

    }

    /**
     * Get the message ID associated with the message
     *
     * @return
     *      The message ID of the Intent, or null if it hasn't been set yet
     */
    @Override
    public int getMessageID() {
        return this.messID;
    }

    /**
     * Get the timestamp associated with the message
     *
     * @return
     *      The timestamp associated with the message
     */
    @Override
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Create and return a human readable representation of the message
     *
     * @return
     *      A human readable representation of the message
     */
    public String toString() {

        String intStr = "Package: " + this.getPackage() + "\n";
        intStr += "Action: " + this.getAction() + "\n";
        intStr += "Issuer: " + this.getStringExtra(SDCS_MESSAGES.EXTRA_NAME_ISSUER) + "\n";
        intStr += "Reply Action: " + this.getStringExtra(SDCS_MESSAGES.EXTRA_NAME_REPLY_ACTION) + "\n";
        intStr += "Request ID: " + this.getStringExtra(SDCS_MESSAGES.EXTRA_NAME_REQID) + "\n";
        intStr += "Timestamp: " + timestamp + "\n";
        intStr += "Content Type: " + this.getStringExtra(SDCS_MESSAGES.EXTRA_NAME_CONTENT_TYPE) + "\n";
        intStr += "Method: " + this.getStringExtra(SDCS_MESSAGES.EXTRA_NAME_METHOD) + "\n";
        intStr += "Path: " + this.getStringExtra(SDCS_MESSAGES.EXTRA_NAME_PATH) + "\n";
        intStr += "Content: " + this.getStringExtra(SDCS_MESSAGES.EXTRA_NAME_CONTENT) + "\n";

        return intStr;
    }

}
