This is a Python server application for the Python scripting integration for KNIME.

## Compilation
The easiest is to eport the package from Eclipse. When compiling this plugin make sure that the it is NOT packaged in a jar file, since we have to access the start script later.

## Startup
To start the server run *pyserver.sh* from the scripts directory of the compiled package.

Currently there is no graceful way to shutdown the server. If you want to stop it, just kill the process.

## Client Configuration
The server listens by default on the port 1198.
To connect to the serverfrom KNME client (Analytics Platform, desktop application), the preferences have to be set accordingly. 

(Menu > KNIME > Preferences > KNIME > Python Scripting)

In the preference dialog:

* set the host
* set the port
* un-check  "Run Python scripts on local system" just bellow in the Preference dialog (since this overrides the host/port settings).


