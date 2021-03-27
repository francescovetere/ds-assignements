#!/bin/bash

# The number of nodes we want to create is passed by command line as the first argument
if (( $# != 1 ))
then
    echo "Usage: $0 <number_of_nodes>"
    exit 1
fi

N=$1

# Each node has a port number which start from 4000 and go up incrementally
basePort=4000;

# Start Master
MASTER_PATH="java -classpath bin it.unipr.ds.A1.Master localhost 9000;"
gnome-terminal -e "bash -c \"$MASTER_PATH exec bash\"";

# Start Communication Nodes
for (( i=0; i<$N; i++ ))
do
    port=$(( $basePort + $i ));
    echo $port
    NODE_PATH="java -classpath bin it.unipr.ds.A1.Node localhost $port;"
    gnome-terminal -e "bash -c \"$NODE_PATH exec bash\"";
done