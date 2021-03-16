package it.unipr.ds.A1;

import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Class that defines a generic peer (node) in the system,
 * which can both send and receive messages
 *
 */
public class Node {
	private static final float LP = 0.05f;
	private static final int M = 100;
	private static final int N = 10;
	private static final String PROPERTIES = "config.properties";
	
	private final int id = 0;
	private static String ADDRESS = null;
	private static int PORT = 0;
	
	private static final int COREPOOL = 5;
	private static final int MAXPOOL = 100;
	private static final long IDLETIME = 5000;
	
	private ServerSocket socket;
	private ThreadPoolExecutor pool;

	public Node(final int PORT) throws IOException {
		this.socket = new ServerSocket(PORT);
	}
	
	/**
	 * Method that reads and parses a .properties file,
	 * containing the address and the port of the master node
	 * TODO Maybe this file could contain something else?
	 * 
	 * @param properties string that identifies the properties file
	 * 
	 * @return a Pair<String, Integer> object, containing <master_addr, master_port>
	 * 
	 * @throws IOException
	 */
	private Pair<String, Integer> readConfig(final String properties) throws IOException {
		
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

		return new Pair<String, Integer>(address, port);
	}

	private void run() {
		System.out.println("Node started (" + ADDRESS + ":" + PORT + ")");
		// Lettura del file .properties
		Pair<String, Integer> master = null;
		try {
			master = readConfig(PROPERTIES);
		} catch (IOException e) {
			System.out.println("File " + PROPERTIES + " not found");
			e.printStackTrace();
		}
		
		// Ora, master.getFirst() e master.getSecond() mi danno indirizzo e porta del master node
		// Attendo quindi dal master l'invio del messaggio di start
		
		System.out.println("Waiting for master node (" + master.getFirst() + ":" + master.getSecond() + ") start message");
		// Fase di invio degli M messaggi
		
		// TODO
		
		// Fase di ricezione degli M messaggi
		
		// TODO
		// Possibile gestione del pool di thread
		/*
		this.pool = new ThreadPoolExecutor(COREPOOL, MAXPOOL, IDLETIME, TimeUnit.MILLISECONDS,
		new LinkedBlockingQueue<Runnable>());

		while (true) {
			try {
				Socket s = this.socket.accept();
				
				// La socket s serve solamente per accettare nuove connessioni
				// Queste vengono poi smistate in un nuovo thread a parte, nel seguente modo
				this.pool.execute(new NodeThread(this, s));
			} catch (Exception e) {
				break;
			}
		}

		this.pool.shutdown();
		*/
	}

	public ThreadPoolExecutor getPool() {
		return this.pool;
	}

	public void close() {
		try {
			this.socket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(final String[] args) throws IOException {
		/* Ad esempio, da riga di comando, potrebbe essere qualcosa di questo tipo:
		 * /home/francesco/eclipse/plugins/org.eclipse.justj.openjdk.hotspot.jre.full.linux.x86_64_15.0.1.v20201027-0507/jre/bin/java 
		 * -classpath /home/francesco/Desktop/SD/lab/ds-assignements/A1/bin it.unipr.ds.A1.Node localhost 4000
		 */
		if (args.length != 2) {
			System.out.println("Usage: java Node <address> <port>");
			System.exit(1);
		}

		ADDRESS = args[0];
		PORT = Integer.parseInt(args[1]);
		
		new Node(PORT).run();
	}
}
