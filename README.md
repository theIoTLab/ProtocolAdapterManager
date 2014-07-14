ProtocolAdapter
===============

Protocol Adapter is an Android library that manages at low-level the M2M data collection from different connected sources on one Android device. It aims to become a generic, modular client for the oneM2M standard. Currently this is only the Alpha version of the project, as resulting from the Alpha Phase of the [FI-STAR](http://www.fi-star.eu) project.

## Usage
This component should be built and installed in an Android device. It should be launched by a oneM2M Gateway by sending an intent to eu.fistar.sdcs.pa . If you have a version of the FI-STAR Sensor Data Collection Service Specific Enabler installed (currently available [here](http://130.206.82.42/enablers/sensor-data-collection-service)), this will automatically start the Protocol Adapter.

### Device Adapters
Different (communication) technologies can be used to feed up data to the Protocol Adapter. Each of these technologies is integrated through the maeans of a Device Adapter. A Device Adapter provides the Protocol Adapter with basic functionalities.

Currently only the HDP Device Adapter is available. 
#### HDP Device Adapter
The HDP Device Adapter manages the data collection from medical devices implementing Bluetooth HDP. The HDP Device Adapter can manage multiple devices at the same time. These devices though must be previously paired with the Android Device.

Note: The HDP Device Adapter makes use for Bluettoth HDP management of Signove's Antidote libraries available as well under L-GPL licence from ( http://oss.signove.com/index.php/Antidote:_IEEE_11073-20601_stack ) for what concerns the data acquisition from HDP medical devices. 

## Acknowledgement
This work was carried out with the support of the FI-STAR project (“Future Internet Social and Technological Alignment Research”), an integrated project funded by the European Commission through the 7th ICT - Framework Programme (318389).



