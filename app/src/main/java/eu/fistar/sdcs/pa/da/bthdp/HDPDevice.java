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

import java.util.ArrayList;
import java.util.List;

import eu.fistar.sdcs.pa.IDeviceDescription;
import eu.fistar.sdcs.pa.ISensorDescription;

/**
 * This class represents a HDP Device and implements the Device Interface. It contains references
 * to all the properties of the device and a list of all the sensors contained in the device
 *
 * @author Marcello Morena
 * @author Alexandru Serbanati
 */
public class HDPDevice implements IDeviceDescription {

    private String deviceID; // The 8 byte System-Id
    private String serialNumber; // The device's serial number, empty if not automatically provided by the device
    private String modelName; // The model name
    private String manufacturerName; // The manufacturer name
    private List<ISensorDescription> sensorList = new ArrayList<ISensorDescription>(); // The list of the properties
    private List<String> ieeeDevSpecID = new ArrayList<String>(); // The Device Specializaton ID (usually only one value)
    private String address; // The MAC Address of the device
    private boolean registered;

    public HDPDevice(String mAddress) {
        address = mAddress;
        registered = false;
    }

    /**
     * Set the device attributes that are obtained in the HDP Attributes and the HDP Configuration
     * retrieval phase
     *
     * @param mDeviceID
     *      The 8 byte System-Id
     *
     * @param mSerialNumber
     *      The serial number of the device (not used right now)
     *
     * @param mModelName
     *      The model name
     *
     * @param mManufacturerName
     *      The manufacturer name
     *
     * @param mIeeeDevSpecID
     *      The Devce Specialization ID
     */
    public void setAttributes(String mDeviceID, String mSerialNumber, String mModelName, String mManufacturerName, String mIeeeDevSpecID) {
        deviceID = mDeviceID.replaceAll(":","").toUpperCase();
        serialNumber = mSerialNumber;
        modelName = mModelName;
        manufacturerName = mManufacturerName;
        ieeeDevSpecID.add(mIeeeDevSpecID);
    }

    /**
     * Add a property to the Device
     *
     * @param mIeeeStandard
     *      The property ID as listed in the ISO 11073 nomenclature
     */
    public void addProperty(String mIeeeStandard) {

        // Create the new property
        HDPSensor newProperty = new HDPSensor(mIeeeStandard);

        // Add the property to the property list
        sensorList.add(newProperty);

    }

    @Override
    public String getDeviceID() {
        return deviceID;
    }

    @Override
    public String getSerialNumber() {
        return serialNumber;
    }

    @Override
    public String getModelName() {
        return modelName;
    }

    @Override
    public String getManufacturerName() {
        return manufacturerName;
    }

    @Override
    public List<ISensorDescription> getSensorList() {
        return sensorList;
    }

    public String[] getIEEEDevSpecID() {
        return ieeeDevSpecID.toArray(new String[ieeeDevSpecID.size()]);
    }

    @Override
    public String getAddress() {
        return address;
    }

    /**
     * Set whether the device is registered in the Protocol Adapter or not
     *
     * @param mRegistered
     *      The boolean value representing registration
     */
    @Override
    public void setRegistered(boolean mRegistered) {
        registered = mRegistered;
    }

    /**
     * Check if the device is registered in the Protocol Adapter or not
     *
     * @return
     *      The boolean value representing registration
     */
    @Override
    public boolean isRegistered() {
        return registered;
    }

    /**
     * Returns a read-friendly String representing the object
     *
     * @return
     *      The String representing the object
     */
    public String toString() {
        String propStr = "\n";

        for (ISensorDescription temp: sensorList) {
            propStr += temp.toString();
        }

        return "ID: "+deviceID+"\nModel Number: "+ serialNumber +"\nModel Name: "+modelName+
                "\nManufacturer: "+manufacturerName+"\nDevice Specialization: "+ieeeDevSpecID.get(0)+
                "\nAddress: "+address+"\nProperties: "+propStr;
    }
}
