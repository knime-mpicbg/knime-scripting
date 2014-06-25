This is a Python server application for the Python scripting integration for KNIME.

When compiling this plugin make sure that the it is NOT packaged in a jar file.

To start the server run pyserver.sh from the scripts directory of the compiled plugin.
The server listens by default on the port 1198.
To use the server from KNME, check the configurations host and port and un-check 
"Run Python scripts on local system" just bellow in the Preference dialog (KNIME>Python Scripting).

Currently there is no graceful way to shutdown the server. If you want to stop it, just kill the process.
