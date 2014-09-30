This is the MATLAB server application for the MATLAB scripting integration for KNIME.

When compiling this plugin make sure that the it is NOT packaged in a jar file.

To start the server open MATLAB, change directory to the plugin directory 
and run matlab_srv4knime.m
Per default the server will be listening on the port 1198.
To use the server from KNME, check the configurations host and port in the Preference dialog
(KNIME>MATLAB Scripting). 