ProtocolAdapter
===============

Protocol Adapter is an Android library that manages low-level data collection from different connected sources for M2M applications. It aims to become a generic, modular client for the oneM2M standard. Currently this is only the Alpha version of the project, as resulting from the Alpha Phase of the [FI-STAR](http://www.fi-star.eu) project.
## Usage
This component should be built and installed in an Android device. You should be able to compile as soon as you clone this project in Android Studio ( VCS >> Checkout from Version Control >> Git).
Due to Android characteristics, the communication with other components _on the same device_ is currently based on Android Intents.
The Protocol Adapter should be launched by a oneM2M Gateway by sending an intent with the action set to eu.fistar.sdcs.pa . Optionally, one can set the package to eu.fistar.sdcs.pa to avoid broadcasting.
Future versions of the FI-STAR Sensor Data Collection Service Specific Enabler (currently available [here](http://130.206.82.42/enablers/sensor-data-collection-service)) will automatically start the Protocol Adapter.
### Device Adapters
Different (communication) technologies can be used to feed up data to the Protocol Adapter. Each of these technologies is integrated through the maeans of a Device Adapter. A Device Adapter provides the Protocol Adapter with basic functionalities.
Currently only the HDP Device Adapter is available. 
#### HDP Device Adapter
The HDP Device Adapter manages the data collection from medical devices implementing Bluetooth HDP. The HDP Device Adapter can manage multiple devices at the same time. These devices though must be previously paired with the Android Device.
Note: The HDP Device Adapter makes use for Bluettoth HDP management of Signove's Antidote libraries available as well under L-GPL licence from ( http://oss.signove.com/index.php/Antidote:_IEEE_11073-20601_stack ) for what concerns the data acquisition from HDP medical devices. 
## Authors, Contact and Contributions
As the licence reads, this is free software released by Consorzio Roma Ricerche. The authors (Marcello Morena and Alexandru Serbanati) will continuously add support for even more medical devices, but external contributions are welcome. Please have a look at the TODO file on what we are working on and contact us (protocoladapter[at]gmail[dot]com) if you plan on contributing.
## Acknowledgement
This work was carried out with the support of the FI-STAR project (“Future Internet Social and Technological Alignment Research”), an integrated project funded by the European Commission through the 7th ICT - Framework Programme (318389).



