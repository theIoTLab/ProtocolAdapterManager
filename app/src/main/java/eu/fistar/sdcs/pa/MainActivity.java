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


import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;
import java.util.Map;
import java.util.Set;

import eu.fistar.sdcs.pa.common.Capabilities;
import eu.fistar.sdcs.pa.common.DeviceDescription;
import eu.fistar.sdcs.pa.common.IProtocolAdapter;
import eu.fistar.sdcs.pa.common.IProtocolAdapterListener;
import eu.fistar.sdcs.pa.common.Observation;
import eu.fistar.sdcs.pa.common.PAAndroidConstants;


/**
 * This is just an example activity used to bind the Protocol Adapter directly in case it's not
 * bounded elsewhere. Remember that you can bind the Protocol Adapter from here but it should be
 * directly bounded by the software that uses it e.g. by the Sensor Data Collection Service SE.
 *
 * @author Marcello Morena
 * @author Alexandru Serbanati
 */
public class MainActivity extends Activity {

    private static final String LOGTAG = "PA Activity";

    private IProtocolAdapter pa;
    private boolean toggle = false;

    private ServiceConnection serv = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            // Get the output TextView
            TextView t=((TextView) findViewById(R.id.lblServiceFeedback));
            t.setText("Protocol Adapter started!");

            // Disable the start button
            Button bStart = ((Button) findViewById(R.id.btnStartService));
            bStart.setEnabled(false);

            // Enable the stop button
            Button bStop = ((Button) findViewById(R.id.btnStopService));
            bStop.setEnabled(true);

            // Start all the Device Adapters of the system
            pa = IProtocolAdapter.Stub.asInterface(iBinder);
            try {
                // Registering listener
                pa.registerPAListener(paListener);

                // Start all the Device Adapters of the system
                @SuppressWarnings("unchecked")
                Map<String, Capabilities> das = (Map<String, Capabilities>) pa.getAvailableDAs();

                Log.d(LOGTAG, "Starting all Device Adapters of the system");

                for (String tempDa : das.keySet()) {
                    pa.startDA(tempDa);
                }

            } catch (RemoteException e) {
                Log.d(LOGTAG, "Error contacting Protocol Adapter");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(LOGTAG, "Protocol Adapter disconnected unexpectedly");
        }
    };

    /**
     * Dummy implementation of the IProtocolAdapterListener interface which just prints on the logs
     * all the data received by the Protocol Adapter.
     */
    private final IProtocolAdapterListener.Stub paListener = new IProtocolAdapterListener.Stub() {
        @Override
        public void registerDevice(DeviceDescription deviceDescription) throws RemoteException {
            Log.d(LOGTAG,  "Device registered " + deviceDescription.getDeviceID());
        }

        @Override
        public void pushData(List<Observation> observations, DeviceDescription deviceDescription) throws RemoteException {
            Log.d(LOGTAG,  "Data pushed from device " + deviceDescription.getDeviceID());
        }

        @Override
        public void deregisterDevice(DeviceDescription deviceDescription) throws RemoteException {
            Log.d(LOGTAG,  "Device deregistered " + deviceDescription.getDeviceID());
        }

        @Override
        public void registerDeviceProperties(DeviceDescription deviceDescription) throws RemoteException {
            Log.d(LOGTAG, "Device properties registered: " + deviceDescription.getDeviceID());
        }

        @Override
        public void deviceDisconnected(DeviceDescription deviceDescription) throws RemoteException {
            Log.d(LOGTAG, "Device disconnected: " + deviceDescription.getDeviceID());
        }

        @Override
        public void log(int logLevel, String daId, String message) throws RemoteException {
            Log.d(LOGTAG, "LOG! Level: " + logLevel + "; DA: " + daId + "; Message: " + message + ";");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void startService(View v) {
        // Create the Intent to start the PA with
        Intent intent = new Intent().setComponent(new ComponentName(PAAndroidConstants.PA_PACKAGE, PAAndroidConstants.PA_ACTION));

        // Start the Protocol Adapter
        bindService(intent, serv, Context.BIND_AUTO_CREATE);
    }

    public void stopService(View v) {
        // Unbind the service
        this.unbindService(serv);

        // Get the output TextView
        TextView t=((TextView) findViewById(R.id.lblServiceFeedback));
        t.setText("Protocol Adapter in unknown status");

        // Disable the start button
        Button bStart = ((Button) findViewById(R.id.btnStartService));
        bStart.setEnabled(true);

        // Enable the stop button
        Button bStop = ((Button) findViewById(R.id.btnStopService));
        bStop.setEnabled(false);
    }

    public void connectDevices(View v) {

        try {
            // Connect to all devices of the system
            @SuppressWarnings("unchecked")
            Set<String> devices = pa.getDADevices().keySet();

            if (!devices.isEmpty()) {
                for (String tmpDev : devices) {
                    Log.d(LOGTAG, "Connecting to device: " + tmpDev);
                    pa.connectDev(tmpDev);
                }
            } else {
                Log.d(LOGTAG, "No device found!");
            }
        } catch (RemoteException e) {
            Log.d(LOGTAG, "Error communicating wih the PA");
        }
    }

    /**
     * Toggle configuration for the Zephyr BioHarness 3.
     *
     * @param v
     */
    public void toggleConfig(View v) {
        try {
            if (toggle) {
                pa.execCommand("disableGeneralData", "", "C8:3E:99:0D:DC:26");
                pa.execCommand("disableAccelerometerData", "", "C8:3E:99:0D:DC:26");
                pa.execCommand("disableBreathingData", "", "C8:3E:99:0D:DC:26");
                pa.execCommand("disableRtoRData", "", "C8:3E:99:0D:DC:26");
            } else {
                pa.execCommand("enableGeneralData", "", "C8:3E:99:0D:DC:26");
                pa.execCommand("enableAccelerometerData", "", "C8:3E:99:0D:DC:26");
                pa.execCommand("enableBreathingData", "", "C8:3E:99:0D:DC:26");
                pa.execCommand("enableRtoRData", "", "C8:3E:99:0D:DC:26");
            }
            toggle = !toggle;
        } catch (RemoteException e) {
            Log.d(LOGTAG, "Error executing commands");
        }
    }

}
