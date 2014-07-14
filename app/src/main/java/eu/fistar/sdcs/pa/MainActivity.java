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


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;


/**
 * This is just an example activity used to start the Protocol Adapter directly in case it's not
 * started elsewhere. You can start the Protocol Adapter from here but it can be started e.g. by the
 * Sensor Data Collection Service SE
 *
 * @author Marcello Morena
 * @author Alexandru Serbanati
 */
public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void startService(View v) {

        // Create fake SDCS receiver
        IntentFilter iff = new IntentFilter(PAAndroidConstants.SDCS.ACTION);

        BroadcastReceiver myBroadcastReceiver = new BroadcastReceiver() {
            private TextView t = null;

            @Override
            public void onReceive(Context context, Intent intent) {

                // Get the output TextView
                t=((TextView) findViewById(R.id.lblServiceFeedback));

                // Extract info from Intent
                String str = intent.getStringExtra("fromPA");

                if (intent.getStringExtra("fromPA") != null) {
                    t.setText("Service Started: " + str);
                } else {
                    Log.d("ExampleActivity", "Payload returned from Service is null");
                }

            }
        };

        // Register Broadcast Listener
        LocalBroadcastManager.getInstance(this).registerReceiver(myBroadcastReceiver, iff);

        // Create the Intent to start the PA with
        Intent i = new Intent(this, PAManagerService.class);
        i.putExtra(PAAndroidConstants.SDCS_MESSAGES.EXTRA_NAME_ISSUER, "Example Activity");

        // Start the Protocol Adapter
        this.startService(i);

    }


}
