#Start Master
MASTER_PATH="/home/francesco/eclipse/plugins/org.eclipse.justj.openjdk.hotspot.jre.full.linux.x86_64_15.0.2.v20210201-0955/jre/bin/java -classpath /home/francesco/Desktop/SD/assignements/A1/bin it.unipr.ds.A1.Master localhost 9000;"
gnome-terminal -e "bash -c \"$MASTER_PATH exec bash\"";

#Start Nodes

NODE1_PATH="/home/francesco/eclipse/plugins/org.eclipse.justj.openjdk.hotspot.jre.full.linux.x86_64_15.0.2.v20210201-0955/jre/bin/java -classpath /home/francesco/Desktop/SD/assignements/A1/bin it.unipr.ds.A1.Node localhost 4000;"
gnome-terminal -e "bash -c \"$NODE1_PATH exec bash\"";

NODE2_PATH="/home/francesco/eclipse/plugins/org.eclipse.justj.openjdk.hotspot.jre.full.linux.x86_64_15.0.2.v20210201-0955/jre/bin/java -classpath /home/francesco/Desktop/SD/assignements/A1/bin it.unipr.ds.A1.Node localhost 4001;"
gnome-terminal -e "bash -c \"$NODE2_PATH exec bash\"";

NODE3_PATH="/home/francesco/eclipse/plugins/org.eclipse.justj.openjdk.hotspot.jre.full.linux.x86_64_15.0.2.v20210201-0955/jre/bin/java -classpath /home/francesco/Desktop/SD/assignements/A1/bin it.unipr.ds.A1.Node localhost 4002;"
gnome-terminal -e "bash -c \"$NODE3_PATH exec bash\"";

