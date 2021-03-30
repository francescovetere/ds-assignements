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
	
	/**
	 * Method that reads a map and a value, and return the first key in the map 
	 * that corresponds to that value
	 * @param <K> key type
	 * @param <V> value type
	 * @param map the map from which the key will be extracted
	 * @param value the value to search in the map
	 * @return the first key in the map that corresponds to value
	 */
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
	 * the port of the master node 
	 * TODO: Maybe this file could contain something else?
	 * 
	 * @param properties String that identifies the properties file
	 * @return a String, containing master_addr and master_port separated by ":"
	 * @throws IOException
	 */
	public static String readConfig(final String properties, final String property) throws IOException {
		// Create a reader object on the properties file
		FileReader reader = new FileReader(properties);

		// Create properties object
		Properties p = new Properties();

		// Add a wrapper around reader object
		p.load(reader);

		// Access properties data
		String serversProperty = p.getProperty(property);

		return serversProperty;
	}
	
	
	/**
	 * Method that writes on the .properties file all the necessary parameters for the system:
	 * master=address,port
	 * LP=0.05
	 * M=100
	 * Used to initialize the config file after Master node starting
	 * 
	 * @param properties String that identifies the properties file
	 * @param address    String that identifies the address (key)
	 * @param port       Int that identifies the port (value)
	 * @throws IOException
	 */
	public static void writeConfig(final String properties, final String address, final int port) throws IOException {
		// Create an OutputStream to write on file
		try (OutputStream output = new FileOutputStream(properties)) {
			Properties p = new Properties();
			p.setProperty("master", address + "," + port);
			p.setProperty("LP", "0.05f");
			p.setProperty("M", "10"); // TODO: all possible values for M

			p.store(output, null);
		} catch (IOException io) {
			io.printStackTrace();
		}

	}

}
