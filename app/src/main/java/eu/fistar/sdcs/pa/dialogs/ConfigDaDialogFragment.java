package eu.fistar.sdcs.pa.dialogs;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.os.Bundle;

import java.util.Map;

import eu.fistar.sdcs.pa.R;

public class ConfigDaDialogFragment extends DialogFragment {
    // DevID, Config Activity
    private Map<String, ComponentName> daConfigActivities;
    private CharSequence[] daIds;

    private IPADialogListener mListener;

    public static ConfigDaDialogFragment newInstance(Map<String, ComponentName> daConfigActivities) {
        ConfigDaDialogFragment fragment = new ConfigDaDialogFragment();
        fragment.daConfigActivities = daConfigActivities;
        fragment.daIds = daConfigActivities.keySet().toArray(new CharSequence[daConfigActivities.keySet().size()]);
        return fragment;
    }

    public ConfigDaDialogFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle(R.string.configure_da)
               .setItems(daIds, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                            mListener.configDa(daConfigActivities.get(daIds[id]));
                    }
                });

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

}
