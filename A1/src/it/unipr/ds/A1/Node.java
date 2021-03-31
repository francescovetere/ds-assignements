package it.unipr.ds.A1;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Class that defines a generic peer (node) in the system, which can both send
 * and receive messages
 */
public class Node {
	// private static final int COREPOOL = 5;
	// private static final int MAXPOOL = 100;
	// private static final long IDLETIME = 5000;
	// private ThreadPoolExecutor pool;

	private static int MASTER_PORT;
	private static String MASTER_ADDR;

	private static int NODE_ID;
	private static String NODE_ADDR;
	private static int NODE_PORT;

	// Each node sends a registration string of this form: ip:port
	// id will be assigned by Master
	String registrationString = null;

	private int MSG_ID = 0;

	private static final String PROPERTIES = "config.properties";
	// Probability of error
	private float LP;

	// Number of messages
	private int M;

	// Number of nodes
	private int N;

	// List of sockets to communicate with other N - 1 nodes
	List<Socket> sockets;

	// Statistics variables
	private double totTime = 0;
	private double avgTime = 0;
	private int numSent = 0;
	private int numResent = 0;
	private int numReceived = 0;
	private int numLost = 0;

	// 2 variabili che servono solamente per capire quando uscire dal while(true) dello scambio multicast
	private int numMissing = 0;
	private int numEnded = 0;

	// The queue of received messages
	// (shared by the main thread and the N-1 storing threads)
	private List<Deque<Message>> msgQueue = new ArrayList<>();

	// a map containing all the registered nodes,
	// in the form <key, val> where key = ID, val = ip:port
	private Map<Integer, String> nodes;

	public Node() throws IOException {
		this.receivingSocket = new ServerSocket(NODE_PORT);

		// this.pool = new ThreadPoolExecutor(COREPOOL, MAXPOOL, IDLETIME,
		// TimeUnit.MILLISECONDS,
		// new LinkedBlockingQueue<Runnable>());

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

	// The socket from which this Node will be receiving messages from other Nodes
	private ServerSocket receivingSocket;

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
		for (int i = 0; i < N; ++i)
			msgQueue.add(new ConcurrentLinkedDeque<>());

		try {
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

			System.out.println("Here");
			this.avgTime = this.totTime / this.numSent;

		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println("\n\tMulticast exchange terminated correctly\n");

		// System.out.println("\n\nMessage queue list: " + msgQueue);

		// // Now, we open a socket towards the master, and we send our statistics data
		// try (Socket masterSocket = new Socket(MASTER_ADDR, MASTER_PORT)) {
		// 	masterOs = new ObjectOutputStream(masterSocket.getOutputStream());

		// 	Statistics statistics = new Statistics();
		// 	statistics.nodeID = NODE_ID;
		// 	statistics.totTime = this.totTime;
		// 	statistics.avgTime = this.avgTime;
		// 	statistics.numSent = this.numSent;
		// 	statistics.numResent = this.numResent;
		// 	statistics.numReceived = this.numReceived;
		// 	statistics.numLost = this.numLost;

		// 	System.out.println("Node sends its statistics to master, and terminate its execution");
		// 	System.out.println(statistics);

		// 	masterOs.writeObject(statistics);
		// 	masterOs.flush();

		// 	masterOs.close();
		// } catch (IOException e) {
		// 	e.printStackTrace();
		// }
	}

	@SuppressWarnings("unchecked")
	private Map<Integer, String> registrateToMaster() {
		ObjectOutputStream masterOs = null;
		ObjectInputStream masterIs = null;

		// We open a socket towards the master, and we send our registration message
		try (Socket masterSocket = new Socket(MASTER_ADDR, MASTER_PORT)) {
			masterOs = new ObjectOutputStream(masterSocket.getOutputStream());

			System.out.println("Node sends: " + registrationString + " to master");

			masterOs.writeObject(registrationString);
			masterOs.flush();

			masterIs = new ObjectInputStream(new BufferedInputStream(masterSocket.getInputStream()));

			System.out.println("Waiting for start message from master");
			Object o = masterIs.readObject();

			// We expect to receive a Map<Integer, String> (i.e.: id --> <ip, port>)
			// If we receive something else, we terminate our execution
			// TODO: maybe we could terminate it more gracefully (?)

			if (!(o instanceof Map<?, ?>)) {
				System.out.println("Didn't received correctly the Map of nodes");
				System.exit(-1);
			}

			nodes = (Map<Integer, String>) o;

		} catch (IOException | ClassNotFoundException e) {
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
			Message msg = new Message(NODE_ID, MSG_ID);

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

		// The threads that will manage the reception of the inital(setup) messages from
		// other nodes
		List<Thread> setupThreads = new ArrayList<>();

		for (int i = 0; i < NODE_ID; ++i) {
			Thread t = null;

			try {
				Socket s = receivingSocket.accept();
				receivedSockets.add(s);

				t = new Thread(new Runnable() {
					@Override
					public void run() {
						Object obj = Utility.receive(s);

						if (obj instanceof Message) {
							Message msg = (Message) obj;

							System.out.println("**Received setup message: " + msg.getMessageID() + " from node "
									+ msg.getSenderID());

						}

					}
				});
			} catch (Exception e) {
				e.printStackTrace();
			}

			t.start();
			setupThreads.add(t);
		}

		// Wait for every receving thread to terminate
		try {
			for (int i = 0; i < setupThreads.size(); ++i)
				setupThreads.get(i).join();
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
			for (int i = 0; i < sockets.size(); ++i) {
				// We send a Message object
				Message msg = new Message(NODE_ID, MSG_ID);

				// If this is the last iteration, we send to every Node the termination message
				if (n_messages == M - 1)
					msg.setBody("end");

				float randomVal = r.nextFloat();
				
				if (randomVal >= this.LP || msg.getBody().equals("end")) {
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
					Utility.send(sockets.get(i), null);
				}

			}

			++MSG_ID;
		}
	}

	/**
	* Method that receives messages from all the other N - 1 nodes
	* (N - 1 threads concurrently listening, one for each socket)
	*/
	private void receiveFromAll() {
		List<Thread> receivingThreads = new ArrayList<>();
		// List<Message> lostMessages = new ArrayList<>();

		for (int i = 0; i < sockets.size(); ++i) {
			Socket s = sockets.get(i);

			Thread t = new Thread(new Runnable() {
				@Override
				public void run() {
					ObjectInputStream is = null;

					while (true) {
						System.out.println("===" + " numMissing: " + numMissing + " numResent: " + numResent
								+ " numLost: " + numLost + " numEnded: " + numEnded + " ===");

						try {
							is = new ObjectInputStream(s.getInputStream());

							Object obj = is.readObject();

							if (obj instanceof Message) {
								Message msg = (Message) obj;

								// L'errore era qui: quando arrivava un messaggio di tipo "lost",
								// entrava comunque nell'if ma al momento dell'assegnamento di *lastMsgIdReceived*
								// generava un'eccezione

								// Check if the received message (except the first)is correct according to the last message in the queue
								if (!msgQueue.get(msg.getSenderID()).isEmpty()) {
									if (!msg.getBody().equals("lost") && !msg.getBody().equals("resent")) {

										int lastMsgIdReceived = msgQueue.get(msg.getSenderID()).peekLast()
												.getMessageID();
										int expectedMsgId = lastMsgIdReceived + 1;
										int senderExpectedMsg = (msg.getSenderID());

										// System.out.println("**********************************");
										// System.out.println("lastIDreceived: " + lastMsgIdReceived);
										// System.out.println("expectedIDreceived: " + expectedMsgId);
										// System.out.println("currentIDreceived: " + msg.getMessageID());
										// System.out.println("**********************************");

										if (msg.getMessageID() != expectedMsgId) {
											int diff = msg.getMessageID() - expectedMsgId;

											for (int i = diff; i >= 1; i--) {
												try {
													ObjectOutputStream os = null;

													if (os == null)
														os = new ObjectOutputStream(
																new BufferedOutputStream(s.getOutputStream()));

													// System.out.println("Msg with id " + expectedMsgId + " from node "
													// 		+ senderExpectedMsg + " never arrived!");

													// Request message to ask which message resend
													Message reqMsg = new Message(senderExpectedMsg,
															msg.getMessageID() - diff, "lost");

													System.out.println("*Send request for lost message: "
															+ reqMsg.getMessageID() + " via " + s);
													// + " to node " + reqMsg.getSenderID());

													os.writeObject(reqMsg);
													os.flush();
												} catch (IOException e) {
													e.printStackTrace();
												}

												// Scopro che un messaggio che non mi è arrivato, quindi incremento i messaggi mancanti
												synchronized (this) {
													++numMissing;
												}

											}

										}
									}

								}
								//Handle the case if was arrived the first message but its ID isn't 0 
								else if (msgQueue.get(msg.getSenderID()).isEmpty() & msg.getMessageID() != 0) {
									if (!msg.getBody().equals("lost") && !msg.getBody().equals("resent")) {

										int senderExpectedMsg = (msg.getSenderID());

										for (int i = 0; i < msg.getMessageID(); i++) {
											try {
												ObjectOutputStream os = null;

												if (os == null)
													os = new ObjectOutputStream(
															new BufferedOutputStream(s.getOutputStream()));

												// Request message to ask which message resend
												Message reqMsg = new Message(senderExpectedMsg, i, "lost");

												System.out.println("*Send request for lost message: "
														+ reqMsg.getMessageID() + " via " + s);
												// + " to node " + reqMsg.getSenderID());

												os.writeObject(reqMsg);
												os.flush();
											} catch (IOException e) {
												e.printStackTrace();
											}

											// Scopro che un messaggio che non mi è arrivato, quindi incremento i messaggi mancanti
											synchronized (this) {
												++numMissing;
											}
										}
									}
								}

								msgQueue.get(msg.getSenderID()).add(msg);

								if (msg.getBody().equals("lost")) { // TODO: Not working properly...

									// Immediately resend requested message
									// Invio un messaggio che non era arrivato ad un nodo, quindi incremento i messaggi reinviati
									synchronized (this) {
										++numResent;
									}

									System.out.println("**Received request to resend message: " + msg.getMessageID()
									// + " from node " + msg.getSenderID()
											+ " via " + s + "\n*Send previously lost message: " + msg.getMessageID()
											+ " to node " + msg.getSenderID());

									ObjectOutputStream os = new ObjectOutputStream(
											new BufferedOutputStream(s.getOutputStream()));

									msg.setBody("resent");

									os.writeObject(msg);
									os.flush();

								} else if (msg.getBody().equals("end")) { // TODO: Not working properly... (?)
									System.out.println("**Received termination message: " + msg.getMessageID()
									// + " from node " + msg.getSenderID());
											+ " via " + s);

									// Ricevo un messaggio di end, quindi incremento il numero di nodi che mi hanno segnalato di aver finito
									synchronized (this) {
										++numReceived;
										++numEnded;
									}

								} else if (msg.getBody().equals("resent")) { // TODO: Not working properly... (?)
									// Ricevo un messaggio che non mi era arrivato, quindi decremento i messaggi mancanti
									synchronized (this) {
										--numMissing;
									}

									System.out.println("**Received previously lost message: " + msg.getMessageID()
									// + " from node " + msg.getSenderID());
											+ " via " + s);

								} else {
									synchronized (this) {
										++numReceived;
									}

									System.out.println("**Received message: " + msg.getMessageID() + " from node "
											+ msg.getSenderID());
								}
							}
						} catch (Exception e) {
							e.printStackTrace();
							break;
						}

						// Se non mi manca alcun messaggio da ricevere dagli altri nodi
						// E se ho inviato reinviato agli altri nodi tutti i messaggi che avevo inzialmente perso
						// E se ho ricevuto N - 1 messaggi di end
						// ===> allora posso uscire dal while
						if ((numMissing == 0) & (numResent == numLost) & (numEnded == N - 1)) {
							System.out.println("*** Per me si può chiudere ***");
							// try {
							// 	s.close();
							// } catch (Exception e) {
							// 	e.printStackTrace();
							// }

							// break;
						}
					}
				}

			});

			t.start();
			receivingThreads.add(t);
		}

		// Wait for every receving thread to terminate
		try {
			for (int i = 0; i < receivingThreads.size(); ++i)
				receivingThreads.get(i).join();
		} catch (InterruptedException e) {
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
