package it.unipr.ds.A1;

import java.io.BufferedInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Queue;
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
	// private Queue<Message> msgQueue = new LinkedBlockingQueue<>();

	// a map containing all the registered nodes,
	// in the form <key, val> where key = ID, val = ip:port
	private Map<Integer, String> nodes;

	private ThreadPoolExecutor pool;
	
	public Node() throws IOException {
		this.receivingSocket = new ServerSocket(NODE_PORT);
	}

	// The socket from which this Node will be receiving messages from other Nodes
	private ServerSocket receivingSocket;


	@SuppressWarnings("unchecked") // nodes = (Map<Integer, String>) o; (TODO make a "cleaner" cast)
	public void run() {
		System.out.println("Node running (" + NODE_ADDR + ":" + NODE_PORT + ")");

		this.pool = new ThreadPoolExecutor(COREPOOL, MAXPOOL, IDLETIME, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<Runnable>());

		// Reading .properties file to get Master address and port
		String master = null;
		try {
			master = Utility.readConfig(PROPERTIES);
		} catch (IOException e) {
			System.out.println("File " + PROPERTIES + " not found");
			e.printStackTrace();
		}

		// Now, we parse the string obtained by reading the .properties file
		String[] masterAddrAndPort = master.split(":");
		MASTER_ADDR = masterAddrAndPort[0];
		MASTER_PORT = Integer.parseInt(masterAddrAndPort[1]);

		// Each node sends a registration string of this form: ip:port
		// id will be assigned by Master
		String registrationString = null;

		// We open a socket towards the master, and we send our registration message
		try (Socket masterSocket = new Socket(MASTER_ADDR, MASTER_PORT)) {

			ObjectOutputStream os = new ObjectOutputStream(masterSocket.getOutputStream());
			ObjectInputStream is = null;

			registrationString = NODE_ADDR + ":" + NODE_PORT;

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
		} catch (IOException | ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		NODE_ID = Utility.getKey(nodes, registrationString);
		System.out.println("My ID is " + NODE_ID);

		// total number of nodes
		int N = nodes.size();

		try {
			/****************************/
			/*** INITIALIZING SOCKETS ***/
			/****************************/
			/*
			 * Node i must activate N-i-1 connections. In this way, we avoid creating N^2
			 * connections, because we exploit the fact that TCP connections are
			 * bidirectional
			 * 
			 * In the end, we want for each node N sockets N - NODE_ID - 1 will be created
			 * from the Node itself. The other NODE_ID sockets will be created from other
			 * Nodes, and this current Node will save them when it receives the first
			 * Message from them
			 */
			System.out.println("\n*** INITIALIZING SOCKETS ***");
			List<Socket> createdSockets = new ArrayList<>();

			// Each node initially creates N - NODE_ID - 1 sockets
			for (int i = NODE_ID + 1; i < N; ++i) {
				// We create a socket towards node with ID = i

				String addr = nodes.get(i).split(":")[0];
				int port = Integer.parseInt(nodes.get(i).split(":")[1]);

				createdSockets.add(new Socket(addr, port));
			}

			System.out.println("\n---Created " + createdSockets.size() + " sockets---");
			for (int i = 0; i < createdSockets.size(); ++i) {
				System.out.println("\t<" + createdSockets.get(i).getInetAddress().getCanonicalHostName() + ":"
						+ createdSockets.get(i).getPort() + ">");
			}

			// Now that we created the N - NODE_ID - 1 sockets, we send messages (in a
			// single thread)
			for (int i = 0; i < createdSockets.size(); ++i) {
				ObjectOutputStream nodeOs = new ObjectOutputStream(createdSockets.get(i).getOutputStream());

				// We send a Message object
				// TODO implement a "better" message id generation (incremental)

				Message msg = new Message(NODE_ID, MSG_ID);

				System.out.println("*Send setup message: " + msg.getMessageID() + " on socket "
						+ createdSockets.get(i).getInetAddress().getCanonicalHostName() + ":"
						+ createdSockets.get(i).getPort());

				// TODO send msg only if generated random value is bigger than 0,05
				nodeOs.writeObject(msg);
				nodeOs.flush();
			}

			// The remaining NODE_ID sockets, will be received from other nodes
			List<Socket> receivedSockets = new ArrayList<>();
			for (int i = 0; i < NODE_ID; ++i) {
				Socket s = receivingSocket.accept();
				receivedSockets.add(s);
				// this.pool.execute(new NodeThread(this, s));
				new Thread(new Runnable() {
					@Override
					public void run() {
						ObjectInputStream is = null;

						try {
							is = new ObjectInputStream(s.getInputStream());

							Object obj = is.readObject();

							if (obj instanceof Message) {
								Message msg = (Message) obj;

								System.out.println("**Received setup message: " + msg.getMessageID() + " from node "
										+ msg.getSenderID());
							}

						} catch (Exception e) {
							e.printStackTrace();
							System.exit(-1);
						}

					}
				}).start();
			}

			System.out.println("\n---Received " + receivedSockets.size() + " sockets---");
			for (int i = 0; i < receivedSockets.size(); ++i) {
				// N.B.: We're printing the local address and port that we created with the
				// accept()
				// But it doesn't matter, because sockets are bidirectional
				System.out.println("\t<" + receivedSockets.get(i).getInetAddress().getCanonicalHostName() + ":"
						+ receivedSockets.get(i).getPort() + ">");
			}

			// Now, we merge together the created sockets with the received sockets
			// so that we obtain the global array of N (-1) sockets that we need in order to
			// communicate with all the other nodes
			createdSockets.addAll(receivedSockets);
			List<Socket> sockets = createdSockets;

			System.out.println("\n---Total " + sockets.size() + " sockets---");
			for (int i = 0; i < sockets.size(); ++i) {
				System.out.println("\t<" + sockets.get(i).getInetAddress().getCanonicalHostName() + ":"
						+ sockets.get(i).getPort() + ">");
			}

			/********************************/
			/*** END INITIALIZING SOCKETS ***/
			/********************************/

			// At this point, we have our N sockets:
			// we can begin our multicast protocol: 1 thread send M messages to all the
			// sockets, the other N-1 threads receives messages from other
			// we repeat the process M times

			/********************************/
			/****** MULTICAST EXCHANGE *****/
			/********************************/

			/**********
			 * TODO ********** Quella che segue è una possibile implementazione del
			 * multicast ancora così non funziona perfettamente, probabilmente ci manca un
			 * qualche tipo di sincronizzazione con la coda di cui parlava il prof
			 * (msgQueue)
			 */

			int M = 5;

			for (int n_messages = 0; n_messages < M; ++n_messages) {
				// 1 thread send
				for (int i = 0; i < sockets.size(); ++i) {
					ObjectOutputStream nodeOs = new ObjectOutputStream(sockets.get(i).getOutputStream());

					// We send a Message object
					// TODO implement a "better" message id generation (incremental)

					Message msg = new Message(NODE_ID, MSG_ID);
					System.out.println("*Send message: " + msg.getMessageID() + " on socket "
							+ sockets.get(i).getInetAddress().getCanonicalHostName() + ":" + sockets.get(i).getPort());

					// TODO send msg only if generated random value is bigger than 0,05
					nodeOs.writeObject(msg);
					nodeOs.flush();
				}

				++MSG_ID;
			}

			// N-1 threads receive
			for (int i = 0; i < sockets.size(); ++i) {
				Socket s = sockets.get(i);
				// this.pool.execute(new NodeThread(this, s));
				new Thread(new Runnable() {
					@Override
					public void run() {
						while (true) {
							ObjectInputStream is = null;

							try {
								is = new ObjectInputStream(s.getInputStream());

								Object obj = is.readObject();

								if (obj instanceof Message) {
									Message msg = (Message) obj;

									System.out.println("**Received message: " + msg.getMessageID() + " from node "
											+ msg.getSenderID());

									// La sleep si può mettere per vedere meglio l'esecuzione, ma non è necessaria
									// Thread.sleep(1000);
								}
							} catch (Exception e) {
								e.printStackTrace();
								System.exit(0);
							}
						}
					}

				}).start();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void close() {
		try {
			// Thanks to this close(), we go directly to this.pool.shutdown()
			this.receivingSocket.close();
		} catch (Exception e) {
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

		new Node().run();
	}
}
