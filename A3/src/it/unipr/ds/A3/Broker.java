package it.unipr.ds.A3;

import java.util.Scanner;
import org.apache.activemq.broker.BrokerFactory;
import org.apache.activemq.broker.BrokerService;

/**
 * Class that implements a JMS Broker.
 */
public class Broker {
	private static final String URL = "tcp://localhost:61616";
	private static final String PROPS = "persistent=false&useJmx=false";

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

	public static void main(final String[] args) {
		new Broker().start();
	}
}
