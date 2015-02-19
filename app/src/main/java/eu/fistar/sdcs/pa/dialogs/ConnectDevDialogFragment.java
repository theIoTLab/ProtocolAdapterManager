package eu.fistar.sdcs.pa.dialogs;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import eu.fistar.sdcs.pa.R;

public class ConnectDevDialogFragment extends DialogFragment implements AdapterView.OnItemSelectedListener {

    // DevID, List of DA ID
    private Map<String, List<String>> devices;

    private IPADialogListener mListener;

    private String devId = null;
    private String daId = null;

    public static ConnectDevDialogFragment newInstance(Map<String, List<String>> devices) {
        ConnectDevDialogFragment fragment = new ConnectDevDialogFragment();
        fragment.devices = devices;
        return fragment;
    }

    public ConnectDevDialogFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        final View actualView = inflater.inflate(R.layout.fragment_connect_dev_dialog, null);

        builder.setView(actualView)
                .setTitle(R.string.connect_dev)
                // Add action buttons
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        if (devId != null && daId != null) {
                            mListener.connectDev(devId, daId);
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        ConnectDevDialogFragment.this.getDialog().cancel();
                    }
                });

        Spinner devSpin = (Spinner) actualView.findViewById(R.id.spin_device);
        devSpin.setOnItemSelectedListener(ConnectDevDialogFragment.this);

        ArrayAdapter<String> devSpinAdapter = new ArrayAdapter<String>(ConnectDevDialogFragment.this.getActivity(), android.R.layout.simple_spinner_item, populateDevices(devices.keySet()));
        devSpinAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        devSpin.setAdapter(devSpinAdapter);

        Spinner daSpin = (Spinner) actualView.findViewById(R.id.spin_da);
        daSpin.setOnItemSelectedListener(ConnectDevDialogFragment.this);

        return builder.create();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (IPADialogListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement IPADialogListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        switch (parent.getId()) {
            case R.id.spin_device:
                String devName = ((TextView) view).getText().toString();
                devName = devName.substring(devName.indexOf("(") + 1);
                devId = devName.substring(0, devName.indexOf(")"));

                Spinner daSpin = (Spinner) getDialog().findViewById(R.id.spin_da);

                ArrayAdapter<String> daSpinAdapter = new ArrayAdapter<String>(ConnectDevDialogFragment.this.getDialog().getContext(), android.R.layout.simple_spinner_item, devices.get(devId));
                daSpinAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                daSpin.setAdapter(daSpinAdapter);
                break;
            case R.id.spin_da:
                daId = ((TextView) view).getText().toString();
                break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    private List<String> populateDevices(Set<String> devices) {
        List<String> devNames = new ArrayList<String>();
        for (String addr : devices) {
            String devName = getFriendlyName(addr) + " (" + addr +")";
            devNames.add(devName);
        }
        return devNames;
    }

    private String getFriendlyName(String addr) {
        BluetoothAdapter bt = BluetoothAdapter.getDefaultAdapter();
        if (bt != null) {
            return bt.getRemoteDevice(addr).getName();
        }
        else return null;
    }
}
