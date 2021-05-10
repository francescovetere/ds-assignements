package it.unipr.ds.A3;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueReceiver;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
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
	private ActiveMQConnection connection = null;

	// Max time between two requests submit
	private static final int MAX_SLEEP = 7000;

	// Max time for receiving a coordinator vote
	private static final int MAX_RECEIVE = 1000;

	// Time for simulating a read operation
	private static final int READ_TIME = 3000;

	// Time for simulating a write operation
	private static final int WRITE_TIME = 5000;

	// List of votes (Coordinators which I've to send a RELEASE when I'm done)
	private List<TextMessage> votes = new ArrayList<>();

	// Boolean that tells if the previous client's request was successfully handled
	// (he received enough votes)
	// If it is true, we can submit a new (randomic) type of request
	// Otherwise, we must continue submitting the same type of request
	private boolean previousRequestHandled = true;

	private String properties = "config.properties";
	private int readQuorum;
	private int writeQuorum;

	TopicSession session;
	Topic topic;
	TopicPublisher publisher;

	QueueSession qsession;
	Queue queue;
	QueueReceiver receiver;

	public Client(int id) {
		this.id = id;

		System.out.println("Client " + this.id + " running");

		try {
			readQuorum = Integer.parseInt(Broker.readConfig(properties, "readQuorum"));
			writeQuorum = Integer.parseInt(Broker.readConfig(properties, "writeQuorum"));

			System.out.println("Read quorum: " + readQuorum);
			System.out.println("Write quorum: " + writeQuorum);
		} catch (NumberFormatException | IOException e) {
			e.printStackTrace();
		}
	}

	public void start() throws InterruptedException {
		// Random number, useful to decide when to submit a request, and of which type
		// the request will be
		Random random = new Random();

		// The type of the request
		Type requestType = null;

		try {
			ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory(Client.BROKER_URL);
			this.connection = (ActiveMQConnection) cf.createConnection();
			connection.start();

			// Client publish its request, waiting for subscribers (Coordinators) to handle
			// this request
			session = connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
			topic = session.createTopic(TOPIC_NAME);
			publisher = session.createPublisher(topic);

			qsession = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
			queue = qsession.createQueue("queue " + this.id);
			receiver = qsession.createReceiver(queue);

			while (true) {
				votes.clear();

				// We sleep for a random time before submitting a new request
				Thread.sleep(random.nextInt(MAX_SLEEP));

				// If the previous request received enough votes, we send either a read or a
				// write request, randomly
				// Otherwise, we continue submitting the same type of request as before
				if (previousRequestHandled)
					requestType = (random.nextDouble() < 0.5) ? Type.READ : Type.WRITE;

				String correlationID = Integer.toString(this.id);
				System.out.println("Sending " + requestType + " request to coordinators (correlation id: " + correlationID + ")");

				ObjectMessage request = session.createObjectMessage();
				request.setObject(new Request(this.id, requestType, queue));

				request.setJMSCorrelationID(correlationID);
				publisher.publish(request);

				// Now, we wait for the votes
				// If the request was a read and we reached the read quorum, we sleep for
				// READ_TIME ms
				// If the request was a write and we reached the write quorum, we sleep for
				// WRITE_TIME ms
				Message receivedMessage = null;
				while (true) {
					receivedMessage = receiver.receive(MAX_RECEIVE);
					if (receivedMessage == null)
						break;

					if (receivedMessage instanceof TextMessage) {
						System.out.println("**Received vote (correlation id: " + receivedMessage.getJMSCorrelationID() + ")");
						votes.add((TextMessage) receivedMessage); // get the temporary queue created by coordinator
					}
				}

				System.out.println("\t" + votes.size() + " coordinators voted for me");

				// If we reach the quorum for the corresponding request made,
				// we simulate the action with a sleep
				if (requestType == Type.READ && votes.size() >= readQuorum) {
					read();

					// When I've finished, I send a RELEASE message to all my voters
					release();

				} else if (requestType == Type.WRITE && votes.size() >= writeQuorum) {
					write();

					// When I've finished, I send a RELEASE message to all my voters
					release();
				}

				else {
					previousRequestHandled = false;

					System.out.println("Quorum not satisfied");
				}
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

	private void read() throws InterruptedException {
		System.out.println("***Simulating a read***");
		Thread.sleep(READ_TIME);
	}

	private void write() throws InterruptedException {
		System.out.println("***Simulating a write***");
		Thread.sleep(WRITE_TIME);
	}

	private void release() throws JMSException {
		for (TextMessage vote : votes) {
			MessageProducer producer = qsession.createProducer(null);
			ObjectMessage releaseMsg = qsession.createObjectMessage(new Request(this.id, Request.Type.RELEASE, null));

			String correlationID = Integer.toString(this.id);

			releaseMsg.setJMSCorrelationID(correlationID);

			System.out.println("*Sending release (correlation id=" + correlationID +")");
			producer.send(vote.getJMSReplyTo(), releaseMsg);
		}

		previousRequestHandled = true;

		System.out.println("*Release successfully sent to voters");
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