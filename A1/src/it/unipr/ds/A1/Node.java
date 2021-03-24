package it.unipr.ds.A1;

import java.io.BufferedInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Class that defines a generic peer (node) in the system, which can both send
 * and receive messages
 */
public class Node {
	private static final int COREPOOL = 5;
	private static final int MAXPOOL = 100;
	private static final long IDLETIME = 5000;

	private static int MASTER_PORT;
	private static String MASTER_ADDR;

	private static int NODE_ID;
	private static String NODE_ADDR;
	private static int NODE_PORT;
	
	private static int MSG_ID;

	private static final String PROPERTIES = "config.properties";
	
	// The queue of received messages 
	// (shared by the main thread and the N-1 storing threads)
	private Queue<Message> msgQueue = new LinkedBlockingQueue<>();
	
	// a map containing all the registered nodes,
	// in the form <key, val> where key = ID, val = ip:port
	private Map<Integer, String> nodes;
	
	private ThreadPoolExecutor pool;
	
	public Node() throws IOException {
		this.receivingSocket = new ServerSocket(NODE_PORT);
	}
	
	// The socket from which this Node will be receiving messages from other Nodes
	private ServerSocket receivingSocket;
	
	/**
	 * Method that reads and parses a .properties file, containing the address and
	 * the port of the master node TODO Maybe this file could contain something
	 * else?
	 * 
	 * @param properties String that identifies the properties file
	 * 
	 * @return a String, containing master_addr and master_port separated by ":"
	 * 
	 * @throws IOException
	 */
	private String readConfig(final String properties) throws IOException {
		// Create a reader object on the properties file
		FileReader reader = new FileReader(properties);

		// Create properties object
		Properties p = new Properties();

		// Add a wrapper around reader object
		p.load(reader);

		// Access properties data
		String serversProperty = p.getProperty("master");

		// Split over ";"
		String[] serversAndPorts = serversProperty.split(",");

		String address = serversAndPorts[0];
		int port = Integer.parseInt(serversAndPorts[1].trim());

		return address + ":" + port;
	}

	@SuppressWarnings("unchecked") // nodes = (Map<Integer, String>) o; (TODO make a "cleaner" cast)
	public void run() {
		System.out.println("Node running (" + NODE_ADDR + ":" + NODE_PORT + ")");
		
		this.pool = new ThreadPoolExecutor(COREPOOL, MAXPOOL, IDLETIME, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<Runnable>());
		
		// Reading .properties file
		String master = null;
		try {
			master = readConfig(PROPERTIES);
		} catch (IOException e) {
			System.out.println("File " + PROPERTIES + " not found");
			e.printStackTrace();
		}

		// Now, we parse the string obtained by reading the .properties file
		String[] masterAddrAndPort = master.split(":");
		MASTER_ADDR = masterAddrAndPort[0];
		MASTER_PORT = Integer.parseInt(masterAddrAndPort[1]);

		// We open a socket towards the master, and we send our registration message
		try (Socket masterSocket = new Socket(MASTER_ADDR, MASTER_PORT)) {

			ObjectOutputStream os = new ObjectOutputStream(masterSocket.getOutputStream());
			ObjectInputStream is = null;

			// Each node sends a registration string of this form: ip:port
			// id will be assigned by Master
			String registrationString = NODE_ADDR + ":" + NODE_PORT;

			System.out.println("Node sends: " + registrationString + " to master");

			os.writeObject(registrationString);
			os.flush();

			is = new ObjectInputStream(new BufferedInputStream(masterSocket.getInputStream()));

			System.out.println("Waiting for start message from master");
			Object o = is.readObject();

			// We expect to receive a Map<Integer, String> (i.e.: id --> <ip, port>)
			// If we receive something else, we terminate our execution
			// TODO: maybe we could terminate it more gracefully (?)
			if (!(o instanceof Map<?, ?>)) {
				System.out.println("Not a Map");
				System.exit(-1);
			}

			nodes = (Map<Integer, String>) o;

			System.out.println("Received start message from master");
			System.out.println("The following nodes are registered in the system");
			nodes.forEach((id, addrAndPort) -> System.out.println("<" + id + "; " + addrAndPort + ">"));
			System.out.println();

			NODE_ID = getKey(nodes, registrationString);
			System.out.println("My ID is " + NODE_ID);
			
			// total number of nodes
			int N = nodes.size();
			
			// i-th node must activate N-i-1 connections
			// in this way, we avoid creating N^2 connections, because we exploit 
			// the fact that TCP connections are bidirectional
			
			// int send = N - NODE_ID - 1;
			// int receive = NODE_ID;
			
			
			// We repeat the send - receive process for M times
			int M = 7; // TODO: Read M in a cleaner way
			for(int count = 0; count < M; ++count) {
				
				// TODO: Per ora non uso la coda di messaggi condivisa tra i thread, l'ho solo dichiarata all'inizio della classe
				// però probabilmente andrà usata (il prof la cita nelle note)
				
				// Send msg to every node with bigger ID
				for(int i = NODE_ID; i < N; ++i) {
					// We open a socket towards node with ID = i + 1
					int nodeID = (i + 1) % N;
					
					// per semplicità, per l'invio del msg, invece che fare this.pool.execute(...);
					// al momento creo il codice della run direttamente qui
					new Thread(new Runnable() {
						
						@Override
						public void run() {
							String addr = nodes.get(nodeID).split(":")[0];
							int port = Integer.parseInt(nodes.get(nodeID).split(":")[1]);
							
							try (Socket nodeSocket = new Socket(addr, port)) {

								ObjectOutputStream nodeOs = new ObjectOutputStream(nodeSocket.getOutputStream());

								// We send a Message object
								// TODO implement a "better" message id generation (incremental)
								//Random rnd = new Random();
								//int msgID = rnd.nextInt(M);
								
								Message msg = new Message(NODE_ID, MSG_ID);
								System.out.println("Node sends: " + msg.getMessageID() + " to node " + nodeID);

								//TODO send msg only if generated random value is bigger than 0,05 
								nodeOs.writeObject(msg);
								nodeOs.flush();
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}).start();
				}
								
				// Receive msg from every node with smaller ID
				for(int i = 0; i < NODE_ID; ++i) {
					Socket s = receivingSocket.accept();
					// Socket s is just needed in order to accept new connections from nodes
					// Each connection is then handled by a new thread, in the following way
					this.pool.execute(new NodeThread(this, s));
				}
				
				MSG_ID++;
			}
			
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();	
		}
	}
	
	public void close() {
		try {
			// Thanks to this close(), we go  directly to this.pool.shutdown()
			this.receivingSocket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public Map<Integer, String> getNodes() {
		return nodes;
	}

	public <K, V> K getKey(Map<K, V> map, V value) {
		for (Entry<K, V> entry : map.entrySet()) {
			if (entry.getValue().equals(value)) {
				return entry.getKey();
			}
		}
		return null;
	}

	public static void main(final String[] args) throws IOException {
		if (args.length != 2) {
			System.out.println("Usage: java Node <NODE_ADDR> <NODE_PORT>");
			System.exit(1);
		}

		NODE_ADDR = args[0];
		NODE_PORT = Integer.parseInt(args[1]);

		new Node().run();
	}
}
