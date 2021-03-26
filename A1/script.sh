#Start Master
MASTER_PATH="java -classpath bin it.unipr.ds.A1.Master localhost 9000;"
gnome-terminal -e "bash -c \"$MASTER_PATH exec bash\"";

#Start communication Nodes
for i in {0..2}
do
    NODE_PATH="java -classpath bin it.unipr.ds.A1.Node localhost 400"$i";"
    gnome-terminal -e "bash -c \"$NODE_PATH exec bash\"";
done
