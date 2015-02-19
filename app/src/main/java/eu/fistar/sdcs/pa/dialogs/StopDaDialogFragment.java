package eu.fistar.sdcs.pa.dialogs;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

import eu.fistar.sdcs.pa.R;

public class StopDaDialogFragment extends DialogFragment {
    private List<String> availableDa;

    private List<String> daToStop = new ArrayList<String>();

    private IPADialogListener mListener;

    public static StopDaDialogFragment newInstance(List<String> availableDa) {
        StopDaDialogFragment fragment = new StopDaDialogFragment();
        fragment.availableDa = availableDa;
        return fragment;
    }

    public StopDaDialogFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        // Specify the list array, the items to be selected by default (null for none),
        // and the listener through which to receive callbacks when items are selected
        builder.setTitle(R.string.stop_da)
                .setMultiChoiceItems(availableDa.toArray(new CharSequence[availableDa.size()]), null,
                        new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                                if (isChecked) {
                                    // If the user checked the item, add it to the selected items
                                    daToStop.add(availableDa.get(which));
                                } else if (daToStop.contains(availableDa.get(which))) {
                                    // Else, if the item is already in the array, remove it
                                    daToStop.remove(availableDa.get(which));
                                }
                            }
                        })
                // Set the action buttons
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        if (!daToStop.isEmpty()) {
                            for (String daId : daToStop) {
                                mListener.stopDA(daId);
                            }
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        StopDaDialogFragment.this.getDialog().cancel();
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
