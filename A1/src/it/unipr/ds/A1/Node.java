package it.unipr.ds.A1;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Class that defines a generic peer (node) in the system, which can both send
 * and receive messages
 */
public class Node {
	public Object lock = new Object();

	private static int MASTER_PORT;
	private static String MASTER_ADDR;

	public int NODE_ID;
	private static String NODE_ADDR;
	private static int NODE_PORT;

	// Each node sends a registration string of this form: ip:port
	// id will be assigned by Master
	String registrationString = null;

	private static final String PROPERTIES = "config.properties";
	// Probability of error
	private float LP;

	// Number of messages
	public int M;

	public List<Message> messages;

	// Number of nodes
	public int N;

	// List of sockets to communicate with other N - 1 nodes
	private List<Socket> sockets;

	// a map containing all the registered nodes,
	// in the form <key, val> where key = ID, val = ip:port
	private Map<Integer, String> nodes;

	// The socket from which this Node will be receiving messages from other Nodes
	private ServerSocket receivingSocket;

	// ****************** N.B. ******************
	// Le seguenti variabili sono public solamente per comodità di accesso da NodeThreadMulticast
	// Ovviamente, dovrebbero essere private e avere ciascuna la propria get e set...

	// Statistics variables
	public double totTime = 0;
	public double avgTime = 0;
	public int numSent = 0;
	public int numResent = 0;
	public int numReceived = 0;
	public int numLost = 0;

	// 2 variabili che servono solamente per capire quando uscire dal while(true) dello scambio multicast
	public int numMissing = 0;
	public int numEnded = 0;
	public int numReReceived = 0;

	// The queue of received messages
	// (shared by the main thread and the N-1 storing threads)
	public Map<Integer, List<Message>> msgQueue = new ConcurrentHashMap<>();

	public Node() throws IOException {
		this.receivingSocket = new ServerSocket(NODE_PORT);

		String LP = null;
		String M = null;

		try {
			LP = Utility.readConfig(PROPERTIES, "LP");
			M = Utility.readConfig(PROPERTIES, "M");
		} catch (IOException e) {
			System.out.println("File " + PROPERTIES + " not found");
			e.printStackTrace();
		}

		this.LP = Float.parseFloat(LP);
		this.M = Integer.parseInt(M);

		messages = new CopyOnWriteArrayList<>();

		// What we send to the master in order to registrate
		registrationString = NODE_ADDR + ":" + NODE_PORT;

		// Reading .properties file to get Master address and port
		String masterAddrAndPort = null;
		try {
			masterAddrAndPort = Utility.readConfig(PROPERTIES, "master");
		} catch (IOException e) {
			System.out.println("File " + PROPERTIES + " not found");
			e.printStackTrace();
		}

		// Now, we parse the string obtained by reading the .properties file
		String[] masterAddrAndPortArray = masterAddrAndPort.split(",");
		MASTER_ADDR = masterAddrAndPortArray[0];
		MASTER_PORT = Integer.parseInt(masterAddrAndPortArray[1]);
	}

	public void start() {
		System.out.println("Node running (" + NODE_ADDR + ":" + NODE_PORT + ")");

		// We open a socket towards the master, we send our registration message
		// and we wait for the response (a map of all the nodes in the system)
		nodes = registrateToMaster();

		System.out.println("Received start message from master");
		System.out.println("The following nodes are registered in the system");
		nodes.forEach((id, addrAndPort) -> System.out.println("<" + id + "; " + addrAndPort + ">"));
		System.out.println();

		NODE_ID = Utility.getKey(nodes, registrationString);
		System.out.println("My ID is " + NODE_ID);

		// total number of nodes
		N = nodes.size();

		// allocate space for every queue
		for (int id : nodes.keySet()) {
			if (id != NODE_ID)
				msgQueue.put(id, new CopyOnWriteArrayList<>());
		}

		// We initialize the sockets with a "pyramidal" approach
		this.sockets = socketsSetup();

		System.out.println("\n---Total " + sockets.size() + " sockets---");
		for (int i = 0; i < sockets.size(); ++i) {
			System.out.println("\t<" + sockets.get(i).getInetAddress().getCanonicalHostName() + ":"
					+ sockets.get(i).getPort() + ">");
		}

		// At this point, we have our N - 1 sockets:
		// we can begin our multicast protocol: 1 thread send M messages to all the
		// sockets, the other N-1 threads receives messages from other
		// we repeat the process M times
		sendToAll();

		receiveFromAll();

		this.avgTime = this.totTime / this.numSent;

		System.out.println("\n\tMulticast exchange terminated correctly\n");

		// Finally, we open a socket towards the master, and we send our statistics data
		sendStatisticsToMaster();
	}

	@SuppressWarnings("unchecked")
	private Map<Integer, String> registrateToMaster() {
		// ObjectOutputStream masterOs = null;
		// ObjectInputStream masterIs = null;

		// We open a socket towards the master, and we send our registration message
		try (Socket masterSocket = new Socket(MASTER_ADDR, MASTER_PORT)) {
			// masterOs = new ObjectOutputStream(masterSocket.getOutputStream());

			System.out.println("Node sends: " + registrationString + " to master");

			// masterOs.writeObject(registrationString);
			// masterOs.flush();

			Utility.send(masterSocket, registrationString);
			// masterIs = new ObjectInputStream(new BufferedInputStream(masterSocket.getInputStream()));

			System.out.println("Waiting for start message from master");
			// Object o = masterIs.readObject();

			Object o = Utility.receive(masterSocket);

			// We expect to receive a Map<Integer, String> (i.e.: id --> <ip, port>)
			// If we receive something else, we terminate our execution
			// TODO: maybe we could terminate it more gracefully (?)

			if (!(o instanceof Map<?, ?>)) {
				System.out.println("Didn't received correctly the Map of nodes");
				System.exit(-1);
			}

			nodes = (Map<Integer, String>) o;

		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return nodes;
	}

	/**
	 * The initial socket setup, before the multicast exchange begins
	 * Node i must activate N-i-1 connections. In this way, we avoid creating N^2
	 * connections, because we exploit the fact that TCP connections are bidirectional
	 * In the end, we want for each node N sockets N - NODE_ID - 1 will be created
	 * from the Node itself. The other NODE_ID sockets will be created from other
	 * Nodes, and this current Node will save them when it receives the first Message from them
	 * 
	 * @return totalSockets all the created sockets + all the received sockets 
	 */
	private List<Socket> socketsSetup() {
		System.out.println("\n*** INITIALIZING SOCKETS ***");

		// Each node initially creates N - NODE_ID - 1 sockets
		List<Socket> createdSockets = createSockets();

		// Now that we created the N - NODE_ID - 1 sockets, we send setup messages (in a
		// single thread)

		for (int i = 0; i < createdSockets.size(); ++i) {
			// We send a Message object
			Message msg = new Message(NODE_ID, -1);

			Utility.send(createdSockets.get(i), msg);

			System.out.println("*Send setup message: " + msg.getMessageID() + " on socket "
					+ createdSockets.get(i).getInetAddress().getCanonicalHostName() + ":"
					+ createdSockets.get(i).getPort());
		}

		// The remaining NODE_ID sockets, will be received from other nodes
		List<Socket> receivedSockets = receiveSockets();

		// Now, we merge together the created sockets with the received sockets
		// so that we obtain the global array of N (-1) sockets that we need in order to
		// communicate with all the other nodes
		List<Socket> totalSockets = new ArrayList<>();

		totalSockets.addAll(createdSockets);
		totalSockets.addAll(receivedSockets);
		return totalSockets;
	}

	/**
	 * Method that creates N - NODE_ID - 1 sockets
	 * @return createdSockets the list of created sockets
	 */
	private List<Socket> createSockets() {
		List<Socket> createdSockets = new ArrayList<>();

		for (int i = NODE_ID + 1; i < N; ++i) {
			// We create a socket towards node with ID = i

			String addr = nodes.get(i).split(":")[0];
			int port = Integer.parseInt(nodes.get(i).split(":")[1]);

			Socket s = null;
			try {
				s = new Socket(addr, port);
			} catch (IOException e) {
				e.printStackTrace();
			}

			createdSockets.add(s);
		}

		return createdSockets;
	}

	/**
	 * Method that receives NODE_ID sockets
	 * @return receivedSockets the list of received sockets
	 */
	private List<Socket> receiveSockets() {
		List<Socket> receivedSockets = new ArrayList<>();

		Thread[] threads = new Thread[NODE_ID];
		try {
			for (int i = 0; i < NODE_ID; ++i) {
				Socket s = receivingSocket.accept();
				receivedSockets.add(s);

				Thread t = new Thread(new NodeThreadSetup(s));
				threads[i] = t;
				t.start();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			for (Thread thread : threads) {
				thread.join();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return receivedSockets;
	}

	/**
	 * Method that sends M messages to all the other N - 1 nodes
	 * (1 thread only)
	 */
	private void sendToAll() {
		// Send msg only if generated random value is bigger than 0.05
		Random r = new Random(NODE_ID);

		for (int n_messages = 0; n_messages < M; ++n_messages) {
			// We send a Message object
			Message msg = new Message(NODE_ID, n_messages, "body " + n_messages);
			messages.add(msg);

			// Message msg = messages.get(n_messages);
			for (int i = 0; i < sockets.size(); ++i) {
				float randomVal = r.nextFloat();

				// If this is the last iteration, we send to every Node the termination message
				if(n_messages == M - 1) {
					randomVal = 1;
				}
					
				if (randomVal >= this.LP) {
					// if (true) {
					++numSent;

					System.out.println("*Send message: " + msg.getMessageID() + " on socket "
							+ sockets.get(i).getInetAddress().getCanonicalHostName() + ":" + sockets.get(i).getPort());

					long startTime = System.nanoTime();
					Utility.send(sockets.get(i), msg);
					long endTime = System.nanoTime();

					this.totTime += (endTime - startTime) * Math.pow(10, -6); // nanosecond --> milliseconds
				}

				else {
					++numLost;

					System.out.println("Message lost!");
					// Utility.send(sockets.get(i), null); //TODO: Forse si puo' anche non mettere?
				}

			}
		}

	}

	/**
	* Method that receives messages from all the other N - 1 nodes
	* (N - 1 threads concurrently listening, one for each socket)
	*/
	private void receiveFromAll() {
		Thread[] threads = new Thread[sockets.size()];

		for (int i = 0; i < sockets.size(); ++i) {
			Socket s = sockets.get(i);
			Thread t = new Thread(new NodeThreadMulticast(this, s));
			threads[i] = t;
			t.start();
		}

		try {
			for (Thread thread : threads) {
				thread.join();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Method that send the final statistics to the master
	 */
	private void sendStatisticsToMaster() {
		try (Socket masterSocket = new Socket(MASTER_ADDR, MASTER_PORT)) {
			Statistics statistics = new Statistics();
			statistics.nodeID = NODE_ID;
			statistics.totTime = this.totTime;
			statistics.avgTime = this.avgTime;
			statistics.numSent = this.numSent;
			statistics.numResent = this.numResent;
			statistics.numReceived = this.numReceived;
			statistics.numLost = this.numLost;

			System.out.println("Node sends its statistics to master, and terminate its execution");
			System.out.println(statistics);

			Utility.send(masterSocket, statistics);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Map<Integer, String> getNodes() {
		return nodes;
	}

	public static void main(final String[] args) throws IOException {
		if (args.length != 2) {
			System.out.println("Usage: java Node <NODE_ADDR> <NODE_PORT>");
			System.exit(1);
		}

		NODE_ADDR = args[0];
		NODE_PORT = Integer.parseInt(args[1]);

		new Node().start();
	}
}
