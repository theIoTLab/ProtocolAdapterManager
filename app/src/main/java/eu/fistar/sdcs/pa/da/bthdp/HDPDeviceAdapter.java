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

package eu.fistar.sdcs.pa.da.bthdp;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;

import eu.fistar.sdcs.pa.IDeviceDescription;
import eu.fistar.sdcs.pa.PAAndroidConstants;
import eu.fistar.sdcs.pa.da.IDeviceAdapter;
import eu.fistar.sdcs.pa.da.bthdp.hdpservice.HealthAgentAPI;
import eu.fistar.sdcs.pa.da.bthdp.hdpservice.HealthServiceAPI;
import eu.fistar.sdcs.pa.IProtocolAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is the HDP Device Adapter. It is a bound service which can be bound from the Protocol
 * Adapter, implements the Device Adapter interface and takes care of communicating with and
 * managing all the HDP devices. It makes use of a ISO 11073 Manager (which is a modified version
 * of Antidote, developed by Signove) to communicate with the HDP Devices.
 *
 * @author Marcello Morena
 * @author Alexandru Serbanati
 */
public class HDPDeviceAdapter extends Service implements IDeviceAdapter {

    private final static String SERVICE_HDP_MANAGER_NAME = "eu.fistar.sdcs.pa.da.bthdp.hdpservice.HDPHealthManagerService";
    private final static String LOGTAG_HDP_SERVICE = "HDP >>>";

    private final HDPBinder daBinder = new HDPBinder();
    private HealthServiceAPI hdpApi;
    private IProtocolAdapter paApi;

    private Handler handler;
    private final Map<String, HDPDevice> devices = new HashMap<String, HDPDevice>();


    /**
     * Implementation of the ServiceConnection object used to
     * handle the connection to the service
     */
    private ServiceConnection serviceHDPManagerConnection = new ServiceConnection() {

        /**
         * Called when connected to {@link eu.fistar.sdcs.pa.da.bthdp.hdpservice.HDPHealthManagerService}
         *
         * @param name
         *      Component Name of the service
         * @param service
         *      The binder returned by the service
         */
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            Log.w(LOGTAG_HDP_SERVICE, "Service connected");

            // Get the object to use the IPC API of the service
            hdpApi = HealthServiceAPI.Stub.asInterface(service);
            try {
                // Call the method to initiate the communication with the HDP device
                hdpApi.ConfigurePassive(agent, null);
            } catch (RemoteException e) {
            }
        }

        /**
         * Gracefully handle the service disconnection
         *
         * @param name
         *      Component Name of the service
         */
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.w(LOGTAG_HDP_SERVICE, "Service connection closed");
        }
    };


    /**
     * Implementation of the HealthAgentAPI, which is needed to allow
     * the service to contact back this activity and notify it of some
     * interesting situations, such as Connection, Association,
     * Disassociation, etc, of a device. Health Agent is the user of
     * the service and should not be confused with ISO 11073 Agent.
     */
    private HealthAgentAPI.Stub agent = new HealthAgentAPI.Stub() {

        /**
         * Called back by the ISO 11073 Manager when a device is connected. Create a new HDPDevice
         * and put it in the HashMap
         *
         * @param dev
         *      The String that identifies the device inside the ISO 11073 Manager
         *
         * @param addr
         *      The MAC address of the bluetooth device that has just connected
         */
        @Override
        public void Connected(String dev, String addr) {
            Log.d(LOGTAG_HDP_SERVICE, "Device "+dev+" connected");
            devices.put(dev, new HDPDevice(addr));
        }

        /**
         * Called back by the ISO 11073 Manager when a device is associated
         *
         * @param dev
         *      The String that identifies the device inside the ISO 11073 Manager
         *
         * @param assocXmlData
         *      The raw XML data generated by the ISO 11073 Manager containing data from the device
         */
        @Override
        public void Associated(String dev, String assocXmlData) {

            Log.d(LOGTAG_HDP_SERVICE, "Device "+dev+" associated");

            final String idev = dev;

            // Prepare the request for the device attributes
            Runnable req = new Runnable() {
                public void run() {
                    RequestDeviceAttributes(idev);
                }
            };

            // Enqueue the requests in the Handler
            handler.postDelayed(req, 500);
        }

        /**
         * Called back by the ISO 11073 Manager when the the measurement data is returned
         *
         * @param dev
         *      The String that identifies the device inside the ISO 11073 Manager
         *
         * @param measurementXmlData
         *      The raw XML data generated by the ISO 11073 Manager containing data from the device
         */
        @Override
        public void MeasurementData(String dev, String measurementXmlData) {

            Log.d(LOGTAG_HDP_SERVICE, "Received measurement from device "+dev);

            // Retrieve the device from the list
            HDPDevice hdpDev = devices.get(dev);

            if (hdpDev != null && hdpDev.isRegistered()) {
                // Parse XML measurement data
                HDPObservation[] observations = HDPXMLUtils.parseMeasurementData(hdpDev, measurementXmlData);

                // Log the value of measurements
                String obsStr = "";
                for (HDPObservation obs : observations) {
                    obsStr += obs.toString();
                }
                Log.d(PAAndroidConstants.DA_LOGTAG, "Device: "+hdpDev.getDeviceID()+"\nObservations:\n"+obsStr+"\n");

                // Push measurements to Protocol Adapter
                paApi.push_Data(observations, hdpDev);
            }

        }

        /**
         * Called back by the ISO 11073 Manager when device attributes are received
         *
         * @param dev
         *      The String that identifies the device inside the ISO 11073 Manager
         *
         * @param attrXmlData
         *      The raw XML data generated by the ISO 11073 Manager containing data from the device
         */
        @Override
        public void DeviceAttributes(String dev, String attrXmlData) {

            Log.v(LOGTAG_HDP_SERVICE, "Received attributes from device "+dev);

            String confXmlData = "";
            HDPDevice hdpDev;

            // Retrieve device configuration
            try {
                // Call the API method to retrieve the configuration
                confXmlData = hdpApi.GetConfiguration(dev);
                Log.v(LOGTAG_HDP_SERVICE, "Got configuration from device "+dev);

            } catch (RemoteException e) {
                Log.d(LOGTAG_HDP_SERVICE, "Exception on receiving configuration from device "+dev + ".\n" + e.toString());
            }

            hdpDev = devices.get(dev);

            // Parse attributes and configuration data and populate the device object
            HDPXMLUtils.parseAttrData(hdpDev, attrXmlData);
            HDPXMLUtils.parseConfData(hdpDev, confXmlData);

            // Register the device to the Protocol Adapter
            Log.v(PAAndroidConstants.DA_LOGTAG, hdpDev.toString());
            paApi.register_Device(hdpDev);

            // Mark the device as registered
            hdpDev.setRegistered(true);

        }

        /**
         * Called back by the ISO 11073 Manager when a device is disassociated
         *
         * @param dev
         *      The String that identifies the device inside the ISO 11073 Manager
         */
        @Override
        public void Disassociated(String dev) {
            Log.d(LOGTAG_HDP_SERVICE, "Device "+dev+" disassociated");
        }

        /**
         * Called back by the ISO 11073 Manager when a device is disconnected
         *
         * @param dev
         *      The String that identifies the device inside the ISO 11073 Manage
         */
        @Override
        public void Disconnected(String dev) {

            HDPDevice hdpDev;

            // Remove device from HashMap
            hdpDev = devices.remove(dev);

            if (hdpDev != null) {
                // Deregister device from Protocol Adapter
                paApi.deregister_Device(hdpDev);

                // Mark the device as unregistered
                hdpDev.setRegistered(false);

                Log.d(LOGTAG_HDP_SERVICE, "Device " + dev + " disconnected");
            }
        }
    };

    /**
     * Request the device attributes from the device attributes
     *
     * @param dev
     *      The String that identifies the device inside the ISO 11073 Manage
     */
    private void RequestDeviceAttributes(String dev)
    {
        try {
            // Call the API method to request the device attributes
            hdpApi.RequestDeviceAttributes(dev);

        } catch (RemoteException e) {
            Log.v(LOGTAG_HDP_SERVICE, "Warning: Remote Exception while requesting device Attributes" + e.toString());
        }
    }

    @Override
    public void onCreate() {

        handler = new Handler();

        // Create the Intent to start the service, start the service and bind to it
        Intent intent = new Intent(SERVICE_HDP_MANAGER_NAME);

        // Decomment this line just in case of issue with service starting
        Log.d(LOGTAG_HDP_SERVICE, "Starting HDP Manager Service");
        startService(intent);

        Log.d(LOGTAG_HDP_SERVICE, "Connecting to HDP Manager Service");
        bindService(intent, serviceHDPManagerConnection, 0);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            // Deregister the application
            hdpApi.Unconfigure(agent);
        } catch (Throwable t) {
        }
        // Unbind from the service
        unbindService(serviceHDPManagerConnection);

    }

    @Override
    public IBinder onBind(Intent intent) {
        return daBinder;
    }


    /**
     * This class is used to facilitate communication with the Protocol Adapter, allowing the PA to
     * retrieve the Device Adapter's instance
     */
    public class HDPBinder extends DABinder {

        public IDeviceAdapter getDeviceAdapter() {
            // Return this instance of the Device Adapter
            return HDPDeviceAdapter.this;
        }

    }


    /**
     * Returns the list of all the devices that are actually connected to the Device Adapter
     *
     * @return
     *      The list of all the device active now inside the Device Adapter
     */
    @Override
    public List<IDeviceDescription> getDevices() {
        return new ArrayList<IDeviceDescription>(devices.values());
    }


    /**
     * Set the Protocol Adapter API endpoint
     *
     * @param pa
     *      The Protocol Adapter Binder
     */
    public void RegisterPA(IProtocolAdapter.PABinder pa) {
        paApi = pa.getProtocolAdapter();
    }

}
