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
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Class that defines a generic peer (node) in the system, which can both send
 * and receive messages
 */
public class Node {
	// private static final int COREPOOL = 5;
	// private static final int MAXPOOL = 100;
	// private static final long IDLETIME = 5000;

	private static int MASTER_PORT;
	private static String MASTER_ADDR;

	private static int NODE_ID;
	private static String NODE_ADDR;
	private static int NODE_PORT;

	private static int MSG_ID;

	private static final String PROPERTIES = "config.properties";
	// Probability of error
	private float LP;

	// Number of messages
	private int M;

	private double totTime = 0;
	private double avgTime = 0;
	private int numSent = 0;
	private int numResent = 0;
	private int numReceived = 0;
	private int numLost = 0;

	// The queue of received messages
	// (shared by the main thread and the N-1 storing threads)
	private List<Deque<Message>> msgQueue = new ArrayList<>();

	// a map containing all the registered nodes,
	// in the form <key, val> where key = ID, val = ip:port
	private Map<Integer, String> nodes;

	// private ThreadPoolExecutor pool;

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

		// System.out.println("LP: " + this.LP);
		// System.out.println("M: " + this.M);
	}

	// The socket from which this Node will be receiving messages from other Nodes
	private ServerSocket receivingSocket;

	@SuppressWarnings("unchecked") // nodes = (Map<Integer, String>) o; (TODO make a "cleaner" cast)
	public void run() {
		System.out.println("Node running (" + NODE_ADDR + ":" + NODE_PORT + ")");

		// this.pool = new ThreadPoolExecutor(COREPOOL, MAXPOOL, IDLETIME,
		// TimeUnit.MILLISECONDS,
		// new LinkedBlockingQueue<Runnable>());

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

		System.out.println(MASTER_ADDR + ":" + MASTER_PORT);

		// Each node sends a registration string of this form: ip:port
		// id will be assigned by Master
		String registrationString = null;

		ObjectOutputStream os = null;
		ObjectInputStream is = null;

		// We open a socket towards the master, and we send our registration message
		try (Socket masterSocket = new Socket(MASTER_ADDR, MASTER_PORT)) {
			os = new ObjectOutputStream(masterSocket.getOutputStream());

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
			e.printStackTrace();
		}

		NODE_ID = Utility.getKey(nodes, registrationString);
		System.out.println("My ID is " + NODE_ID);

		// total number of nodes
		int N = nodes.size();

		// allocate space for every queue
		for (int i = 0; i < N; ++i)
			msgQueue.add(new ConcurrentLinkedDeque<>());

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

				long startTime = System.nanoTime();
				nodeOs.writeObject(msg);
				nodeOs.flush();
				long elapsedTime = (System.nanoTime() - startTime);

				this.totTime += elapsedTime * Math.pow(10, -6); // nanosecond --> milliseconds
			}

			// The remaining NODE_ID sockets, will be received from other nodes
			List<Socket> receivedSockets = new ArrayList<>();

			// The threads that will manage the reception of the inital(setup) messages from
			// other nodes
			List<Thread> setupThreads = new ArrayList<>();

			for (int i = 0; i < NODE_ID; ++i) {
				Socket s = receivingSocket.accept();
				receivedSockets.add(s);
				// this.pool.execute(new NodeThread(this, s));

				Thread t = new Thread(new Runnable() {
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
				});

				t.start();
				setupThreads.add(t);
			}

			// Wait for every receving thread to terminate
			for (int i = 0; i < setupThreads.size(); ++i)
				setupThreads.get(i).join();

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

			// Send msg only if generated random value is bigger than 0.05
			Random r = new Random(NODE_ID);

			for (int n_messages = 0; n_messages < M; ++n_messages) {
				// 1 thread send
				for (int i = 0; i < sockets.size(); ++i) {
					ObjectOutputStream nodeOs = new ObjectOutputStream(sockets.get(i).getOutputStream());

					// We send a Message object
					// TODO implement a "better" message id generation (incremental)

					Message msg = new Message(NODE_ID, MSG_ID);

					// If this is the last iteration, we send to every Node the termination message
					if (n_messages == M - 1)
						msg.setBody("end");

					float randomVal = r.nextFloat();
					// System.out.println("\t\tval: " + val);

					if (randomVal >= this.LP) {
						++numSent;

						System.out.println("*Send message: " + msg.getMessageID() + " on socket "
								+ sockets.get(i).getInetAddress().getCanonicalHostName() + ":"
								+ sockets.get(i).getPort());

						long startTime = System.nanoTime();
						nodeOs.writeObject(msg);
						nodeOs.flush();
						long elapsedTime = (System.nanoTime() - startTime);

						this.totTime += elapsedTime * Math.pow(10, -6); // nanosecond --> milliseconds
					}

					else {
						++numLost;

						System.out.println("Message lost!");
						nodeOs.writeObject(null);
						nodeOs.flush();
					}

				}

				++MSG_ID;
			}

			// N-1 threads receive
			List<Thread> receivingThreads = new ArrayList<>();

			for (int i = 0; i < sockets.size(); ++i) {
				Socket s = sockets.get(i);
				// this.pool.execute(new NodeThread(this, s));
				Thread t = new Thread(new Runnable() {
					ObjectOutputStream os = null;
					ObjectInputStream is = null;

					@Override
					public void run() {
						while (true) {

							try {
								is = new ObjectInputStream(s.getInputStream());

								Object obj = is.readObject();
								if (obj instanceof Message) {
									++numReceived;

									Message msg = (Message) obj;

									if(msg.getMessageID() > 0) {
										int lastMsgIdReceived = msgQueue.get(msg.getSenderID()).peekLast().getMessageID();
										int expectedMsgId = (msg.getMessageID() - 1);
										int senderExpectedMsg = (msg.getSenderID());

										if(lastMsgIdReceived != expectedMsgId) {
											System.out.println("Msg with id " + expectedMsgId + 
											" from node " + senderExpectedMsg +  " never arrived!");
											
											// Request message to ask which message resend
											//Message reqMsg = new Message(senderExpectedMsg, expectedMsgId);

											// Or maybe I can send just the ID of the message 
											// which must be sent back? I this case, *expectedMsgId*
											// if(os == null) {
											// 	os = new ObjectOutputStream(new BufferedOutputStream(s.getOutputStream()));
											// }

											// os.writeObject(expectedMsgId);
											// os.flush();
										}
										
									}
									
									msgQueue.get(msg.getSenderID()).add(msg); // TODO: handling msg queue

									if (msg.getBody().equals("end")) {
										// receivingSocket.close();
										s.close();
									}

									System.out.println("**Received message: " + msg.getMessageID() + " from node "
											+ msg.getSenderID());

									// La sleep si può mettere per vedere meglio l'esecuzione, ma non è necessaria
									// Thread.sleep(1000);
								}
								// else   {
								// 	Integer id = (Integer) obj;
								// 	System.out.println("Received request to resend message " + id + " from " + s);
								// }
							} catch (Exception e) {
								break;
							}
						}
					}

				});

				t.start();
				receivingThreads.add(t);
			}

			// Wait for every receving thread to terminate
			for (int i = 0; i < receivingThreads.size(); ++i)
				receivingThreads.get(i).join();

			this.avgTime = this.totTime / this.numSent;

		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println("\n\tMulticast exchange terminated correctly\n");

		// System.out.println("\n\nMessage queue list: " + msgQueue);

		// Now, we open a socket towards the master, and we send our statistics data
		try (Socket masterSocket = new Socket(MASTER_ADDR, MASTER_PORT)) {
			os = new ObjectOutputStream(masterSocket.getOutputStream());

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

			os.writeObject(statistics);
			os.flush();

			os.close();
		} catch (IOException e) {
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
