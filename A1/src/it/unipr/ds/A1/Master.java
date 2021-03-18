package it.unipr.ds.A1;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Class that defines a master node, which provides a naming service
 * for the communication nodes
 */
public class Master {
	private static final int COREPOOL = 5;
	private static final int MAXPOOL = 100;
	private static final long IDLETIME = 5000;
	
	private static final String PROPERTIES = "config.properties";
	
	private static String MASTER_ADDR;
	private static int MASTER_PORT;
	
	// flag setted to true when the administrator tells the Master (via console) to stop accepting new Nodes
	private static boolean END_REGISTRATION = false;
	
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
		
		// We use a concurrent hash map, since multiple threads can write on this data structure at once
		nodes = new ConcurrentHashMap<Integer, String>();
				
		/**
		 * An inner class that handles administrator inputs
		 * Any time an input is provided, the input is analyzed
		 * If the input string provided is equal to a special terminator string, we end the registration phase
		 * just by calling master.close()
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

				} while(!adminInput.equals("end"));
				
				master.close();
			}
			
		}
		// Master keep accepting new nodes until the administrator inserts a special input message via console, 
		// that states the end of the registration phase
		// This input message, which can be submitted at any time, is handled by a separated thread, as shown in the line below
		// N.B.: A class is needed, because we need the ability to refer "this" object (Master) in order to call this.close()
		// 		 the same approach is used in the while loop, when we create a new MasterThread
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

		this.pool.shutdown();
		
		System.out.println("\nRegistration phase terminated");
		System.out.println("Master collected the following " + nodes.size() + " nodes:");
		nodes.forEach((k,v) -> System.out.println("<" + k + "; " + v + ">"));
		System.out.println();
	}

	public ThreadPoolExecutor getPool() {
		return this.pool;
	}
	
	public Map<Integer, String> getNodes() {
		return nodes;
	}

	public void close() {
		try {
			// Thanks to this close(), we go  directly to this.pool.shutdown()
			this.mainSocket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Method that write on the properties file an (address, port) pair
	 * Used to initialize the config file after Master node starting
	 * 
	 * @param properties String that identifies the properties file
	 * @param address String that identifies the address (key)
	 * @param port Int that identifies the port (value)
	 * 
	 * @throws IOException
	 */
	public static void writeConfig(final String properties, final String address, final int port) throws IOException {
		//Create an OutputStream to write on file
        try (OutputStream output = new FileOutputStream(properties)) { 

        	Properties p = new Properties();
        	
            p.setProperty("master", address + "," + port);
            
            p.store(output, null);

        }
        catch (IOException io) {
            io.printStackTrace();        	
        }

	}
	
	public static void main(final String[] args) throws IOException {
		if (args.length != 2) {
			System.out.println("Usage: java Master <MASTER_ADDR> <MASTER_PORT>");
			System.exit(1);
		}

		MASTER_ADDR = args[0];
		MASTER_PORT = Integer.parseInt(args[1]);
		
		writeConfig(PROPERTIES, MASTER_ADDR, MASTER_PORT);
		
		new Master().run();
	}
}
