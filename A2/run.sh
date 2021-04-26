#!/bin/bash

# The number of nodes we want to create is passed by command line as the first argument
if (( $# != 1 ))
then
    echo "Usage: $0 <N_NODES>"
    exit 1
fi

N_NODES=$1

# Each node has an ID number which start from 0 and goes up incrementally
ID=0;
TYPE="";


# Start N_NODES nodes
for (( i=0; i<$N_NODES; i++ ))
do
    if (( $i == 0 )) 
    then 
        TYPE="f"

    else if (( $i == $(( $N_NODES-1 )) )) 
    then
        TYPE="l"

    else TYPE="i"
    fi
    fi

    CMD="java -classpath bin it.unipr.ds.A2.Node $ID $TYPE;"
    gnome-terminal -e "bash -c \"$CMD exec bash\"";
    echo $TYPE

    ID=$(( $ID + 1 ));
done