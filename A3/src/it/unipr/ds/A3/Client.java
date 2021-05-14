package it.unipr.ds.A3;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
 * Class that implements a client
 * 
 * A client publishes a read or write request to the topic, waiting for votes
 * When votes arrives, he evalutes if these votes are enough to reach the quorum:
 * If it is so, it performs its operation, then sends a release to all its voters
 *
 * Then, the client is ready to perform new requests: if the previous request was successfull,
 * he randomly changes the type of its request
 * Otherwise, he keeps submitting the same type of request
 **/
public class Client {
	private static final String BROKER_URL = "tcp://localhost:61616";
	private static final String TOPIC_NAME = "topic";
	private final int id;
	private ActiveMQConnection connection = null;

	// Max time between two requests submit
	private static final int MAX_SLEEP = 7000;

	// Max time waiting for receiving a vote
	private static final int MAX_RECEIVE = 5000;

	// Time for simulating a read operation
	private static final int READ_TIME = 3000;

	// Time for simulating a write operation
	private static final int WRITE_TIME = 5000;

	// List of votes (Coordinators to which I need to send a RELEASE when I'm done)
	private List<TextMessage> votes = new ArrayList<>();

	// Boolean that tells if the previous client's request was successfully handled
	// (he received enough votes)
	// If it is true, we can submit a new (randomic) type of request
	// Otherwise, we must continue submitting the same type of request
	private boolean previousRequestHandled = true;

	// Quorums are read from a configuration file
	private String properties = "config.properties";

	private int readQuorum;
	private int writeQuorum;

	TopicSession session;
	Topic topic;
	TopicPublisher publisher;

	QueueSession qsession;
	Queue queue;
	QueueReceiver receiver;

	int msgID = 0;

	/**
	* Class constructor
	* @param id Client's id
	*/
	public Client(int id) {
		this.id = id;

		System.out.println("Client " + this.id + " running");

		// We read quorums from a configuration file
		try {
			readQuorum = Integer.parseInt(Broker.readConfig(properties, "readQuorum"));
			writeQuorum = Integer.parseInt(Broker.readConfig(properties, "writeQuorum"));

			System.out.println("Read quorum: " + readQuorum);
			System.out.println("Write quorum: " + writeQuorum);
		} catch (NumberFormatException | IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Method that starts the client's execution
	 */
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

			// Client publishes its request, waiting for subscribers (Coordinators) to handle
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

				String correlationID = "<" + this.id + ":" + this.msgID + ">";
				++msgID;

				System.out
						.println("Sending " + requestType + " request to coordinators (correlation id: " + correlationID + ")");

				ObjectMessage request = session.createObjectMessage();
				request.setObject(new Request(this.id, requestType, queue));

				request.setJMSCorrelationID(correlationID);
				publisher.publish(request);

				// Now, we wait for the votes
				// If the request was a read and we reached the read quorum, we sleep for READ_TIME ms
				// If the request was a write and we reached the write quorum, we sleep for WRITE_TIME ms
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
				if (requestType.equals(Type.READ) && votes.size() >= readQuorum) {
					if (votes.size() > readQuorum) {
						// We release only the first votes.size() - readQuorum voters
						System.out.println("\t\t" + (votes.size() - readQuorum) + " exceeding voters");
						List<TextMessage> toBeReleased = votes.subList(0, votes.size() - readQuorum);
						release(toBeReleased);
						// votes.subList(0, votes.size() - readQuorum).clear();
						// toBeReleased.clear();
					}

					read();

					// When I've finished, I send a RELEASE message to all my voters
					release(votes);

					previousRequestHandled = true;

				} else if (requestType.equals(Type.WRITE) && votes.size() >= writeQuorum) {
					write();

					// When I've finished, I send a RELEASE message to all my voters
					release(votes);

					previousRequestHandled = true;

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

	/**
	 * Simulates a read operation
	 * @throws InterruptedException
	 */
	private void read() throws InterruptedException {
		System.out.println("***Simulating a read***");
		Thread.sleep(READ_TIME);
	}

	/**
	 * Simulates a write operation
	 * @throws InterruptedException
	 */
	private void write() throws InterruptedException {
		System.out.println("***Simulating a write***");
		Thread.sleep(WRITE_TIME);
	}

	/**
	 * Release all the voters contained in the list votes
	 * @param votes The list of votes sent by voters to be released
	 * @throws JMSException
	 */
	private void release(List<TextMessage> votes) throws JMSException {
		System.out.println("\t\tI release " + votes.size() + " voters");
		for (int i = 0; i < votes.size(); ++i) {
			MessageProducer producer = qsession.createProducer(null);
			ObjectMessage releaseMsg = qsession.createObjectMessage(new Request(this.id, Request.Type.RELEASE, null));

			String correlationID = "<" + this.id + ":" + this.msgID + ">";
			++msgID;

			releaseMsg.setJMSCorrelationID(correlationID);

			System.out.println("*Sending release (correlation id = " + correlationID + ")");
			producer.send(this.votes.get(i).getJMSReplyTo(), releaseMsg);

			// votes.remove(i);
		}

		votes.clear();
	}

	/**
	 * Main method
	 * @param args Vector of arguments
	 * 						 In particular: 
	 * 						 <CLIENT_ID>: a positive integer representing this Client's ID\n"
	 * @throws InterruptedException
	 */
	public static void main(final String[] args) throws InterruptedException {
		if (args.length != 1) {
			System.out.print("Usage: java -classpath bin:lib/activemq-all-5.16.1.jar it.unipr.ds.A3.Client <CLIENT_ID> \n\t"
					+ "where:\n" + "\t<CLIENT_ID>: a positive integer representing this Client's ID\n");
			System.exit(1);
		}

		int id = Integer.parseInt(args[0]);
		new Client(id).start();
	}
}