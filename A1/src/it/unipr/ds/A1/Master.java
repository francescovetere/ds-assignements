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
