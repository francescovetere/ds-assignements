package it.unipr.ds.A3;

import java.util.Random;

import javax.jms.JMSException;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;

import it.unipr.ds.A3.Request.Type;

/**
 * Class that implements a client.
 **/
public class Client {
	private static final String BROKER_URL = "tcp://localhost:61616";
	private static final String TOPIC_NAME = "topic";
	private final int id;

	// Max time between two requests submit
	private static final int MAX_SLEEP = 5000;

	// Time for simulating a read operation
	private static final int READ_TIME = 3000;

	// Time for simulating a write operation
	private static final int WRITE_TIME = 5000;

	public Client(int id) {
		this.id = id;
	}

	public void start() throws InterruptedException {
		System.out.println("Client " + this.id + " running");

		ActiveMQConnection connection = null;

		// Random number, useful for decide when to submit a request, and of which type the request will be
		Random random = new Random();

		// The type of the request
		Type requestType = null;

		try {
			ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory(Client.BROKER_URL);
			connection = (ActiveMQConnection) cf.createConnection();
			connection.start();

			// Client publish its request, waiting for subscribers (Coordinators) to handle this request
			TopicSession session = connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

			Topic topic = session.createTopic(TOPIC_NAME);

			TopicPublisher publisher = session.createPublisher(topic);

			while (true) {
				// We sleep for a random time before submitting a new request
				Thread.sleep(random.nextInt(MAX_SLEEP));

				// We send either a read or a write request, randomly
				if (random.nextDouble() < 0.5)
					requestType = Type.READ;
				else
					requestType = Type.WRITE;

				System.out.println("Sending " + requestType + " request to coordinators");

				ObjectMessage msg = session.createObjectMessage();
				msg.setObject(new Request(this.id, requestType));

				publisher.publish(msg);

				// Now, we wait for the votes
				// If the request was a read and we reached the read quorum, we sleep for READ_TIME ms
				// If the request was a write and we reached the write quorum, we sleep for WRITE_TIME ms
			}

		} catch (JMSException e) {
			e.printStackTrace();
		} finally {
			if (connection != null) {
				try {
					connection.close();
				} catch (JMSException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static void main(final String[] args) throws InterruptedException {
		if (args.length != 1) {
			System.out.print("Usage: java Client <CLIENT_ID>");
			System.exit(1);
		}

		int id = Integer.parseInt(args[0]);
		new Client(id).start();
	}
}
