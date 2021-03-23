#Start Master
MASTER_PATH="/usr/lib/jvm/java-16-oracle/bin/java -Dfile.encoding=UTF-8 -classpath /home/edo/dev/ds-assignements/A1/bin it.unipr.ds.A1.Master localhost 9000;"
gnome-terminal -e "bash -c \"$MASTER_PATH exec bash\"";

#Start Nodes
NODE1_PATH="/usr/lib/jvm/java-16-oracle/bin/java -Dfile.encoding=UTF-8 -classpath /home/edo/dev/ds-assignements/A1/bin it.unipr.ds.A1.Node localhost 4000;"
gnome-terminal -e "bash -c \"$NODE1_PATH exec bash\"";

NODE2_PATH="/usr/lib/jvm/java-16-oracle/bin/java -Dfile.encoding=UTF-8 -classpath /home/edo/dev/ds-assignements/A1/bin it.unipr.ds.A1.Node localhost 4001;"
gnome-terminal -e "bash -c \"$NODE2_PATH exec bash\"";

NODE3_PATH="/usr/lib/jvm/java-16-oracle/bin/java -Dfile.encoding=UTF-8 -classpath /home/edo/dev/ds-assignements/A1/bin it.unipr.ds.A1.Node localhost 4002;"
gnome-terminal -e "bash -c \"$NODE3_PATH exec bash\"";

