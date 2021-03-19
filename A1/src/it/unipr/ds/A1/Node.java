package it.unipr.ds.A1;

import java.io.BufferedInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Class that defines a generic peer (node) in the system,
 * which can both send and receive messages
 */
public class Node {
	private static AtomicInteger atomicInteger = new AtomicInteger();  
	
	private static int MASTER_PORT;
	private static String MASTER_ADDR;
	
	private static int NODE_ID;
	private static String NODE_ADDR;
	private static int NODE_PORT;
	
	private static final String PROPERTIES = "config.properties";
	
	/**
	 * Method that reads and parses a .properties file,
	 * containing the address and the port of the master node
	 * TODO Maybe this file could contain something else?
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
	
	public void run() {
		try {
			System.out.println("Node running (" + NODE_ADDR + ":" + NODE_PORT + ")");
			
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
			Socket masterSocket = new Socket(MASTER_ADDR, MASTER_PORT);

			ObjectOutputStream os = new ObjectOutputStream(masterSocket.getOutputStream());
			ObjectInputStream is = null;

			// Each node sends a registration string of this form: id;ip:port
//			String registrationString = NODE_ID + ";" + NODE_ADDR + ":" + NODE_PORT;
			String registrationString = NODE_ADDR + ":" + NODE_PORT;

			System.out.println("Node sends: " + registrationString + " to master");

			os.writeObject(registrationString);
			os.flush();
				
//			if (is == null) {
				is = new ObjectInputStream(new BufferedInputStream(masterSocket.getInputStream()));
//			}

			System.out.println("Waiting for start message from master");
			Object o = is.readObject();
			
			if (o instanceof Map<?, ?>) {
				@SuppressWarnings("unchecked")
				Map<Integer, String> nodes = (Map<Integer, String>) o;

				System.out.println("Received start message from master");
				System.out.println("The following nodes are registered in the system");
				nodes.forEach((id, addrAndPort) -> System.out.println("<" + id + "; " + addrAndPort + ">"));
				System.out.println();
			}
			
			masterSocket.close();
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	public static void main(final String[] args) {
		if (args.length != 2) {
			System.out.println("Usage: java Node <NODE_ADDR> <NODE_PORT>");
			System.exit(1);
		}
		
		// TODO: NODE_ID should not be read by command line, but should be generated incrementally
		//NODE_ID = Integer.parseInt(args[0]);
		NODE_ADDR = args[0];
		NODE_PORT = Integer.parseInt(args[1]);
		
		new Node().run();
	}
}
