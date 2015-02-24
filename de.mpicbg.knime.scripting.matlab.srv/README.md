#MATLAB Server for KNIME's scripting integration 


This is the Server application for the KNIME nodes. It bundles the tools to handle MATLAB code, 
transform data between Java (KNIME) and MATLAB types and finally it allows to communicate with a MATLAB application.
This plugin uses [Matlabcontrol](https://code.google.com/p/matlabcontrol/) connect to MATLAB and [Cajo](https://github.com/ravn/cajo) 
This pluing has the potential to run as a separate instance, so that KNIME clients can connecto to this servlet that runs MATLAB for them instead of having their own local MATLAB installation. However due to concurency problems this server functionality has been deactivated for this release (2.0.3) 

