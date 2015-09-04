#Protocol Adapter Manager

FI-STAR Phase: Beta  
Document version: 1.1
Date: 04/09/2015

##What is the Protocol Adapter Manager
The Protocol Adapter Manager is a component of the Protocol Adapter. More information about the Protocol Adapter can be found [here](https://github.com/theIoTLab/ProtocolAdapterManager/blob/master/Protocol%20Adapter%20Guide.md) and it is advised that you first read that document before going on.
The Protocol Adapter Manager is a service (`PAManagerService`) that manages the several Device Adapters (DA) possibly installed on the same Android device. All of them are implemented in separate Android applications and communicate using the AIDL interfaces and the common objects included in the separate library. At startup time, the Protocol Adapter Manager automatically discovers the DAs installed on the system and adds them to the pool of available DAs. This, together with the ability to discover the capabilities of the different DAs (and thus of the supported classes of devices) makes the architecture modular and expandable.
The `PAManagerService` has three main roles:

* to provide a single entry point for data-collection applications,
* to provide device management interfaces for the applications,
* to manage the lifecycle of the DAs.

##Using the Protocol Adapter Manager
###Installation
In order to use the Protocol Adapter, it must be installed on the Android device where it is going to be used. To do this, just manually install the Protocol Adapter APK in the system.
The Protocol Adapter by itself is pretty useless, so you should install at least one Device Adapter, specifically the one (or ones) that handles the devices you want to work with. To do this, manually install in the system the APKs of the Device Adapters you want to use.
Remember that, due to Android security policies, Device Adapters will not respond to Protocol Adapter’s startup probing unless they are manually started once after installation. To do this, just go to your Drawer (the list of installed applications), find the icon of the Device Adapter you just installed and tap on it. After their first time (see above), there is no need to start the Device Adapters again.

###Using the Protocol Adapter Library to interact with the Protocol Adapter
Once the Protocol Adapter is installed in your system, the easiest way to interact with it is by using the Protocol Adapter Library. To know more on how to include this library in your project and how to use it, please refer to the Protocol Adapter Library documentation that you can find [here](https://github.com/theIoTLab/ProtocolAdapterLibrary/blob/master/README.md).

###Protocol Adapter interaction flow
####Initialization of the Protocol Adapter
The first thing you should do in order to use the Protocol Adapter is binding the service as described in the related section and then register your listener.
Once the binding is complete and your listener is registered, the Protocol Adapter is up and running, but it is not really working yet. The reason is that no Device Adapters are started at this point. You can start a specific Device Adapter that you know in advance of or you can get a list of all the Device Adapters available in the system and start some of them.
To get all the Device Adapters available in the system you can use the `getAvailableDAs()` method of the `IProtocolAdapter` interface. This method returns a `Map` that has DA IDs as keys, and the corresponding Capabilities as values. If you ever need to get the `Capabilities` object of a specific DA, you can retrieve it by calling the `getDACapabilities()` method of the `IProtocolAdapter` interface, passing the DA ID as an argument.
To start a specific DA you can use the `startDA()` method of the `IProtocolAdapter` interface, passing the DA ID as an argument.
Once you started the DAs you are interested in, the corresponding devices will be available to use in the Protocol Adapter.
Remember that Bluetooth device discovery and bluetooth device pairing is not supported inside the Protocol Adapter, so for a bluetooth device to be available it must be paired in advance.

####Interaction with devices
Interaction with devices may greatly vary depending on the DA’s capabilities which reflect the capabilities of the supported devices and which are stated in the `Capabilities` object. 
The greatest differences in device interaction patterns depend on whether the Device Adapter is a connection initiator or not. In the first case, the application will be in charge of creating the connection with the device (as in the Zephyr BioHarness Device Adapter) and maybe enabling data pushing (as in the MIR Spirodoc Device Adapter), while in the latter case devices just connect and start sending data by themselves (as in the HDP Device Adapter) and it can happen that the application won’t be able to interact with devices at all.
If you are uncertain whether your DA is a connection initiator or not, you can find this out by retrieving its `Capabilities` object and invoking the `isConnectionInitiator()` method.

#####Getting available devices
If in your system there are DAs that are connection initiators, you are able to access the devices hey handle. To do this, you should use the `getDADevices()` method of the `IProtocolAdapter` interface. This method will return you a map that has the devices ID as the keys and, as  values, a list of identifiers of the DAs which can handle that device .

#####Connection
If in your system there are DAs that are connection initiators, you are able to explicitly connect to the devices they handle. Usually this is the only way to start getting data from that devices. To connect to a device, you should use the `connectDev()` method of the `IProtocolAdapter` interface, passing the ID of the device you want to connect to. The Protocol Adapter will take care of finding the DA that manages that device. Beware that, if the device is handled by more than one DA, the Protocol Adapter won’t be able to pick one of them for you. In this case you should use the `forceConnectDev()` method of the `IProtocolAdapter` interface to connect to the device, passing both the ID of the device you want to connect to and the ID of the DA you want to use for the connection as arguments. If you don’t do this way, the Protocol Adapter will throw an exception when you try to perform a regular connection. To find out how many DAs handle a single device, please refer to the previous section.
Anyway, when the connection with the device is established, the `registerDevice()` method of the `IProtocolAdapterListener` interface is called by the Protocol Adapter to notify the event, no matter what method was called to perform the connection. The same method get called even when the connection is spontaneous (in case the DA is not connection initiator).

#####Configuration
Some DAs may support device configuration. To find out if a DA supports it, you can retrieve its Capabilities object and invoke the `getDeviceConfigurationType()` method. This way you will know if the configuration is not supported, if its supported only before you connect to a device, if its supported at runtime, or if it’s supported both before you connect to a device and at runtime.
If the DA supports device configuration, you can invoke the `setDeviceConfig()` method of the `IProtocolAdapter` interface to provide a custom configuration. You will need to provide two arguments: the device ID and a Map containing configuration parameters as keys and their values as values.
There are no standards about device configuration, so the syntax of keys and values is completely up to the Device Adapter and you should know it in advance.

#####Sending commands
Some DAs may support sending commands to devices. To find out if a DA supports it, you can retrieve its Capabilities object and invoke the `supportCommands()` method. If sending commands is supported, you can send commands to a specific device by invoking the `execCommand()` method of the `IProtocolAdapter` interface, passing the command, its parameter and the device ID as arguments. The Protocol Adapter will take care of finding the right DA. Note that the device must be connected before you can send it commands, otherwise the Protocol Adapter will throw an exception.
There are no standards about commands and parameters, so their syntax is completely up to the Device Adapter and you should know it in advance. However, you can retrieve the list of supported commands at runtime by invoking the `getCommandList()` method of the `IProtocolAdapter` interface.

#####Blacklist/Whitelist
Some DAs may support blacklisting and whitelisting. To find out if a DA supports these features, you can retrieve its `Capabilities` object and invoke the `hasBlacklist()` and `hasWhitelist()` methods of the `IProtocolAdapter` interface. If blacklisting or whitelisting are supported you can add a device to the list and remove a device from the list by calling the right method of the `IProtocolAdapter` interface and passing the device ID as an argument.
If you want to retrieve the entire blacklist/whitelist or if you want to set devices in bulk, you can do that by invoking the right methods and passing the list of devices ID as an argument.
You can always add/remove devices to blacklist and whitelist via the Protocol Adapter, but if the DA that manages that devices does not support blacklist and whitelist, your actions will have no effects at all.
Remember that, when playing with blacklists and whitelists, you should use either one approach or another, not both together, since it may lead to unpredictable results.

####Data collection lifecycle
The entire purpose of the Protocol Adapter is to collect data from devices. So here we will cover the lifecycle of data collection to better understand all its phases and how each one of them is organized.

#####Device Registration
Device connections can be spontaneous or requested, but in both cases a `DeviceDescription` object is generated upon connection and the `registerDevice()` method of the `IProtocolAdapterListener` interface is invoked passing that object as an argument. The `DeviceDescription` object contains all the useful data available about the device that has just connected. To know more about this object see the related section inside this document.
From this moment on, the device is considered connected. This means that it is allowed to send data at any time and it is available for all the interactions allowed by its DA.
Sometimes device information may not be completely available at connection time or they can vary in time. Either cases, whenever new or updated information about the device are available, the Protocol Adapter will call the `registerDeviceProperties()` method of the `IProtocolAdapter` interface, passing the updated `DeviceDescription` object as an argument.

#####Data Pushing
Once a device is registered, it can send data. Data sending can be spontaneous or explicitly requested (e.g. via a command). Either cases, whenever new data is available from device, an `Observation` object is created and the `pushData()` method of the `IProtocolAdapterListener` interface is invoked passing that object as an argument together with the DeviceDescription object. The `Observation` object contains all the measurement data and other useful information such as metadata. To know more about this object see the related section inside this document.
Data pushing can be repeated as many times as it is needed while a device is registered.

#####Device Disconnection
When a registered device loses its connection unexpectedly, the Protocol Adapter takes care of notifying this event by invoking the `deviceDisconnected()` method of the `IProtocolAdapterListener` interface, passing as an argument the `DeviceDescription` object associated with the device that has lost the connection. Please note that when this method gets called, the device is not yet removed from the system, but it will not send any data and it is not available for interactions that requires the connection. It is up to the DA to decide when to deregister the device.

#####Device Deregistration
The natural ending of the device operations can occur for different reasons. Whatever these reasons are, the Protocol Adapter takes care of notifying this event by invoking the `deregisterDevice()` method of the `IProtocolAdapterListener` interface, passing as an argument the `DeviceDescription` object associated with the deregistered device.
This marks the end of the device lifecycle. At this point, the device can not send any data, it is not available for any interactions and it is removed from the system.

## Authors, Contact and Contributions
As the licence reads, this is free software released by Consorzio Roma Ricerche. The authors (Marcello Morena and Alexandru Serbanati) will continuously add support for even more medical devices, but external contributions are welcome. Please have a look at the TODO file to know what we are working on and contact us (protocoladapter[at]gmail[dot]com) if you plan on contributing.

## Acknowledgement
This work was carried out with the support of the FI-STAR project (“Future Internet Social and Technological Alignment Research”), an integrated project funded by the European Commission through the 7th ICT - Framework Programme (318389).
