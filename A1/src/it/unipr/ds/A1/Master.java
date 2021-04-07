package it.unipr.ds.A1;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import jxl.Workbook;
import jxl.write.WritableWorkbook;

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
	private static int M;
	private static float LP;

	// a map containing all the registered nodes,
	// in the form <key, val> where key = ID, val = ip:port
	private Map<Integer, String> nodes;

	private ServerSocket mainSocket;
	private ThreadPoolExecutor pool;

	// The map of received statistics, on which the N threads MasterThreadStatistics write
	public Map<Integer, Statistics> statsMap = new ConcurrentHashMap<>();

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
			Scanner sc = new Scanner(System.in);
			
			do {
				System.out.println("Type 'end' to end the registration phase");
//				adminInput = System.console().readLine();
				adminInput = sc.nextLine();

			} while (!adminInput.equals("end"));

			sc.close();
			master.close();
			
		}

	}

	public Master() {
		try {
			Utility.writeConfig(PROPERTIES, MASTER_ADDR, MASTER_PORT, M, LP);
			System.out.println(M);
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

		this.pool.shutdown();

		System.out.println("\nRegistration phase terminated");
		System.out.println("Master collected the following " + nodes.size() + " nodes:");
		nodes.forEach((id, addrAndPort) -> System.out.println("<" + id + "; " + addrAndPort + ">"));
		System.out.println();

		System.out.println("\nWaiting for nodes to terminate their multicast exchange...\n");

		// Here, the first node has terminated its multicast exchange, and start sending its Statistics object, which will
		// be handled by a new Thread
		acceptNodesStatistics();

		System.out.println("\nEvery node sent its statistics");

		for(int i = 0; i < statsMap.size(); ++i) {
			System.out.println(statsMap.get(i));
		}

		
//		System.out.println(Workbook.getVersion());
		String filename = "report-m-" + M + ".xls";
		Utility.createExcel(new File(filename), statsMap);
		
		System.out.println(filename + " generated correctly");
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
		Thread[] threads = new Thread[nodes.size()];

		try {
			this.mainSocket = new ServerSocket(MASTER_PORT);
		} catch (IOException e) {
			e.printStackTrace();
		}

		for (int i = 0; i < nodes.size(); ++i) {
			try {
				Socket s = mainSocket.accept();
				Thread t = new Thread(new MasterThreadStatistics(this, s));
				t.start();
				threads[i] = t;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// The master waits for all the nodes to send their statistics
		try {
			for (Thread thread : threads) {
				thread.join();
			}
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
		if (args.length != 4) {
			System.out.println("Usage: java Master <MASTER_ADDR> <MASTER_PORT> <M> <LP>");
			System.exit(1);
		}

		MASTER_ADDR = args[0];
		MASTER_PORT = Integer.parseInt(args[1]);
		M = Integer.parseInt(args[2]);
		LP = Float.parseFloat(args[3]);

		new Master().start();
	}
}
