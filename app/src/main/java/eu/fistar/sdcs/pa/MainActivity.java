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


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.fistar.sdcs.pa.common.Capabilities;
import eu.fistar.sdcs.pa.common.DeviceDescription;
import eu.fistar.sdcs.pa.common.IProtocolAdapter;
import eu.fistar.sdcs.pa.common.IProtocolAdapterListener;
import eu.fistar.sdcs.pa.common.Observation;
import eu.fistar.sdcs.pa.common.PAAndroidConstants;
import eu.fistar.sdcs.pa.dialogs.ConfigDaDialogFragment;
import eu.fistar.sdcs.pa.dialogs.ConnectDevDialogFragment;
import eu.fistar.sdcs.pa.dialogs.DisconnectDevDialogFragment;
import eu.fistar.sdcs.pa.dialogs.IPADialogListener;
import eu.fistar.sdcs.pa.dialogs.SendCommandDialogFragment;
import eu.fistar.sdcs.pa.dialogs.StartDaDialogFragment;
import eu.fistar.sdcs.pa.dialogs.StopDaDialogFragment;


/**
 * This is just an example activity used to bind the Protocol Adapter directly in case it's not
 * bounded elsewhere. Remember that you can bind the Protocol Adapter from here but it should be
 * directly bounded by the software that uses it e.g. by the Sensor Data Collection Service SE.
 *
 * @author Marcello Morena
 * @author Alexandru Serbanati
 */
public class MainActivity extends FragmentActivity implements IPADialogListener {

    private static final String LOGTAG = "PA Activity";

    private IProtocolAdapter pa;

    private ServiceConnection serv = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            // Get the output TextView
            ((TextView) findViewById(R.id.lblServiceFeedback)).setText(getText(R.string.lbl_feedback_started));

            // Disable the start button
            (findViewById(R.id.btnStartService)).setEnabled(false);

            // Enable the stop button
            (findViewById(R.id.btnStopService)).setEnabled(true);

            // Enable the stop button
            (findViewById(R.id.btnStopService)).setEnabled(true);

            // Enable the start DA button
            (findViewById(R.id.btnStartDA)).setEnabled(true);

            // Enable the stop DA button
            (findViewById(R.id.btnStopDA)).setEnabled(true);

            // Enable the config DA button
            (findViewById(R.id.btnConfigDA)).setEnabled(true);

            // Enable the connect device button
            (findViewById(R.id.btnConnectDevices)).setEnabled(true);

            // Enable the disconnect device button
            (findViewById(R.id.btnDisconnectDevices)).setEnabled(true);

            // Enable the send command button
            (findViewById(R.id.btnSendCommand)).setEnabled(true);

            // Start all the Device Adapters of the system
            pa = IProtocolAdapter.Stub.asInterface(iBinder);
            try {
                // Registering listener
                pa.registerPAListener(paListener);

                // Start all the Device Adapters of the system
                @SuppressWarnings("unchecked")
                Map<String, Capabilities> das = (Map<String, Capabilities>) pa.getAvailableDAs();

                updateLog("Starting all Device Adapters of the system");

                for (String tempDa : das.keySet()) {
                    pa.startDA(tempDa);
                }

            } catch (RemoteException e) {
                updateLog("Error contacting Protocol Adapter");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            updateLog("Protocol Adapter disconnected unexpectedly");
        }
    };

    /**
     * Dummy implementation of the IProtocolAdapterListener interface which just prints on the logs
     * all the data received by the Protocol Adapter.
     */
    private final IProtocolAdapterListener.Stub paListener = new IProtocolAdapterListener.Stub() {
        @Override
        public void registerDevice(DeviceDescription deviceDescription, String daId) throws RemoteException {
            updateLog("Device registered " + deviceDescription.getDeviceID() + ", handled by Device Adapter " + daId);
        }

        @Override
        public void pushData(List<Observation> observations, DeviceDescription deviceDescription) throws RemoteException {
            updateLog("Data pushed from device " + deviceDescription.getDeviceID());
        }

        @Override
        public void deregisterDevice(DeviceDescription deviceDescription) throws RemoteException {
            updateLog("Device deregistered " + deviceDescription.getDeviceID());
        }

        @Override
        public void registerDeviceProperties(DeviceDescription deviceDescription) throws RemoteException {
            updateLog("Device properties registered: " + deviceDescription.getDeviceID());
        }

        @Override
        public void deviceDisconnected(DeviceDescription deviceDescription) throws RemoteException {
            updateLog("Device disconnected: " + deviceDescription.getDeviceID());
        }

        @Override
        public void log(int logLevel, String daId, String message) throws RemoteException {
            updateLog("LOG! Level: " + logLevel + "; DA: " + daId + "; Message: " + message + ";");
        }

        @Override
        public void onDAConnected(String daId) throws RemoteException {
            updateLog("Device Adapter " + daId + " completed the initialization phase");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void startService(View v) {

        updateLog("Starting the Protocol Adapter");

        // Create the Intent to start the PA with
        Intent intent = new Intent().setComponent(new ComponentName(PAAndroidConstants.PA_PACKAGE, PAAndroidConstants.PA_ACTION));

        // Start the Protocol Adapter
        bindService(intent, serv, Context.BIND_AUTO_CREATE);
    }

    public void stopService(View v) {

        updateLog("Stopping the Protocol Adapter");

        // Unbind the service
        this.unbindService(serv);

        // Get the output TextView
        ((TextView) findViewById(R.id.lblServiceFeedback)).setText(getText(R.string.lbl_feedback_unknown));

        // Disable the start button
        (findViewById(R.id.btnStartService)).setEnabled(true);

        // Disable the stop button
        (findViewById(R.id.btnStopService)).setEnabled(false);

        // Disable the start DA button
        (findViewById(R.id.btnStartDA)).setEnabled(false);

        // Disable the stop DA button
        (findViewById(R.id.btnStopDA)).setEnabled(false);

        // Disable the config DA button
        (findViewById(R.id.btnConfigDA)).setEnabled(false);

        // Disable the connect device button
        (findViewById(R.id.btnConnectDevices)).setEnabled(false);

        // Disable the disconnect device button
        (findViewById(R.id.btnDisconnectDevices)).setEnabled(false);

        // Disable the send command button
        (findViewById(R.id.btnSendCommand)).setEnabled(false);
    }

    @Override
    public void connectDev(String devId, String daId) {
        try {
            // Connect to the specified device
            updateLog("Connecting to device: " + devId);
            pa.forceConnectDev(devId, daId);
        } catch (RemoteException e) {
            updateLog("Error communicating wih the PA");
        }
    }

    @Override
    public void disconnectDev(String devId) {
        try {
            // Disconnect from the specified device
            updateLog("Disconnecting from device: " + devId);
            pa.disconnectDev(devId);
        } catch (RemoteException e) {
            updateLog("Error communicating wih the PA");
        }
    }

    @Override
    public void sendCommand(String command, String parameter, String devId) {
        try {
            pa.execCommand(command, parameter != null ? parameter : "", devId);
        } catch (RemoteException e) {
            updateLog("Error executing command");
        }
    }

    @Override
    public void configDa(ComponentName comp) {
        Intent intent = new Intent().setComponent(comp);
        startActivity(intent);
    }

    @Override
    public void startDA(String daId) {
        try {
            updateLog("Starting the Device Adapter " + daId);
            pa.startDA(daId);
        } catch (RemoteException e) {
            updateLog("Error starting the Device Adapter " + daId);
        }
    }

    @Override
    public void stopDA(String daId) {
        try {
            updateLog("Stopping the Device Adapter " + daId);
            pa.stopDA(daId);
        } catch (RemoteException e) {
            updateLog("Error stopping the Device Adapter " + daId);
        }
    }

    public void parseClick(View v) {
        switch(v.getId()) {
            case R.id.btnStartDA:
                // Start the dialog to start the DA
                try {
                    List<String> availableDa = new ArrayList<String>(pa.getAvailableDAs().keySet());
                    StartDaDialogFragment startDaFrag = StartDaDialogFragment.newInstance(availableDa);
                    startDaFrag.show(getFragmentManager(), "startda");
                } catch (RemoteException e) {
                    updateLog("Failed to show the dialog to start the DAs");
                }
                break;
            case R.id.btnStopDA:
                // Start the dialog to stop the DA
                try {
                    List<String> availableDa = new ArrayList<String>(pa.getAvailableDAs().keySet());
                    StopDaDialogFragment stopDaFrag = StopDaDialogFragment.newInstance(availableDa);
                    stopDaFrag.show(getFragmentManager(), "stopda");
                } catch (RemoteException e) {
                    updateLog("Failed to show the dialog to stop the DAs");
                }
                break;
            case R.id.btnConnectDevices:
                // Start the dialog to connect to a device
                try {
                    if (!pa.getDADevices().isEmpty()) {
                        ConnectDevDialogFragment connDevFrag = ConnectDevDialogFragment.newInstance(pa.getDADevices());
                        connDevFrag.show(getFragmentManager(), "connectDev");
                    }
                    else {
                        Toast.makeText(this, "No devices available", Toast.LENGTH_SHORT).show();
                    }
                } catch (RemoteException e) {
                    updateLog("Failed to show the dialog to connect a device");
                }
                break;
            case R.id.btnSendCommand:
                // Start the dialog to send command
                try {
                    Map<String, List<String>> devicesDa = pa.getDADevices();
                    Map<String, List<String>> devicesCommands = new HashMap<>();

                    for (DeviceDescription tmpDev : pa.getConnectedDevices()) {
                        String devId = tmpDev.getDeviceID();
                        String daId = devicesDa.get(tmpDev.getDeviceID()).get(0);
                        List<String> commands = pa.getCommandList(daId);
                        devicesCommands.put(devId, commands);
                    }

                    if (!devicesCommands.isEmpty()) {
                        // Start the dialog to chose the DA to configure
                        SendCommandDialogFragment sendCommandFrag = SendCommandDialogFragment.newInstance(devicesCommands);
                        sendCommandFrag.show(getFragmentManager(), "sendCommand");
                    }
                    else {
                        Toast.makeText(this, "No device connected", Toast.LENGTH_SHORT).show();
                    }

                } catch (RemoteException e) {
                    updateLog("Failed to show the dialog to send a command");
                }
                break;
            case R.id.btnDisconnectDevices:
                // Start the dialog to disconnect from a device
                try {
                    List<DeviceDescription> connDev = pa.getConnectedDevices();
                    if (!connDev.isEmpty()) {
                        List<String> connDevId = new ArrayList<String>();
                        for (DeviceDescription tmpDev : connDev) {
                            connDevId.add(tmpDev.getDeviceID());
                        }
                        DisconnectDevDialogFragment disconnDevFrag = DisconnectDevDialogFragment.newInstance(connDevId);
                        disconnDevFrag.show(getFragmentManager(), "disconnectDev");
                    }
                    else {
                        Toast.makeText(this, "No devices connected", Toast.LENGTH_SHORT).show();
                    }
                } catch (RemoteException e) {
                    updateLog("Failed to show the dialog to disconnect from a device");
                }
                break;
            case R.id.btnConfigDA:
                // Start the dialog to chose the DA to configure
                try {
                    Map<String, Capabilities> availableDas = pa.getAvailableDAs();
                    Map<String, ComponentName> daConfigActivities = new HashMap<String, ComponentName>();
                    for (Capabilities tmpDaCap : availableDas.values()) {
                        if (tmpDaCap.isGuiConfigurable()) {
                            daConfigActivities.put(tmpDaCap.getDaId(), tmpDaCap.getConfigActivityName());
                        }
                    }
                    if (!daConfigActivities.isEmpty()) {
                        ConfigDaDialogFragment configDaFrag = ConfigDaDialogFragment.newInstance(daConfigActivities);
                        configDaFrag.show(getFragmentManager(), "configDa");
                    }
                    else {
                        Toast.makeText(this, "No configurable DA available", Toast.LENGTH_SHORT).show();
                    }
                } catch (RemoteException e) {
                    updateLog("Failed to show the dialog to configure the DA");
                }
                break;
        }
    }

    public void clearLogs(View v) {
        ((TextView) findViewById(R.id.logBox)).setText("");
    }

    private void updateLog(String log) {
        Log.d(LOGTAG, log);

        final String fLog = log;

        final TextView tv = (TextView) findViewById(R.id.logBox);
        tv.post(new Runnable() {
            @Override
            public void run() {
                tv.append(fLog + "\n");
            }
        });

        final ScrollView sv = (ScrollView) findViewById(R.id.scrollBox);
        sv.postDelayed(new Runnable() {
            @Override
            public void run() {
                sv.fullScroll(ScrollView.FOCUS_DOWN);
            }
        }, 100L);

    }

}
