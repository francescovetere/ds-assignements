#!/bin/bash

# The number of clients and of coordinators we want to create is passed by command line as the first and second arguments
if (( $# != 2 ))
then
    echo "Usage: $0 <N_CLIENTS> <N_COORDINATORS>"
    exit 1
fi

N_CLIENTS=$1
N_COORDINATORS=$2

# Start N_COORDINATORS coordinators
for (( i=0; i<$N_COORDINATORS; i++ ))
do
    CMD="java -classpath bin:lib/activemq-all-5.16.1.jar it.unipr.ds.A3.Coordinator;"
    gnome-terminal -e "bash -c \"$CMD exec bash\"";
done

# Start N_CLIENTS coordinators
for (( i=0; i<$N_COORDINATORS; i++ ))
do
    CMD="java -classpath bin:lib/activemq-all-5.16.1.jar it.unipr.ds.A3.Client;"
    gnome-terminal -e "bash -c \"$CMD exec bash\"";
done