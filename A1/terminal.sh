# Master
/home/fra/eclipse/plugins/org.eclipse.justj.openjdk.hotspot.jre.full.linux.x86_64_15.0.2.v20210201-0955/jre/bin/java -classpath /home/fra/Desktop/SD/lab/ds-assignements/A1/bin it.unipr.ds.A1.Master localhost 9000 &
P1=$!

sleep 2

# Node
/home/fra/eclipse/plugins/org.eclipse.justj.openjdk.hotspot.jre.full.linux.x86_64_15.0.2.v20210201-0955/jre/bin/java -classpath /home/fra/Desktop/SD/lab/ds-assignements/A1/bin it.unipr.ds.A1.Node localhost 4000 &
P2=$!

wait $P1 $P2
