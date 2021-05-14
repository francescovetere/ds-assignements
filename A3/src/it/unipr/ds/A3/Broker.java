package it.unipr.ds.A3;

import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import java.util.Scanner;

import org.apache.activemq.broker.BrokerFactory;
import org.apache.activemq.broker.BrokerService;

/**
 * Class that implements a JMS Broker
 * 
 * A Broker allows Clients and Coordinators to communicate (it's the bottleneck of the application)
 * It must be started as the first process
 * Then, Clients and Coordinators can be started in any order 
 */
public class Broker {
	private static final String URL = "tcp://localhost:61616";
	private static final String PROPS = "persistent=false&useJmx=false";

	/**
	 * Method that start Broker's execution
	 */
	public void start() {
		System.out.println("Broker running");

		try {
			BrokerService broker = BrokerFactory.createBroker("broker:(" + URL + ")?" + PROPS);

			broker.start();

			Scanner sc = new Scanner(System.in);

			while (true) {
				System.out.println("Insert 'stop' to terminate broker's execution");

				String line = sc.nextLine();

				if (line.equals("stop")) {
					broker.stop();
					System.out.println("Terminated");
					break;
				}
			}

			sc.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	* Method that reads a property contained in a .properties file
	* 
	* @param filename String that identifies the properties file
	* @param property String that identifies the property to be read
	* @return property value
	* @throws IOException
	*/
	public static String readConfig(final String filename, final String property) throws IOException {
		// Create a reader object on the properties file
		FileReader reader = new FileReader(filename);

		// Create properties object
		Properties p = new Properties();

		// Add a wrapper around reader object
		p.load(reader);

		// Access properties data
		return p.getProperty(property);
	}

	/**
	 * Main method
	 * @param args Vector of arguments (no args needed)
	 */
	public static void main(final String[] args) {
		new Broker().start();
	}
}
