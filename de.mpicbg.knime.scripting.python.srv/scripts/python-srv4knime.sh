#! /bin/bash

# Change directory to the server home (provided that this is executed from the scripts folder
cd ..

# Define a specific java RE here if necessairy here it' just the system default.
JAVA=`which java`

# This allows importing the ordereddict module for Python 2.6
PYSERVER_HOME=`pwd`
export PYTHONPATH=$PYSERVER_HOME:.

# Defining the class path.
CP=$PYSERVER_HOME/python-srv4knime.jar:\
$PYSERVER_HOME/lib/cajo-1.134.jar:\
$PYSERVER_HOME/lib/opencsv-2.1.jar

# Start the server. To stop it just kill the process.
$JAVA -cp $CP de.mpicbg.knime.scripting.python.srv.PythonServer $*
