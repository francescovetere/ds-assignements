#Start Master
MASTER_PATH="java -classpath bin it.unipr.ds.A1.Master localhost 9000;"
gnome-terminal -e "bash -c \"$MASTER_PATH exec bash\"";

#Start Nodes
NODE1_PATH="java -classpath bin it.unipr.ds.A1.Node localhost 4000;"
gnome-terminal -e "bash -c \"$NODE1_PATH exec bash\"";

NODE2_PATH="java -classpath bin it.unipr.ds.A1.Node localhost 4001;"
gnome-terminal -e "bash -c \"$NODE2_PATH exec bash\"";

NODE3_PATH="java -classpath bin it.unipr.ds.A1.Node localhost 4002;"
gnome-terminal -e "bash -c \"$NODE3_PATH exec bash\"";

# NODE4_PATH="java -classpath bin it.unipr.ds.A1.Node localhost 4003;"
# gnome-terminal -e "bash -c \"$NODE4_PATH exec bash\"";
