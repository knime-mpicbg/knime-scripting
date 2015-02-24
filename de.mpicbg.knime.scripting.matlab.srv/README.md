#MATLAB Server for KNIME's scripting integration 


This is the Server application for the KNIME nodes. It bundles the tools to handle MATLAB code, 
transform data between Java (KNIME) and MATLAB types and finally it allows to communicate with a MATLAB application.
This plugin uses [Matlabcontrol](https://code.google.com/p/matlabcontrol/) connect to MATLAB and [Cajo](https://github.com/ravn/cajo) 
to allow the connection on a remote machine.
Hence there are two modes of operation for the MATLAB server:

1. The MATLAB client creates a local MATLAB session and controls it.
2. The MATLAB client communicates with a server a remote machine through with it then accesses the MATLAB applicaton



##Deployment

In case the MATLAB scripting integration plugin for KNIME uses only a local MATLAB installation, this plugin does not need to be exported separatly. However if you want to access on a centralized MATLAB application on a remote machine, you can use this plugin to do so.
When Exporting this plugin make sure that the it is NOT packaged in a jar file.


##Run the Server

To start the server open MATLAB, change directory to the plugin directory 
and run matlab_srv4knime.m
Per default the server will be listening on the port 1198.
To use the server from KNME, check the configurations host and port in the Preference dialog
(KNIME>MATLAB Scripting). 

