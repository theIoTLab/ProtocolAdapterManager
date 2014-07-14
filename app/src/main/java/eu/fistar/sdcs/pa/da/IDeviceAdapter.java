package eu.fistar.sdcs.pa.da;

import android.os.Binder;

import java.util.List;

import eu.fistar.sdcs.pa.IDeviceDescription;
import eu.fistar.sdcs.pa.IProtocolAdapter;

/**
 * Interface implemented by Device Adapters (DA).<br>
 * A DA provides a technological adapter and manage the use of base resources.
 *<br>
 * Example of Device Adapters are <br>
 * - HDP Device Adapter<br>
 * - SmartBT Device Adapter<br>
 * - Device Adapters for proprietary devices
 */
public interface IDeviceAdapter {

    /**
     * Returns a list containing all the devices currently associated with the DA
     *
     * @return
     *      List of devices associated with the DA
     */
    public List<IDeviceDescription> getDevices();

    /**
     * Allows a Protocol Adapter to register itself passing its PABinder to the DA
     *
     * @param pa
     *      The PABinder that will be used by the DA to retrieve the instance of the
     *      Protocol Adapter
     */
    public void RegisterPA(IProtocolAdapter.PABinder pa);

    /**
     * This class extends the Binder class and is required in order to facilitate the
     * communication with Protocol Adapter
     */
    public abstract class DABinder extends Binder {

        /**
         * Used by the Protocol Adapter to get the instance of the Device Adapter
         *
         * @return
         *      The instance of the Device Adapter associated with this Binder
         */
        public abstract IDeviceAdapter getDeviceAdapter();

    }

}