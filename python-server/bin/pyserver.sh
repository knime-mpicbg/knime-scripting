#! /bin/bash

JAVA_HOME=/usr/java/jre1.6.0_23
PYSERVER_HOME=/usr/local/bin/pyserver

# This allows importing the ordereddict module for Python 2.6
export PYTHONPATH=$PYSERVER_HOME:.

CP=$PYSERVER_HOME/mpicbg-python-1.0.jar:\
$PYSERVER_HOME/cajo-1.134.jar:\
$PYSERVER_HOME/opencsv-2.1.jar

$JAVA_HOME/bin/java -cp $CP de.mpicbg.sweng.pythonserver.PythonServer $*
