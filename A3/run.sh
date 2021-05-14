#!/bin/bash

# The number of clients and of coordinators we want to create is passed by command line as first and second arguments
if (( $# != 2 ))
then
    echo "Usage: $0 <N_COORDINATORS> <N_CLIENTS>"
    exit 1
fi

N_CLIENTS=$2
N_COORDINATORS=$1

# Start the JMS broker
CMD="java -classpath bin:lib/activemq-all-5.16.1.jar it.unipr.ds.A3.Broker;"
gnome-terminal -e "bash -c \"$CMD exec bash\"";

# Each Coordinator has an ID number which start from 0 and goes up incrementally
COORD_ID=0;

# Start N_COORDINATORS coordinators
for (( i=0; i<$N_COORDINATORS; i++ ))
do
    CMD="java -classpath bin:lib/activemq-all-5.16.1.jar it.unipr.ds.A3.Coordinator $COORD_ID;"
    gnome-terminal -e "bash -c \"$CMD exec bash\"";
    COORD_ID=$(( $COORD_ID + 1 ));
done


# Each Client has an ID number which start from COORD_ID and goes up incrementally
CLIENT_ID=($COORD_ID);

# Start N_CLIENTS clients
for (( i=0; i<$N_CLIENTS; i++ ))
do
    CMD="java -classpath bin:lib/activemq-all-5.16.1.jar it.unipr.ds.A3.Client $CLIENT_ID;"
    gnome-terminal -e "bash -c \"$CMD exec bash\"";
    CLIENT_ID=$(( $CLIENT_ID + 1 ));
done