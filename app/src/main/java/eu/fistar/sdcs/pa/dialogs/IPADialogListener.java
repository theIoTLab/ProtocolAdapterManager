package eu.fistar.sdcs.pa.dialogs;

import android.content.ComponentName;

public interface IPADialogListener {
    public void startDA(String daId);
    public void stopDA(String daId);
    public void sendCommand(String command, String parameter, String devId);
    public void connectDev(String devId, String daId);
    public void disconnectDev(String daId);
    public void configDa(ComponentName comp);
}
