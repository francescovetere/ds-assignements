package it.unipr.ds.A1;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Class that defines a master node, which provides a naming service for the
 * communication nodes
 */
public class Master {
	private static final int COREPOOL = 10;
	private static final int MAXPOOL = 100;
	private static final long IDLETIME = 5000;

	private static final String PROPERTIES = "config.properties";

	private static String MASTER_ADDR;
	private static int MASTER_PORT;

	// a map containing all the registered nodes,
	// in the form <key, val> where key = ID, val = ip:port
	private Map<Integer, String> nodes;

	private ServerSocket mainSocket;
	private ThreadPoolExecutor pool;

	public Master() throws IOException {
		this.mainSocket = new ServerSocket(MASTER_PORT);
	}

	private void run() {
		System.out.println("Master running (" + MASTER_ADDR + ":" + MASTER_PORT + ")");

		this.pool = new ThreadPoolExecutor(COREPOOL, MAXPOOL, IDLETIME, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<Runnable>());

		// We use a concurrent hash map, since multiple threads can write on this data
		// structure at once
		nodes = new ConcurrentHashMap<Integer, String>();

		/**
		 * An inner class that handles administrator inputs Any time an input is
		 * provided, the input is analyzed If the input string provided is equal to a
		 * special terminator string, we end the registration phase just by calling
		 * master.close()
		 */
		class ConsoleInputHandler implements Runnable {

			private Master master; // reference to the Master node (necessary in order to call master.close() )

			public ConsoleInputHandler(final Master master) {
				this.master = master;
			}

			@Override
			public void run() {
				String adminInput;

				do {
					System.out.println("Type 'end' to end the registration phase");
					adminInput = System.console().readLine();

				} while (!adminInput.equals("end"));

				master.close();
			}

		}
		// Master keep accepting new nodes until the administrator inserts a special
		// input message via console,
		// that states the end of the registration phase
		// This input message, which can be submitted at any time, is handled by a
		// separated thread, as shown in the line below
		// N.B.: A class is needed, because we need the ability to refer "this" object
		// (Master) in order to call this.close()
		// the same approach is used in the while loop, when we create a new
		// MasterThread
		new Thread(new ConsoleInputHandler(this)).start();

		while (true) {
			try {
				Socket s = this.mainSocket.accept();

				// Socket s is just needed in order to accept new connections from nodes
				// Each connection is then handled by a new thread, in the following way
				this.pool.execute(new MasterThread(this, s));
			} catch (Exception e) {
				break;
			}
		}

		// At this point, the admin has entered the termination string, so we can notify
		// all the threads in the pool
		// So that each of them will send to their specific node, the whole map of nodes
		synchronized (this.pool) {
			this.pool.notifyAll();
		}

		// Finally, we shut down the pool: from now on, there will be no more
		// connections between Master and Nodes
		// this.pool.shutdown();

		System.out.println("\nRegistration phase terminated");
		System.out.println("Master collected the following " + nodes.size() + " nodes:");
		nodes.forEach((id, addrAndPort) -> System.out.println("<" + id + "; " + addrAndPort + ">"));
		System.out.println();

		System.out.println("\nWaiting for node termination...");
		// System.out.println(this.atom.incrementAndGet());

		try {
			this.mainSocket = new ServerSocket(MASTER_PORT);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// The threads that will manage the reception of the inital(setup) messages from
		// other nodes
		List<Thread> receivingThreads = new ArrayList<>();

		try {
			for (int i = 0; i < nodes.size(); ++i) {
				Socket s = mainSocket.accept();
				Thread t = new Thread(new Runnable() {
					@Override
					public void run() {
						ObjectInputStream is = null;

						try {
							is = new ObjectInputStream(s.getInputStream());

							Object obj = is.readObject();

							if (obj instanceof Statistics) {
								Statistics statistics = (Statistics) obj;
								System.out.println("**Received termination message from Node " + statistics.nodeID
										+ ":\n" + statistics);
							}

						} catch (Exception e) {
							e.printStackTrace();
							System.exit(-1);
						}

					}
				});

				t.start();
				receivingThreads.add(t);
			}

			// Wait for every receving thread to terminate
			for (int i = 0; i < receivingThreads.size(); ++i)
				receivingThreads.get(i).join();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public ThreadPoolExecutor getPool() {
		return this.pool;
	}

	public Map<Integer, String> getNodes() {
		return nodes;
	}

	public void close() {
		try {
			// Thanks to this close(), we go directly to this.pool.shutdown()
			this.mainSocket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(final String[] args) throws IOException {
		if (args.length != 2) {
			System.out.println("Usage: java Master <MASTER_ADDR> <MASTER_PORT>");
			System.exit(1);
		}

		MASTER_ADDR = args[0];
		MASTER_PORT = Integer.parseInt(args[1]);

		Utility.writeConfig(PROPERTIES, MASTER_ADDR, MASTER_PORT);

		new Master().run();
	}
}
