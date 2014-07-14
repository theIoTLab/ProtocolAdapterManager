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

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

import eu.fistar.sdcs.pa.PAAndroidConstants.*;
import eu.fistar.sdcs.pa.da.IDeviceAdapter;

/**
 * This class is the implementation of the Protocol Adapter. For now, it only controls Device
 * Adapters for Bluetooth medical devices
 *
 * @author Marcello Morena
 * @author Alexandru Serbanati
 */
public class PAManagerService extends Service implements IProtocolAdapter {

    // Config constants
    private static final long EXPIRATION_TIME = 10 * 1000;
    private static final long POST_JOB_MIN_INTERVAL = 1000;
    private static final String DEVICE_SEARCH_STRING = "healthDevice";

    // Binder for this instance of the PA
    private final PAManBinder paBinder = new PAManBinder();

    // Device Adapter management
    private String[] availableDAs;                                          // [DAName] from ComponentName.flattenToString()
    private Map<String, IDeviceAdapter> listDA = new HashMap<String, IDeviceAdapter>();     // <[DAName], [DAInstance]>

    // Protocol Adapter management
    private boolean firstStart = true;

    // Message management
    private List<ISDCSMessage> sentMessages = new CopyOnWriteArrayList<ISDCSMessage>();
    private int counter_messageID =  new Random().nextInt(99999);
    private long lastPostedJob = new Date().getTime();
    final Handler h = new Handler();

    // Base path for applications
    private String applicationsReference;

    /**
     * Endpoint for managing the connection/disconnection of the Device Adapters
     */
    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {

            // Retrieve the Device Adapter from the binder
            IDeviceAdapter tempDA = ((IDeviceAdapter.DABinder) iBinder).getDeviceAdapter();

            // Add the Device Adapter to the List
            listDA.put(componentName.flattenToString(), tempDA);
            Log.i(PAAndroidConstants.PA_LOGTAG, "New Device Adapter added: " + componentName.flattenToString() + ".");

            // Register the Protocol Adapter into the Device Adapter
            tempDA.RegisterPA(paBinder);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.i(PAAndroidConstants.PA_LOGTAG, componentName.flattenToString() + " Device Adapter service disconnected.");

            // Remove the Device Adapter from the DA List
            listDA.remove(componentName.flattenToString());

            try {
                // Try to start the DA again. This way, the DA will be added again to the DA List
                // when onServiceConnected is called
                startDeviceAdapter(componentName.flattenToString());
            } catch (Exception ex) {
                Log.e(PAAndroidConstants.PA_LOGTAG, componentName.flattenToString() + " Device Adapter failed to start.");
            }
        }

    };

    /**
     * Initialize the protocol adapter, and specifically:
     * <ul>
     *     <li>Retrieve the base parameters</li>
     *     <li>Discover all the available DAs</li>
     *     <li>Start the Device Adapters</li>
     *     <li>Create the Intent Filter to receive Intents from SDCS</li>
     *     <li>Register the Broadcast Receiver for broadcast-intent based communication</li>
     * </ul>
     */
    public void init(Intent i) {

        // Retrieve the base parameter from the SDCS
        retrieveBaseParams(i);

        // Discover DAs on the system
        discoverDAs();

        // Start the Device Adapters
        startDeviceAdapters();
        Log.i(PAAndroidConstants.PA_LOGTAG, "Started Device Adapters");

        // Create the Intent Filter
        IntentFilter filter = new IntentFilter();
        filter.addAction(PAAndroidConstants.PACKAGE);

        // Register the Broadcast Receiver
        registerReceiver(broadcastReceiverPA, filter);
        Log.i(PAAndroidConstants.PA_LOGTAG, "Added filter for Protocol Adapter: " + PAAndroidConstants.PACKAGE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // If this is the first start, do some initialization tasks
        if (firstStart) {
            try {
                // Check who is the Issuer who started the PA
                String issuer = intent.getStringExtra(SDCS_MESSAGES.EXTRA_NAME_ISSUER);
                if (issuer == null) {
                    Log.d(PAAndroidConstants.PA_LOGTAG, "No Issuer found in activation message. Using default.");
                } else {
                    Log.d(PAAndroidConstants.PA_LOGTAG, "onStartCommand called, Issuer is: " + issuer);
                }

            } catch (Exception ex) {
                Log.d(PAAndroidConstants.PA_LOGTAG, ex.toString());
            }

            // Initialization phase
            init(intent);

            // Do not allow the repetition of this phase
            firstStart = false;

            Log.i(PAAndroidConstants.PA_LOGTAG, "Protocol Adapter up and running.");
        } else {
            Log.i(PAAndroidConstants.PA_LOGTAG, "Protocol Adapter already up and running: not restarted.");
        }

        return super.onStartCommand(intent, flags, startId);

    }

    /**
     * Discover all the DA available on the system
     */
    private void discoverDAs() {
        //TODO: [BETA] implement actual search for DA Services
        availableDAs = PAAndroidConstants.AVAILABLE_DAS;
    }

    /**
     * Retrieve the base parameters from the SDCS instance running on the system
     */
    private void retrieveBaseParams(Intent i) {
        // TODO: [BETA] Get the names of DeviceAdapters to activate from SDCS
        // TODO: [BETA] Get the applicationsReference from SDCS
        // TODO: [BETA] Retrieve all info from starting Intent
        applicationsReference = "/m2m/applications/";
    }

    @Override
    public IBinder onBind(Intent intent) {
        // This is not a bound service, so always return null
        return null;
    }

    /**
     * Start the given Device Adapter
     *
     * @param daName
     *      The Device Adapter to start
     */
    private void startDeviceAdapter(String daName) {
        //TODO [BETA] Make this generic for every DA

        if (daName.startsWith(DEVICE_ADAPTERS.HDP_SERVICE_NAME)) {
            Intent intent;
            intent = new Intent(daName);
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        }

    }

    /**
     * Start all the device adapters present in the the DA's array
     */
    private void startDeviceAdapters() {
        //TODO: [BETA] get list of DA identifiers (availableDAs) to be activated from SDCS
        //TODO: [BETA] check if there is locally an implementation of these DAs

        // Start all Device Adapters that are installed
        for (String da_name : availableDAs) {
            startDeviceAdapter(da_name);
        }

    }

    /**
     * Create a SDCS message of the given type
     *
     * @param messageType
     *      The type of the message
     *
     * @param source_device
     *      The device
     *
     * @return
     *      The created message
     */
    private ISDCSMessage createSDCSMessage(short messageType, String source_device) {
        return new SDCSIntentMessage(getBaseContext(), messageType, source_device, applicationsReference);
    }

    /**
     * Register a new device with the SDCS
     *
     * @param dev_desc
     *      The device to register
     */
    @Override
    public void register_Device(final IDeviceDescription dev_desc) {

        // Create a device registration message
        ISDCSMessage sdcsMessage = createSDCSMessage(SDCS_MESSAGES.MSG_TYPE_DEV_REGISTRATION, createResourceString(dev_desc.getModelName()));

        // Build the Description of the Device
        try{
            JSONArray array = new JSONArray();
            array.put(DEVICE_SEARCH_STRING);

            JSONObject searchString = new JSONObject();
            searchString.put(SDCS_MESSAGES.JSON_NAME_SEARCH_STRING, array);

            JSONObject app_info = new JSONObject();
            app_info.put(SDCS_MESSAGES.JSON_NAME_APP_ID, createResourceString(dev_desc.getModelName()));

            app_info.put(SDCS_MESSAGES.JSON_NAME_SEARCH_STRINGS, searchString);

            JSONObject app = new JSONObject();
            app.put(SDCS_MESSAGES.JSON_NAME_APPLICATION, app_info);

            sdcsMessage.setJSONContent(app);

        }catch(JSONException e){
            Log.e(PAAndroidConstants.PA_LOGTAG, "Error on registering the PA: ",e);
        }

        // Send the message
        sdcsMessage.send(counter_messageID++);
        sentMessages.add(sdcsMessage);
        Log.i(PAAndroidConstants.PA_LOGTAG, "Device registration message sent correctly with message number "+ (counter_messageID-1));

        // Handle the queue cleaning
        queueCleaner();

        // Register all the device's property with the SDCS
        register_Device_Properties(dev_desc);
    }

    /**
     * Register all the property of a given device taking care of sending one separate message for
     * each property
     *
     * @param dev_desc
     *      The device whom the properties belongs to
     */
    @Override
    public void register_Device_Properties(IDeviceDescription dev_desc) {
        final List<ISensorDescription> sensors = dev_desc.getSensorList();
        final String devResName = createResourceString(dev_desc.getModelName());

        for (ISensorDescription sensor : sensors) {

            // Create a property registration message
            ISDCSMessage sdcsMessage = createSDCSMessage(SDCS_MESSAGES.MSG_TYPE_DEV_PROPERTIES_REGISTRATION, devResName);

            try {
                // Build the Description of the Sensor/Property
                JSONObject id = new JSONObject();
                id.put(SDCS_MESSAGES.JSON_NAME_ID, createResourceString(sensor.getPropertyName()));

                JSONObject container = new JSONObject();
                container.put(SDCS_MESSAGES.JSON_NAME_CONTAINER, id);

                sdcsMessage.setJSONContent(container);

                // Set the right path for the message
                sdcsMessage.setPath(applicationsReference + devResName + "/containers");

            } catch (JSONException e) {
                Log.e(PAAndroidConstants.PA_LOGTAG, "Error on registering the property of the device " + devResName + ": ", e);
            }

            // Send the message
            sdcsMessage.send(counter_messageID++);
            sentMessages.add(sdcsMessage);

            // Handle the queue cleaning
            queueCleaner();
        }
    }

    /**
     * Push the data received from the DA, taking care of sending one separate message for each
     * measurement
     *
     * @param observations
     *      The measurements to push to SDCS
     *
     * @param dev_desc
     *      The device whom the observation belongs to
     */
    @Override
    public void push_Data(IObservation[] observations, IDeviceDescription dev_desc) {

        //TODO: [BETA] Ask for change to let the SDCS handle multiple measurements per message

        // Send one separate message for each measurement
        for (IObservation obs : observations) {
        //for (int i=0; i<observations.length; i++) {

            // Create a push message
            ISDCSMessage sdcsMessage = createSDCSMessage(SDCS_MESSAGES.MSG_TYPE_DATA_PUSH, createResourceString(dev_desc.getModelName()));

            // Set the right path for the message
            String path = applicationsReference + createResourceString(dev_desc.getModelName()) + "/containers/" + createResourceString(obs.getPropertyName());
            sdcsMessage.setPath(path + "/contentInstances");

            // Add the measurement to the message
            sdcsMessage.addObservation(obs);

            // send the message to the SDCS
            sdcsMessage.send(counter_messageID++);
            sentMessages.add(sdcsMessage);

            // Handle the queue cleaning
            queueCleaner();

        }
    }

    /**
     * Deregister the given device from the SDCS
     *
     * @param dev_desc
     *      The device to use
     */
    public void deregister_Device(IDeviceDescription dev_desc) {
        ISDCSMessage sdcsMessage = createSDCSMessage(SDCS_MESSAGES.MSG_TYPE_DEV_DEREGISTRATION, createResourceString(dev_desc.getModelName()));

        // Create a deregistration message and send it to the SDCS
        sdcsMessage.setPath(applicationsReference + createResourceString(dev_desc.getModelName()));
        sdcsMessage.send(counter_messageID++);
        sentMessages.add(sdcsMessage);

        // Handle the queue cleaning
        queueCleaner();

    }


    /**
     * This class is used to let the Device Adapter get the instance of Protocol Adapter
     */
    public class PAManBinder extends PABinder {
        public IProtocolAdapter getProtocolAdapter() {
            // Return this instance of the service
            return PAManagerService.this;
        }
    }

    BroadcastReceiver broadcastReceiverPA = new BroadcastReceiver() {

        /**
         * Handle the reception of SDCS's confirmation messages
         *
         * @param context
         *      The context
         *
         * @param intent
         *      The received Intent
         */
        @Override
        public void onReceive(Context context, Intent intent) {

            String messageId = intent.getExtras().getString(SDCS_MESSAGES.EXTRA_NAME_REQID);
            Log.v(PAAndroidConstants.PA_LOGTAG, "Received an intent for action "+intent.getAction()+" with extras "+intent.getExtras().toString());

            // If there is no messageId in the Intent just return and do nothing else
            if (messageId == null) {
                Log.w(PAAndroidConstants.PA_LOGTAG, "Dropping message with no messageID");
                return;
            }
            Log.v(PAAndroidConstants.PA_LOGTAG, "MessageID is: " + messageId);


            // If there is no status, this can be an unsolicited message from SDCS.
            // This is not handled yet, so just return and do nothing else
            String status = intent.getExtras().getString(SDCS_MESSAGES.EXTRA_NAME_STATUS);
            if (status == null) {
                //TODO: [BETA] Implement handling for some kind of spontaneous messages from SDCS
                Log.v(PAAndroidConstants.PA_LOGTAG, "Status in message " + messageId + " is not set.");

            } else {
                Log.v(PAAndroidConstants.PA_LOGTAG, "Status in message " + messageId + " is : " + status + ". The content is: " + intent.getExtras().getString(SDCS_MESSAGES.EXTRA_NAME_CONTENT));
                // Process response message status
                if(status.startsWith(SDCS_MESSAGES.MSG_CODE_OK_PREFIX, 0)){

                    // The message has been successfully confirmed from SDCS
                    Log.v(PAAndroidConstants.PA_LOGTAG, "received confirmation for message: "+ messageId);
                    int mID = Integer.parseInt(messageId);

                    // We can safely find the message and remove it from the queue
                    if (getSentMessageByID(mID) != null) {
                        removeSentMessageByID(mID);
                    // } else {
                        // Do nothing...
                        // The message is not in the queue anymore or has never been there.
                        // We can safely ignore this case.
                    }
                }
            }
        }
    };

    /**
     * Delete from the queue the message having the given ID
     *
     * @param messageID
     *      The message identifier
     *
     * @return
     *      True if the message was deleted, false otherwise
     */
    private boolean removeSentMessageByID(int messageID) {

        ISDCSMessage message = getSentMessageByID(messageID);

        if (message ==null) return false;

        sentMessages.remove(message);
        return true;

    }

    /**
     * Retrieve from the queue the message having the given ID
     *
     * @param messageID
     *      The message identifier
     *
     * @return
     *      The message having the specified ID if any, null otherwise
     */
    private ISDCSMessage getSentMessageByID(int messageID) {

        for (ISDCSMessage message : sentMessages) {
            if (message.getMessageID() == messageID) {
                return message;
            }
        }
        return null;
    }

    /**
     * Check if there are expired messages and remove them from the sent message's queue
     */
    private void removeExpiredSentMessages() {
        long now = new Date().getTime();

        for (ISDCSMessage message : sentMessages) {

            // If the message is expired, remove it from the queue
            if (message.getTimestamp() < (now - EXPIRATION_TIME)) {
                sentMessages.remove(message);
                //TODO: [BETA] Handle the timeout somehow
            }

        }
    }

    /**
     * Called every time a message is sent. Takes care of posting a new delayed job in the Handler
     * to clean the message queue from messages that will expire. Also avoids posting too many jobs
     */
    private void queueCleaner() {

        long now = new Date().getTime();

        // If enough time has passed since last job posting,
        // post a new job and reset the counter
        if (lastPostedJob < (now - POST_JOB_MIN_INTERVAL)) {

            h.postDelayed(new Runnable() {
                @Override
                public void run() {
                    removeExpiredSentMessages();
                }
            }, EXPIRATION_TIME + POST_JOB_MIN_INTERVAL);

            lastPostedJob = now;

        }

    }

    /**
     * Make the input String URL-Safe
     *
     * @param input
     *      The String containing the name of the resource
     *
     * @return
     *      The URL-Safe String
     */
    private String createResourceString(String input) {
        // Return the model name without spaces and slashes
        return input.replaceAll("\\s", "").replaceAll("/", "");
    }
}
