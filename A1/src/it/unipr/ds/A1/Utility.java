package it.unipr.ds.A1;

import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

/*
 * Class containing helpful static methods used across other classes
 */
public class Utility {
	
	public static <K, V> K getKey(Map<K, V> map, V value) {
		for (Entry<K, V> entry : map.entrySet()) {
			if (entry.getValue().equals(value)) {
				return entry.getKey();
			}
		}
		return null;
	}
	
	
	/**
	 * Method that reads and parses a .properties file, containing the address and
	 * the port of the master node TODO Maybe this file could contain something
	 * else?
	 * 
	 * @param properties String that identifies the properties file
	 * 
	 * @return a String, containing master_addr and master_port separated by ":"
	 * 
	 * @throws IOException
	 */
	public static String readConfig(final String properties) throws IOException {
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
	
	
	/**
	 * Method that write on the .properties file an (address, port) pair 
	 * Used to initialize the config file after Master node starting
	 * 
	 * @param properties String that identifies the properties file
	 * @param address    String that identifies the address (key)
	 * @param port       Int that identifies the port (value)
	 * 
	 * @throws IOException
	 */
	public static void writeConfig(final String properties, final String address, final int port) throws IOException {
		// Create an OutputStream to write on file
		try (OutputStream output = new FileOutputStream(properties)) {
			Properties p = new Properties();
			p.setProperty("master", address + "," + port);
			p.store(output, null);
		} catch (IOException io) {
			io.printStackTrace();
		}

	}

}
