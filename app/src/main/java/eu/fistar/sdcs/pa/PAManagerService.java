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
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import eu.fistar.sdcs.pa.common.Capabilities;
import eu.fistar.sdcs.pa.common.IProtocolAdapter;
import eu.fistar.sdcs.pa.common.IProtocolAdapterListener;
import eu.fistar.sdcs.pa.common.PAAndroidConstants;
import eu.fistar.sdcs.pa.common.PAAndroidConstants.*;
import eu.fistar.sdcs.pa.common.DeviceDescription;
import eu.fistar.sdcs.pa.common.Observation;
import eu.fistar.sdcs.pa.common.IDeviceAdapterListener;
import eu.fistar.sdcs.pa.common.da.IDeviceAdapter;

/**
 * This class is the implementation of the Protocol Adapter. It is a bound service which can,
 * in turn, bind other services (the Device Adapters). It implements both the IProtocolAdapter
 * interface for communication with the application, and the IDeviceAdapterListener interface for
 * communication with Device Adapters.
 *
 * @author Marcello Morena
 * @author Alexandru Serbanati
 */
public class PAManagerService extends Service {

    // SharedPreferences related constants
    private final static String SHPREF_FILENAME = "listSync";
    private final static String SHPREF_WHITELIST_NAME = "whitelist";
    private final static String SHPREF_BLACKLIST_NAME = "blacklist";

    // Variables for storing white/black lists
    private final List<String> blacklist = new CopyOnWriteArrayList<>();
    private final List<String> whitelist = new CopyOnWriteArrayList<>();

    // Variables for Device Adapter management
    private Map<String, Capabilities> availableDAs = new ConcurrentHashMap<>(); // <[DA ID], [DACapabilities]>
    private Map<String, IDeviceAdapter> connectedDAs = new ConcurrentHashMap<>(); // <[DA ID], [DAInstance]>
    private Map<String, DAConnection> daConnections = new ConcurrentHashMap<>(); // <[DA ID], [DAConnection]>

    // Variables for Application Management
    private IProtocolAdapterListener appApi;

    // Variables for Protocol Adapter management
    private boolean firstStart = true;

    // Implementation of the Protocol Adapter API (IProtocolAdapter) to pass to the Application
    private final IProtocolAdapter.Stub appEndpoint = new IProtocolAdapter.Stub() {

        /**
         * Returns a list of all the devices connected at the moment in all Device Adapters.
         *
         * @return A List containing the DeviceDescription of all the connected devices
         */
        @Override
        public List<DeviceDescription> getConnectedDevices() throws RemoteException {
            Log.i(PAAndroidConstants.PA_LOGTAG, "Fetching connected devices");

            List<DeviceDescription> connectedDev = new ArrayList<DeviceDescription>();

            // Scan all connected DAs
            for (String tmpDaName : connectedDAs.keySet()) {

                // Retrieve the Capabilities of the DA
                Capabilities cap = availableDAs.get(tmpDaName);

                // If the DA has access to connected devices, add its devices to the general list
                if (cap.isCommunicationInitiator()) {
                    connectedDev.addAll(connectedDAs.get(tmpDaName).getConnectedDevices());
                }
            }

            // Return the list of devices
            return connectedDev;
        }

        /**
         * Returns a map containing the Device ID of all the devices paired with the smartphone that can
         * be handled by at least one DA as the key, and a list of DA IDs of DA that can handle that
         * device as the value.
         *
         * @return A map containing the Device ID of all the paired devices as the key, and a list of DA
         * IDs that can handle that device as the value (Map<String, List<String>>).
         */
        @Override
        public Map<String, List<String>> getDADevices() throws RemoteException {
            Log.i(PAAndroidConstants.PA_LOGTAG, "Fetching paired devices address");

            Map<String, List<String>> daDev = new HashMap<String, List<String>>();

            // For each connected DA...
            for (IDeviceAdapter tmpDa : connectedDAs.values()) {

                // ... retrieve the Capabilities of the DA...
                Capabilities cap = tmpDa.getDACapabilities();

                // ... then if it can provide the paired devices...
                if (cap.canProvideAvailableDevice()) {

                    // ... retrieve all the managed paired devices and the DA ID...
                    List<String> daPairedDev = tmpDa.getPairedDevicesAddress();
                    String daId = cap.getPackageName();

                    // ... and for each device...
                    for (String dev : daPairedDev) {

                        // ... check if it's already contained in the original HashMap...
                        List<String> daHandlingDevice = daDev.get(dev);

                        // ... if so, then add the actual DA to the list of DAs handling this device
                        if (daHandlingDevice != null) {
                            daHandlingDevice.add(daId);
                        }
                        // Otherwise create a new entry in the map for the device
                        else {
                            daHandlingDevice = new ArrayList<String>();
                            daHandlingDevice.add(daId);
                            daDev.put(dev, daHandlingDevice);
                        }
                    }
                }
            }

            // Return the HashMap of the devices
            return daDev;
        }

        /**
         * Returns a list of devices that can be detected with a scanning.
         *
         * @return A list containing the Device ID of all the discovered devices
         */
        @Override
        public List<String> detectDevices() throws RemoteException {
            // TODO Issue #3 Evaluate whether method detectDevices is really needed and, if so, evaluate whether it should be made asynchronous.
            throw new UnsupportedOperationException("This method is not working yet!");
        }

        /**
         * Set the specific configuration of a device managed by the Device Adapter passing a data
         * structure with key-value pairs containing all possible configuration parameters and
         * their values, together with the device ID. This should be done before starting the Device
         * Adapter, otherwise standard configuration will be used. Depending on capabilities, this
         * could also be invoked when the DA is already running.
         *
         * @param config The configuration for the device in the form of a key/value set (String/String)
         * @param devId The device ID (the MAC Address)
         */
        @Override
        public void setDeviceConfig(Map config, String devId) throws RemoteException {
            Log.i(PAAndroidConstants.PA_LOGTAG, "Fetching paired devices address");

            // Scan all connected DAs
            for (String tmpDaName : connectedDAs.keySet()) {

                // Retrieve the Capabilities of the DA
                Capabilities cap = availableDAs.get(tmpDaName);

                // If the DA supports device configuration at runtime, try to configure the device
                if (cap.getDeviceConfigurationType() == Capabilities.CONFIG_RUNTIME_ONLY ||
                        cap.getDeviceConfigurationType() == Capabilities.CONFIG_STARTUP_AND_RUNTIME) {
                    connectedDAs.get(tmpDaName).setDeviceConfig(config, devId);
                }
            }
        }

        /**
         * Start the Device Adapter operations. This will cause the PA to bind the DA's service
         * and start the DA.
         *
         * @param daId The Device Adapter ID
         */
        @Override
        public void startDA(String daId) throws RemoteException {
            Log.i(PAAndroidConstants.PA_LOGTAG, "Starting Device Adapter " + daId);

            // Retrieve the Capabilities object for the specified DA
            Capabilities daCap = daId != null ? availableDAs.get(daId) : null;

            // Start the specified DA using the correct action in the Intent if the DA exists and
            // it is not already started
            if (daCap != null) {
                if (connectedDAs.get(daId) == null) {
                    Intent intent;
                    String action = daCap.getActionName();
                    String pkg = daCap.getPackageName();
                    intent = new Intent().setComponent(new ComponentName(pkg, action));
                    bindService(intent, new DAConnection(), Context.BIND_AUTO_CREATE);
                }
                else {
                    Log.i(PAAndroidConstants.PA_LOGTAG, "Device Adapter " + daId + " is already started");
                }
            }
            else {
                Log.i(PAAndroidConstants.PA_LOGTAG, "Device Adapter " + daId + " is not available in the system");
            }

        }

        /**
         * Stop the Device Adapter operations. This will cause the PA to stop the DA and unbind the
         * related service.
         *
         * @param daId The Device Adapter ID
         */
        @Override
        public void stopDA(String daId) throws RemoteException {
            Log.i(PAAndroidConstants.PA_LOGTAG, "Stopping Device Adapter " + daId);

            // Retrieve the api endpoint of the specified DA
            IDeviceAdapter tmpDa = daId != null ? connectedDAs.get(daId) : null;

            if (tmpDa != null) {
                // Stop the operation of the Device Adapter
                tmpDa.stop();

                // Get the DAConnection object from the Map
                DAConnection conn = daConnections.get(daId);

                if (conn != null) {
                    // Unbind the Device Adapter
                    unbindService(conn);

                    // Remove the DA from the Maps
                    connectedDAs.remove(daId);
                    daConnections.remove(daId);
                }
            }
            else {
                Log.i(PAAndroidConstants.PA_LOGTAG, "Device Adapter " + daId + " is not running");
            }

        }

        /**
         * Return a Map with all the available DAs in the system. The keys of the Map are the DAs'
         * ID and the values are the related Capabilities object.
         *
         * @return A Map with DA identifiers as key and Capabilities object as value
         */
        @Override
        public Map getAvailableDAs() throws RemoteException {
            return availableDAs;
        }

        /**
         * Return the object describing the capabilities of the specified DA.
         *
         * @param daId ID of the DA
         * @return An instance of the Capabilities object containing all the capabilities of the device
         */
        @Override
        public Capabilities getDACapabilities(String daId) throws RemoteException {
            return daId != null ? availableDAs.get(daId) : null;
        }

        /**
         * Connect to the device whose MAC Address is passed as an argument.
         *
         * @param devId The Device ID
         */
        @Override
        public void connectDev(String devId) throws RemoteException {
            Log.i(PAAndroidConstants.PA_LOGTAG, "Connecting to the specific device " + devId);

            // Retrieve the map of the devices and the list of the DAs handling the specified device
            Map<String, List<String>> devices = getDADevices();
            List<String> daHandlingDevice = devices.get(devId);

            // Check whether there is exactly one DA handling the specified device
            if (daHandlingDevice != null && daHandlingDevice.size() == 1) {

                // Retrieve the Capabilities and the endpoint of that DA
                Capabilities cap = availableDAs.get(daHandlingDevice.get(0));
                IDeviceAdapter da = connectedDAs.get(daHandlingDevice.get(0));

                // If the DA supports connection initiation, then connect to the specified device
                if (cap.isCommunicationInitiator()) {
                    da.connectDev(devId);
                } else {
                    appApi.log(LOG_LEVEL.ERROR, PAAndroidConstants.PA_PACKAGE, "Connection initiation is not supported by the specified device (" + devId + ")");
                    throw new RuntimeException("Connection initiation is not supported by the specified device (" + devId + ")");
                }

            } else {
                appApi.log(LOG_LEVEL.ERROR, PAAndroidConstants.PA_PACKAGE, "The device " + devId + " is not present in the list or is handled by more than one Device Adapter. Try using forceConnectDev.");
                throw new RuntimeException("The device " + devId + " is not present in the list or is handled by more than one Device Adapter. Try using forceConnectDev.");
            }
        }

        /**
         * Force connection to the device whose devID is passed as an argument using the specified
         * Device Adapter. This method can be used to connect a supported device that, for some
         * reasons, is not recognised by the corresponding DA.
         *
         * @param devId The Device ID
         * @param daId The ID of the Device Adapter
         */
        @Override
        public void forceConnectDev(String devId, String daId) throws RemoteException {
            Log.i(PAAndroidConstants.PA_LOGTAG, "Forcing connection to the specific device " + devId + " via " + daId);

            // Retrieve the specified DA and its capabilities
            Capabilities cap = availableDAs.get(daId);
            IDeviceAdapter da = connectedDAs.get(daId);

            // If the DA is available in the system...
            if (cap != null) {
                // ... and it is connected...
                if (da != null) {
                    // ... and it supports connection initiation
                    if (cap.isCommunicationInitiator()) {
                        // ... then try connecting to the device
                        da.forceConnectDev(devId);
                    } else {
                        appApi.log(LOG_LEVEL.ERROR, PAAndroidConstants.PA_PACKAGE, "Connection initiation is not supported by the specified device (" + devId + ")");
                        throw new RuntimeException("Connection initiation is not supported by the specified device (" + devId + ")");
                    }
                } else {
                    appApi.log(LOG_LEVEL.ERROR, PAAndroidConstants.PA_PACKAGE, "The specified Device Adapter " + devId + " is not connected!");
                    throw new RuntimeException("The specified Device Adapter " + devId + " is not connected!");
                }
            } else {
                appApi.log(LOG_LEVEL.ERROR, PAAndroidConstants.PA_PACKAGE, "The specified Device Adapter " + devId + " is not available in the system!");
                throw new RuntimeException("The specified Device Adapter " + devId + " is not available in the system!");
            }
        }

        /**
         * Disconnect from the device whose MAC Address is passed as an argument.
         *
         * @param devId The Device ID
         */
        @Override
        public void disconnectDev(String devId) throws RemoteException {
            Log.i(PAAndroidConstants.PA_LOGTAG, "Disconnecting device " + devId);

            // Scan all connected DAs
            for (String tmpDaName : connectedDAs.keySet()) {

                // Retrieve the Capabilities of the DA
                Capabilities cap = availableDAs.get(tmpDaName);

                // If the DA supports connection initiation, connect to the specified device
                if (cap.isCommunicationInitiator()) {
                    connectedDAs.get(tmpDaName).disconnectDev(devId);
                }
            }
        }

        /**
         * Receive a binder from the Application representing its interface.
         *
         * @param application The IBinder of the application
         */
        @Override
        public void registerPAListener(IBinder application) throws RemoteException {
            appApi = IProtocolAdapterListener.Stub.asInterface(application);
            // TODO If more initialization or initial actions are needed after the application registered itself, just do them here
        }

        /**
         * Add a device to the Device Adapter whitelist, passing its device ID as an argument.
         * Note that this insertion will persist, even through Device Adapter reboots, until
         * the device it's removed from the list. Every device adapter should check the format
         * of the address passed as an argument and, if it does not support that kind of
         * address, it can safely ignore that address.
         *
         * @param devId The Device ID
         */
        @Override
        public void addDeviceToWhitelist(String devId) throws RemoteException {
            Log.i(PAAndroidConstants.PA_LOGTAG, "Adding device " + devId + " to whitelist");

            // If the device ID is not valid, just do nothing
            if (devId == null || "".equals(devId)) return;

            // Othwerwise add it to the list
            whitelist.add(devId);

            // Reflect the changes on SharedPreferences
            syncSharedPreferences();

            // Scan all connected DAs
            for (String tmpDaName : connectedDAs.keySet()) {

                // Retrieve the Capabilities of the DA
                Capabilities cap = availableDAs.get(tmpDaName);

                // If the DA supports whitelist, add the specified device to its whitelist
                if (cap.hasWhitelist()) {
                    connectedDAs.get(tmpDaName).addDeviceToWhitelist(devId);
                }
            }
        }

        /**
         * Remove from the whitelist the device whose device ID is passed as an argument.
         * If the device is not in the list, the request can be ignored.
         *
         * @param devId The Device ID
         */
        @Override
        public void removeDeviceFromWhitelist(String devId) throws RemoteException {
            Log.i(PAAndroidConstants.PA_LOGTAG, "Removing device " + devId + " from whitelist");

            // If the device ID is not valid, just do nothing
            if (devId == null || "".equals(devId)) return;

            // Othwerwise remove it from the list
            whitelist.remove(devId);

            // Reflect the changes on SharedPreferences
            syncSharedPreferences();

            // Scan all connected DAs
            for (String tmpDaName : connectedDAs.keySet()) {

                // Retrieve the Capabilities of the DA
                Capabilities cap = availableDAs.get(tmpDaName);

                // If the DA supports whitelist, remove the specified device from whitelist
                if (cap.hasWhitelist()) {
                    connectedDAs.get(tmpDaName).removeDeviceFromWhitelist(devId);
                }
            }
        }

        /**
         * Retrieve all the devices in the whitelist of the DA. If there's no devices, an
         * empty list is returned.
         *
         * @return A list containing all the device id in the white list
         */
        @Override
        public List<String> getWhitelist() throws RemoteException {
            Log.i(PAAndroidConstants.PA_LOGTAG, "Retrieving the global whitelist");

            return whitelist;
        }

        /**
         * Set a list of devices in the whitelist all together, passing their device IDs as an argument.
         * Note that this insertion will persist, even through Device Adapter reboots, until
         * the devices are removed from the list. Every device adapter should check the format
         * of the address passed as an argument one by one and, if it does not support that kind of
         * address, it can safely ignore that address.
         *
         * @param mWhitelist A list containing all the device id to insert in the white list
         */
        @Override
        public void setWhitelist(List<String> mWhitelist) throws RemoteException {
            Log.i(PAAndroidConstants.PA_LOGTAG, "Setting the global whitelist");

            // Empty the list
            emptyList(whitelist);

            if (mWhitelist != null) {
                // Add to the whitelist every element of the list passed as argument
                for (String dev : mWhitelist) {
                    if (dev != null && !"".equals(dev)) whitelist.add(dev);
                }
            }

            // Reflect the changes on SharedPreferences
            syncSharedPreferences();

            // Scan all connected DAs
            for (String tmpDaName : connectedDAs.keySet()) {

                // Retrieve the Capabilities of the DA
                Capabilities cap = availableDAs.get(tmpDaName);

                // If the DA supports whitelist, remove the specified device from whitelist
                if (cap.hasWhitelist()) {
                    connectedDAs.get(tmpDaName).setWhitelist(whitelist);
                }
            }
        }

        /**
         * Add a device to the Device Adapter blacklist, passing its device ID as an argument.
         * Note that this insertion will persist, even through Device Adapter reboots, until
         * the device it's removed from the list. Every device adapter should check the format
         * of the address passed as an argument and, if it does not support that kind of
         * address, it can safely ignore that address.
         *
         * @param devId The Device ID
         */
        @Override
        public void addDeviceToBlackList(String devId) throws RemoteException {
            Log.i(PAAndroidConstants.PA_LOGTAG, "Adding device " + devId + " to blacklist");

            // If the device ID is not valid, just do nothing
            if (devId == null || "".equals(devId)) return;

            // Othwerwise add it to the list
            blacklist.add(devId);

            // Reflect the changes on SharedPreferences
            syncSharedPreferences();

            // Scan all connected DAs
            for (String tmpDaName : connectedDAs.keySet()) {

                // Retrieve the Capabilities of the DA
                Capabilities cap = availableDAs.get(tmpDaName);

                // If the DA supports blacklist, add the specified device to blacklist
                if (cap.hasBlacklist()) {
                    connectedDAs.get(tmpDaName).addDeviceToBlackList(devId);
                }
            }
        }

        /**
         * Remove from the blacklist the device whose device ID is passed as an argument.
         * If the device is not in the list, the request can be ignored.
         *
         * @param devId The Device ID
         */
        @Override
        public void removeDeviceFromBlacklist(String devId) throws RemoteException {
            Log.i(PAAndroidConstants.PA_LOGTAG, "Removing device " + devId + " from blacklist");

            // If the device ID is not valid, just do nothing
            if (devId == null || "".equals(devId)) return;

            // Othwerwise remove it from the list
            blacklist.remove(devId);

            // Reflect the changes on SharedPreferences
            syncSharedPreferences();

            // Scan all connected DAs
            for (String tmpDaName : connectedDAs.keySet()) {

                // Retrieve the Capabilities of the DA
                Capabilities cap = availableDAs.get(tmpDaName);

                // If the DA supports blacklist, remove the specified device from blacklist
                if (cap.hasBlacklist()) {
                    connectedDAs.get(tmpDaName).removeDeviceFromBlacklist(devId);
                }
            }
        }

        /**
         * Retrieve all the devices in the blacklist of the DA. If there's no devices, an
         * empty list is returned.
         *
         * @return A list containing all the device id in the black list
         */
        @Override
        public List<String> getBlacklist() throws RemoteException {
            Log.i(PAAndroidConstants.PA_LOGTAG, "Retrieving the global blacklist");

            return blacklist;
        }

        /**
         * Set a list of devices in the blacklist all together, passing their device IDs as an argument.
         * Note that this insertion will persist, even through Device Adapter reboots, until
         * the devices are removed from the list. Every device adapter should check the format
         * of the address passed as an argument one by one and, if it does not support that kind of
         * address, it can safely ignore that address.
         *
         * @param mBlacklist A list containing all the device id to insert in the black list
         */
        @Override
        public void setBlackList(List<String> mBlacklist) throws RemoteException {
            Log.i(PAAndroidConstants.PA_LOGTAG, "Setting the global blacklist");

            // Empty the list
            emptyList(blacklist);

            if (mBlacklist != null) {
                // Add to the whitelist every element of the list passed as argument
                for (String dev : mBlacklist) {
                    if (dev != null && !"".equals(dev)) blacklist.add(dev);
                }
            }

            // Reflect the changes on SharedPreferences
            syncSharedPreferences();

            // Scan all connected DAs
            for (String tmpDaName : connectedDAs.keySet()) {

                // Retrieve the Capabilities of the DA
                Capabilities cap = availableDAs.get(tmpDaName);

                // If the DA supports blacklist, remove the specified device from blacklist
                if (cap.hasBlacklist()) {
                    connectedDAs.get(tmpDaName).setWhitelist(blacklist);
                }
            }
        }

        /**
         * Return all the commands supported by the Device Adapter for its devices.
         *
         * @param daId ID of the DA
         * @return A list of commands supported by the Device Adapter
         */
        @Override
        public List<String> getCommandList(String daId) throws RemoteException {
            Log.i(PAAndroidConstants.PA_LOGTAG, "Retrieving command list from Device Adapter " + daId);

            List<String> commandList = new ArrayList<String>();

            // Retrieve the right DA
            IDeviceAdapter tmpDa = daId != null ? connectedDAs.get(daId) : null;
            Capabilities cap = daId != null ? availableDAs.get(daId) : null;

            // If the Device Adapter is not connected throw an Exception
            if (tmpDa == null || cap == null) {
                throw new IllegalStateException("The Device Adapter " + daId + " is not connected to Protocol Adapter! If you are sure that the Device Adapter is connected, try increasing the time interval between the Device Adapter connection and the retrieving of command list.");
            }

            // Retrieve and return the Device Adapter's command list
            if (cap.supportCommands()) {
                commandList = tmpDa.getCommandList();
            }

            return commandList;
        }

        /**
         * Execute a command supported by the device. You can also specify a parameter, if the command
         * allows or requires it.
         *
         * @param command The command to execute on the device
         * @param parameter The optional parameter to pass to the device together with the command
         * @param devId The Device ID
         *
         * @throws IllegalArgumentException if the command is not supported by the Device Adapter
         */
        @Override
        public void execCommand(String command, String parameter, String devId) throws RemoteException {
            Log.i(PAAndroidConstants.PA_LOGTAG, "Sending command to device.\n" +
            "Device: " + devId + "\n" +
            "Command: " + command + "\n" +
            "Parameter: " + parameter);

            // Check whether the device passed as an argument is null
            if (devId == null) {
                return;
            }

            // Scan all connected DAs
            for (String tmpDaName : connectedDAs.keySet()) {

                // Retrieve the Capabilities of the DA
                Capabilities cap = availableDAs.get(tmpDaName);

                // If the DA supports sending commands to devices and is connection initiator...
                if (cap.supportCommands() && cap.isCommunicationInitiator()) {

                    List<DeviceDescription> connDevs = connectedDAs.get(tmpDaName).getConnectedDevices();

                    // ... Scan its connected devices...
                    for (DeviceDescription tmpDev : connDevs) {

                        // ... If the desired device is connected with this DA at the moment, send it the command
                        if (devId.equals(tmpDev.getDeviceID())) {
                            connectedDAs.get(tmpDaName).execCommand(command, parameter, devId);
                            break;
                        }
                    }

                }

            }
        }
    };

    /**
     * Implementation of the Device Adapter Listener API (IDeviceAdapterListener) to pass to the
     * Device Adapter
     */
    private final IDeviceAdapterListener.Stub daEndpoint = new IDeviceAdapterListener.Stub() {

        /**
         * Register a new device with the SDCS
         *
         * @param devDesc
         *      The device to register
         */
        @Override
        public void registerDevice(DeviceDescription devDesc, String daId) {
            Log.i(PAAndroidConstants.PA_LOGTAG, "Received device description: " + devDesc.toString());

            try {
                appApi.registerDevice(devDesc, daId);
            } catch (RemoteException e) {
                Log.d(PAAndroidConstants.PA_LOGTAG, "Failed to register device with application!");
            }
        }

        /**
         * Register all the property of a given device taking care of sending one separate message for
         * each property
         *
         * @param devDesc
         *      The device whom the properties belongs to
         */
        @Override
        public void registerDeviceProperties(DeviceDescription devDesc) {
            Log.i(PAAndroidConstants.PA_LOGTAG, "Received properties to register from device: " + devDesc.toString());

            try {
                appApi.registerDeviceProperties(devDesc);
            } catch (RemoteException e) {
                Log.d(PAAndroidConstants.PA_LOGTAG, "Failed to register device properties with application!");
            }
        }

        /**
         * Push the data received from the DA, taking care of sending one separate message for each
         * measurement
         *
         * @param observations
         *      The measurements to push to SDCS
         *
         * @param devDesc
         *      The device whom the observation belongs to
         */
        @Override
        public void pushData(List<Observation> observations, DeviceDescription devDesc) {
            String dataStr = "";
            for (Observation obs : observations) {
                dataStr += obs.toString() + "\n";
            }
            Log.i(PAAndroidConstants.PA_LOGTAG, "Received data to push\n" +
                    "Device: " + devDesc.getDeviceID() + "\n" +
                    "Data: \n" + dataStr);

            try {
                appApi.pushData(observations, devDesc);
            } catch (RemoteException e) {
                Log.d(PAAndroidConstants.PA_LOGTAG, "Failed to push data with application!");
            }
        }

        /**
         * Deregister the given device from the SDCS
         *
         * @param devDesc
         *      The device to use
         */
        public void deregisterDevice(DeviceDescription devDesc) {
            Log.i(PAAndroidConstants.PA_LOGTAG, "Received device deregistration: " + devDesc.getDeviceID());

            try {
                appApi.deregisterDevice(devDesc);
            } catch (RemoteException e) {
                Log.d(PAAndroidConstants.PA_LOGTAG, "Failed to deregister device with application!");
            }

        }

        /**
         * Notify a device disconnection to the upper layer
         *
         * @param devDesc The ID of the disconnected device
         */
        public void deviceDisconnected(DeviceDescription devDesc) {
            Log.i(PAAndroidConstants.PA_LOGTAG, "Received device disconnection: " + devDesc.getDeviceID());

            try {
                appApi.deviceDisconnected(devDesc);
            } catch (RemoteException e) {
                Log.d(PAAndroidConstants.PA_LOGTAG, "Failed to notify device disconnection to application!");
            }
        }

        /**
         * Receive notification from the Device Adapter of some event that happened and forward it
         * to upper layer.
         *
         * @param logLevel The severity of the event
         * @param daId The ID of the Device Adapter that generated the event
         * @param message The message associated with the event
         */
        @Override
        public void log(int logLevel, String daId, String message) throws RemoteException {
            appApi.log(logLevel, daId, message);
        }

    };

    BroadcastReceiver broadcastDiscoveryDA = new BroadcastReceiver() {
        /**
         * Receive Discovery Reply Intents from DAs and insert all info about DAs in the list of
         * available DAs
         *
         * @param context The context
         * @param intent Intent received from DA
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            // Extract DA ID and DA Capabilities from the Intent
            String daId = intent.getStringExtra(DA_DISCOVERY.BUNDLE_DAID);
            Capabilities daCap = intent.getParcelableExtra(DA_DISCOVERY.BUNDLE_DACAP);

            // Insert the newly found DA inside the list of available DAs
            if (daId != null && daCap != null) {
                availableDAs.put(daId, daCap);
                Log.i(PAAndroidConstants.PA_LOGTAG, "Found Device Adapter " + daCap.getFriendlyName() + " (" + daCap.getPackageName() + ")");
            }

        }
    };

    @Override
    public IBinder onBind(Intent intent) {

        // If this is the first start, do some initialization tasks
        if (firstStart) {
            try {
                Log.d(PAAndroidConstants.PA_LOGTAG, "Protocol Adapter v" + BuildConfig.VERSION_NAME + " starting");

                // Check who is the Issuer who started the PA
                String issuer = intent.getStringExtra(SDCS_MESSAGES.EXTRA_NAME_ISSUER);
                if (issuer == null) {
                    Log.d(PAAndroidConstants.PA_LOGTAG, "No Issuer found in activation message. Using default.");
                } else {
                    Log.d(PAAndroidConstants.PA_LOGTAG, "Service was bound, Issuer is: " + issuer);
                }

            } catch (Exception ex) {
                Log.d(PAAndroidConstants.PA_LOGTAG, ex.toString());
            }

            // Retrieve the saved values for blacklist and whitelist
            restoreFromSharedPreferences();

            // Discover DAs on the system
            discoverDAs();

            // Do not allow the repetition of this phase (though this may be redundant for bound services)
            firstStart = false;

            Log.i(PAAndroidConstants.PA_LOGTAG, "Protocol Adapter up and running.");
        } else {
            Log.i(PAAndroidConstants.PA_LOGTAG, "Protocol Adapter already up and running: not restarted.");
        }

        return appEndpoint;

    }

    @Override
    public void onDestroy() {
        // Stop and unbind from binded DAs in order to avoid ServiceConnection leak
        Set<String> das = new TreeSet<String>(connectedDAs.keySet());

        for (String tmpDA : das) {
            try {
                appEndpoint.stopDA(tmpDA);
            } catch (RemoteException e) {
                Log.d(PAAndroidConstants.PA_LOGTAG, "Error disconnecting from DA");
            } catch (IllegalArgumentException e) {
                Log.d(PAAndroidConstants.PA_LOGTAG, "No DA to disconnect from");
                Log.d(PAAndroidConstants.PA_LOGTAG, Log.getStackTraceString(e));
            }
        }

        Log.d(PAAndroidConstants.PA_LOGTAG, "Protocol Adapter terminating");
    }

    /**
     * Discover all the DA available on the system
     */
    private void discoverDAs() {
        Log.i(PAAndroidConstants.PA_LOGTAG, "Starting Device Adapter discovery");

        // Create the Intent to broadcast
        Intent intent = new Intent(DA_DISCOVERY.REQUEST_ACTION);
        intent.putExtra(DA_DISCOVERY.BUNDLE_REPACT, DA_DISCOVERY.REPLY_ACTION);

        // Create the Intent Filter to receive broadcast replies
        IntentFilter filter = new IntentFilter();
        filter.addAction(DA_DISCOVERY.REPLY_ACTION);

        // Register the Broadcast Receiver for replies and set it to run in another thread
        HandlerThread ht = new HandlerThread("ht");
        ht.start();
        registerReceiver(broadcastDiscoveryDA, filter, null, new Handler(ht.getLooper()));

        // Send in broadcast the Intent for discovery
        sendBroadcast(intent);

        // Create a new Thread object to stop the discovery and also use it as a lock
        ScanningStopAndLock r = new ScanningStopAndLock();

        // Set the timeout for receiving replies
        r.start();

        // Wait for the scanning to be done before returning
        synchronized (r) {
            while (!r.isDiscoveryDone()) {
                try {
                    r.wait();
                } catch (InterruptedException e) {
                    // Just wait until scanning is done
                }
            }
        }

    }

    /**
     * Stop the discovery of new DAs
     */
    private void stopDADiscovery() {
        // Stop receiving intents related to DA discovery
        unregisterReceiver(broadcastDiscoveryDA);

        Log.i(PAAndroidConstants.PA_LOGTAG, "Device Adapter discovery ended");
    }

    /**
     * Remove every element on this list
     *
     * @param list The list of devices
     */
    private void emptyList(List<String> list) {

        if (list == null || list.isEmpty()) return;

        for (String dev:list) {
            list.remove(dev);
        }

    }

    /**
     * Synchronize actual value of whitelist and blacklist with SharedPreferences
     */
    private void syncSharedPreferences() {
        // Get the SharedPreferences and prepare them for editing
        SharedPreferences settings = getSharedPreferences(SHPREF_FILENAME, 0);
        SharedPreferences.Editor editor = settings.edit();

        // Update the values of whitelist and blacklist in the SharedPreferences
        editor.putString(SHPREF_WHITELIST_NAME, new JSONArray(whitelist).toString());
        editor.putString(SHPREF_BLACKLIST_NAME, new JSONArray(blacklist).toString());

        // Commit the edits
        editor.commit();
    }

    /**
     * Restore in blacklist and whitelist every device saved in the SharedPreferences
     */
    private void restoreFromSharedPreferences() {
        JSONArray jWhitelist, jBlacklist;

        // Get access to the right SharedPreferences file
        SharedPreferences shPref = getSharedPreferences(SHPREF_FILENAME, 0);

        // Retrieve the whitelist and if there's any problem, create a new empty JSONArray
        try {
            jWhitelist = new JSONArray(shPref.getString(SHPREF_WHITELIST_NAME, "[]"));
        } catch (JSONException e) {
            jWhitelist = new JSONArray();
        }

        // Retrieve the blacklist and if there's any problem, create a new empty JSONArray
        try {
            jBlacklist = new JSONArray(shPref.getString(SHPREF_BLACKLIST_NAME, "[]"));
        } catch (JSONException e) {
            jBlacklist = new JSONArray();
        }

        // Add to whitelist every element present in JSONArray retrieved from SharedPreferences
        for (int i = 0; i < jWhitelist.length(); i++) {
            try {
                whitelist.add(jWhitelist.getString(i));
            } catch (JSONException e) {
                Log.d(PAAndroidConstants.PA_LOGTAG, "Failed restoring whitelist from SharedPreferences");
            }
        }

        // Add to blacklist every element present in JSONArray retrieved from SharedPreferences
        for (int i = 0; i < jBlacklist.length(); i++) {
            try {
                blacklist.add(jBlacklist.getString(i));
            } catch (JSONException e) {
                Log.d(PAAndroidConstants.PA_LOGTAG, "Failed restoring blacklist from SharedPreferences");
            }
        }
    }

    /**
     * Start all the device adapters present in the the DA's array
     */
    @SuppressWarnings("unused")
    private void startDeviceAdapters() {
        // TODO Issue #7 Decide if startDeviceAdapters method should be promoted and put inside IProtocolAdapter interface
        Log.i(PAAndroidConstants.PA_LOGTAG, "Starting all Device Adapters");

        // Start all Device Adapters that are installed
        for (Capabilities daCap : availableDAs.values()) {
            try {
                appEndpoint.startDA(daCap.getActionName());
            } catch (RemoteException e) {
                Log.w(PAAndroidConstants.PA_LOGTAG, "Device Adapter " + daCap.getFriendlyName() + " (" + daCap.getPackageName() + ") failed to start!");
            }
        }

    }

    private class ScanningStopAndLock extends Thread {
        private boolean done = false;

        @Override
        public void run() {
            try {
                Thread.sleep(DA_DISCOVERY.TIMEOUT);
            } catch (InterruptedException e) {
                // Do nothing
            }
            stopDADiscovery();
            synchronized (this) {
                done = true;
                notifyAll();
            }
        }

        public boolean isDiscoveryDone() {
            return done;
        }
    }

    /**
     * Endpoint for managing the connection/disconnection of the Device Adapters
     */
    private class DAConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {

            // Retrieve the Device Adapter from the binder
            String daId = componentName.getPackageName();
            IDeviceAdapter tmpDa = IDeviceAdapter.Stub.asInterface(iBinder);

            // Add the Device Adapter to the List
            connectedDAs.put(daId, tmpDa);
            daConnections.put(daId, this);
            Log.i(PAAndroidConstants.PA_LOGTAG, "New Device Adapter connected: " + daId);

            try {
                // Register the Protocol Adapter into the Device Adapter
                tmpDa.registerDAListener(daEndpoint);
            } catch (RemoteException e) {
                Log.d(PAAndroidConstants.PA_LOGTAG, "Failed to register PA inside DA " + daId);
            }

            try {
                // Get the Capabilities of the DA
                Capabilities cap = tmpDa.getDACapabilities();

                // Restore the blacklist inside the newly connected DA if it's supported
                if (cap.hasBlacklist()) {
                    tmpDa.setBlackList(blacklist);
                }

                // Restore the whitelist inside the newly connected DA if it's supported
                if (cap.hasWhitelist()) {
                    tmpDa.setWhitelist(whitelist);
                }

                // Start the newly connected DA
                tmpDa.start();

                // Notify the Application that the DA has finished its initialization phase
                appApi.onDAConnected(daId);

            } catch (RemoteException e) {
                Log.d(PAAndroidConstants.PA_LOGTAG, "Failed to start DA " + daId);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

            // Get the DA ID
            String daId = componentName.getPackageName();

            Log.i(PAAndroidConstants.PA_LOGTAG, daId + " Device Adapter service disconnected.");

            // Remove the Device Adapter from the DA List
            connectedDAs.remove(daId);

            try {
                // Try to start the DA again. This way, the DA will be added again to the list of
                // connected DAs when onServiceConnected is called
                appEndpoint.startDA(daId);
                Log.i(PAAndroidConstants.PA_LOGTAG, "Restarting Device Adapter " + daId);
            } catch (RemoteException ex) {
                Log.e(PAAndroidConstants.PA_LOGTAG, daId + " Device Adapter failed to connect.");
            }

        }

    }
}
