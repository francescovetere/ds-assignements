Per eseguire un test su N nodi, è sufficiente lanciare N comandi di questa forma:

$ java -classpath bin it.unipr.ds.A2.Node <NODE_ID> <NODE_TYPE>

Dove:
<NODE_ID> è un numero intero positivo che rappresenta l'ID del nodo (da assegnare in modo crescente)
<NODE_TYPE> è un carattere che rappresenta la tipologia del nodo:
			"f" per il nodo iniziale (first)
			"i" per un nodo intermedio (intermediate)
			"l" per il nodo finale (last)

Esempio di esecuzione di 3 nodi:
$ java -classpath bin it.unipr.ds.A2.Node 0 f
$ java -classpath bin it.unipr.ds.A2.Node 1 i
$ java -classpath bin it.unipr.ds.A2.Node 2 l

In ambiente Linux, è inoltre disponibile lo script run.sh, utilizzabile nel seguente modo:

$ ./run.sh <N_NODES>

Dove:
<N_NODES> è un numero intero positivo che rappresenta il numero di nodi che si intendono avviare