package it.unipr.ds.A1;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
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

	/**
	 * An inner class that handles administrator inputs 
	 * Any time an input is provided, the input is analyzed If the input string provided is equal to a
	 * special terminator string, we end the registration phase just by calling master.close()
	 */
	private class ConsoleInputHandler implements Runnable {
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

	public Master() {
		try {
			Utility.writeConfig(PROPERTIES, MASTER_ADDR, MASTER_PORT);
			this.mainSocket = new ServerSocket(MASTER_PORT);
		} catch (IOException e) {
			e.printStackTrace();
		}

		this.pool = new ThreadPoolExecutor(COREPOOL, MAXPOOL, IDLETIME, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<Runnable>());

		// We use a concurrent hash map, since multiple threads can write on this data
		// structure at once
		nodes = new ConcurrentHashMap<Integer, String>();
	}

	private void start() {
		System.out.println("Master running (" + MASTER_ADDR + ":" + MASTER_PORT + ")");

		// Master keep accepting new nodes until the administrator inserts a special input message via console,
		// that states the end of the registration phase
		// This input message, which can be submitted at any time, is handled by a separated thread
		// N.B.: A class is needed, because we need the ability to refer "this" object (Master) in order to call this.close()
		// 		 The same approach is used in the while loop, when we create a new MasterThread
		new Thread(new ConsoleInputHandler(this)).start();

		acceptNodesRegistrations();

		// At this point, the admin has entered the termination string, so we can notify all the threads in the pool
		// so that each of them will send to their specific node, the whole map of nodes
		synchronized (this.pool) {
			this.pool.notifyAll();
		}

		System.out.println("\nRegistration phase terminated");
		System.out.println("Master collected the following " + nodes.size() + " nodes:");
		nodes.forEach((id, addrAndPort) -> System.out.println("<" + id + "; " + addrAndPort + ">"));
		System.out.println();

		System.out.println("\nWaiting for nodes to terminate their multicast exchange...\n");

		// Here, the first node has terminated its multicast exchange, and start sending its Statistics object, which will
		// be handled by a new Thread
		acceptNodesStatistics();

		this.pool.shutdown();

		// TODO: devo capire se serve o meno quest'ultima parte...
		try {
			// Blocks until all tasks have completed execution after a shutdown request, or the timeout occurs, or the current thread is interrupted, whichever happens first.
			this.pool.awaitTermination(1, TimeUnit.HOURS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void acceptNodesRegistrations() {
		while (true) {
			try {
				Socket s = this.mainSocket.accept();

				// Socket s is just needed in order to accept new connections from nodes
				// Each connection is then handled by a new thread, in the following way
				this.pool.execute(new MasterThreadRegistration(this, s));
			} catch (Exception e) {
				break;
			}
		}
	}

	private void acceptNodesStatistics() {
		try {
			this.mainSocket = new ServerSocket(MASTER_PORT);
		} catch (IOException e) {
			e.printStackTrace();
		}

		for (int i = 0; i < nodes.size(); ++i) {
			try {
				Socket s = mainSocket.accept();
				this.pool.execute(new MasterThreadStatistics(s));
			} catch (IOException e) {
				e.printStackTrace();
			}
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

		new Master().start();
	}
}