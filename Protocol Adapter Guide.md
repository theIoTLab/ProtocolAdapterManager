#Protocol Adapter Guide

FI-STAR Phase: Beta  
Document version: 1.0 (draft)  
Date: 05/03/2015

##What is the Protocol Adapter
The Protocol Adapter is an M2M data collection software that runs on Android (mainly mobile) devices acting as a gateway for sensor devices. The Protocol Adapter was developed as an open source component of the FI-STAR Frontend Platform, in the frame of the FI-STAR project.
The Protocol Adapter software architecture has three high-level components: 

* the Protocol Adapter Manager Service
* the Device Adapters
* the Protocol Adapter Library

###The Protocol Adapter Manager
The Protocol Adapter Manager is a service (PAManagerService) that manages the several Device Adapters (DA) possibly installed on the same Android device. All of them are implemented in separate Android applications and communicate using the AIDL interfaces and the common objects included in the separate library. At startup time the Protocol Adapter Manager automatically discovers DAs installed on the system and adds them to the pool of available DAs. This makes the architecture modular and expandable.
The PAManagerService has three main roles:

* to provide a single entry point for data-collection applications
* to provide device management interfaces for the application
* to manage the lifecycle of the DAs. 

At the moment, the implementation of the Protocol Adapter Manager is available under the LGPL License on GitHub. You can find it on [https://github.com/theIoTLab/ProtocolAdapterManager](https://github.com/theIoTLab/ProtocolAdapterManager).

###The Device Adapters
A Device Adapter is a software component that manages low-level connections to sensor devices of a given kind and collects data from them. The collected data resulting from the measurements carried out by the sensor devices are provided to the Protocol Adapter Manager with a well-known data structure (i.e. Java object) called Observation.
Generally, DAs provide communication and interoperability at channel and syntactic level. Some operational aspects are also managed by the DA with sensor devices.

At the moment, several implementations of Device Adapters are available:

* HDP Device Adapter, available under the LGPL License on GitHub. You can find it on [https://github.com/theIoTLab/HDPDeviceAdapter](https://github.com/theIoTLab/HDPDeviceAdapter).
* Zephyr Bioharness 3 Device Adapter, available under the LGPL License on GitHub. You can find it on [https://github.com/theIoTLab/ZephyrBHDeviceAdapter](https://github.com/theIoTLab/ZephyrBHDeviceAdapter).
* BerryMed BM1000C, available under the LGPL License on GitHub. You can find it on [https://github.com/theIoTLab/BerryMedDeviceAdapter](https://github.com/theIoTLab/BerryMedDeviceAdapter).

###The Protocol Adapter Library
The Protocol Adapter Library is a library that contains all the objects and facilities (parcelable objects, AIDL interfaces, etc.) needed to develop applications that make use of the Protocol Adapter. Once included in your project, you won’t need to worry about low level details, but instead you can focus on implementing your logic, taking for granted the underlying infrastructure and functionalities provided by the Protocol Adapter. Please note that the library is not only for using in applications, but it is also used by us in the Protocol Adapter Manager and in every Device Adapter. We released this library in the form of an AAR package.

At the moment, the implementation of the Protocol Adapter Library is available under the LGPL License on GitHub. You can find it on [https://github.com/theIoTLab/ProtocolAdapterLibrary](https://github.com/theIoTLab/ProtocolAdapterLibrary).

## Authors, Contact and Contributions
As the licence reads, this is free software released by Consorzio Roma Ricerche. The authors (Marcello Morena and Alexandru Serbanati) will continuously add support for even more medical devices, but external contributions are welcome. Please have a look at the TODO file to know what we are working on and contact us (protocoladapter[at]gmail[dot]com) if you plan on contributing.

## Acknowledgement
This work was carried out with the support of the FI-STAR project (“Future Internet Social and Technological Alignment Research”), an integrated project funded by the European Commission through the 7th ICT - Framework Programme (318389).
