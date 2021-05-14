Il sistema si compone di 3 classi principali (Broker, Client, Coordinator), 1 classe di appoggio (Request), ed un file di properties contenente i quorum (config.properties).

Per eseguire un test del sistema, occorre eseguire le seguenti operazioni.

===============================
====== config.properties ======
===============================
Per prima cosa occorre settare un read quorum ed un write quorum nel file config.properties:
readQuorum=<Vr>
writeQuorum=<Vw>

Sia V il numero di coordinatori che si intendono avviare. Allora, i vincoli su Vr e Vw sono:
Vr + Vw > V
Vw > V / 2

===============================
============ Broker ===========
===============================
Fatto ciò, occore lanciare un processo Broker:
$ java -classpath bin:lib/activemq-all-5.16.1.jar it.unipr.ds.A3.Broker

A questo punto è possibile eseguire N processi Client ed M processi Coordinator, in qualisasi ordine

===============================
========= Coordinator =========
===============================
Per lanciare un generico Coordinator occorre eseguire il seguente comando:
$ java -classpath bin:lib/activemq-all-5.16.1.jar it.unipr.ds.A3.Coordinator <COORDINATOR_ID>

Dove:
<COORDINATOR_ID> è un numero intero positivo che rappresenta l'ID del coordinatore

===============================
============ Client ===========
===============================
Per lanciare un generico Client occorre eseguire il seguente comando:
$ java -classpath bin:lib/activemq-all-5.16.1.jar it.unipr.ds.A3.Client <CLIENT_ID>

Dove:
<CLIENT_ID> è un numero intero positivo che rappresenta l'ID del client

===============================
=========== Esempio ===========
===============================
Per eseguire un test con 2 Clients e 2 Coordinators, occorre innanzi tutto editare config.properties:
readQuorum=1
writeQuorum=2

Dopodichè si eseguono i seguenti comandi:

$ java -classpath bin:lib/activemq-all-5.16.1.jar it.unipr.ds.A3.Broker
$ java -classpath bin:lib/activemq-all-5.16.1.jar it.unipr.ds.A3.Coordinator 0
$ java -classpath bin:lib/activemq-all-5.16.1.jar it.unipr.ds.A3.Coordinator 1
$ java -classpath bin:lib/activemq-all-5.16.1.jar it.unipr.ds.A3.Client 2
$ java -classpath bin:lib/activemq-all-5.16.1.jar it.unipr.ds.A3.Client 3

Da Eclipse sarà sufficiente importare l'intero progetto ed eseguire i processo Broker, Coordinator e Node, passando ad ogni esecuzione i parametri richiesti in modo opportuno.

In ambiente Linux è inoltre disponibile lo script run.sh, utilizzabile nel seguente modo:

$ ./run.sh <N_COORDINATORS> <N_CLIENTS>

Dove:
<N_COORDINATORS> è un numero intero positivo che rappresenta il numero di coordinatori che si intendono avviare
<N_CLIENTS> è un numero intero positivo che rappresenta il numero di clients che si intendono avviare