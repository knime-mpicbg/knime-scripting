#!/bin/sh


# Usage info
show_help() {
cat << EOF
Usage: ${0##*/} [-h] [-p PORT] [-t THREADS]...
Run a MATLAB server for the KNIME scripting integration

	-h	display this help and exit
	-p	define the port where the server will be listening
	-t	deine the number of threads (MATLAB applications) the server is working with	
EOF
}



# A POSIX variable
OPTIND=1         # Reset in case getopts has been used previously in the shell.

# Set defaults:
port="1198"
threads=1

# Parse input
while getopts "hp:t:" opt; do
	case "$opt" in
		h)
			show_help
			exit 0
			;;
		p)  port=$OPTARG
			;;
		t)  threads=$OPTARG
			;;
	esac
done
shift $((OPTIND-1))



# Run the java server
java -jar ../matlab-srv4knime.jar "-p $port -t $threads"

