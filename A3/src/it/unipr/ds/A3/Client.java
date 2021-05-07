package it.unipr.ds.A3;

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
	private static final int MAX_SLEEP = 5000;

	// Time for simulating a read operation
	private static final int READ_TIME = 3000;

	// Time for simulating a write operation
	private static final int WRITE_TIME = 5000;

	// List of voters (Coordinators which I've to send a RELEASE when I'm done)
	private List<Destination> voters = new ArrayList<>();

	// Counter for the received votes
	private int votes = 0;

	// TODO: read this values from a property file - generate dinamically
	private int readQuorum = 1;
	private int writeQuorum = 2;


	public Client(int id) {
		this.id = id;
	}

	public void start() throws InterruptedException {
		System.out.println("Client " + this.id + " running");

		// Random number, useful for decide when to submit a request, and of which type the request will be
		Random random = new Random();

		// The type of the request
		Type requestType = null;

		try {
			ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory(Client.BROKER_URL);
			this.connection = (ActiveMQConnection) cf.createConnection();
			connection.start();

			// Client publish its request, waiting for subscribers (Coordinators) to handle this request
			TopicSession session = connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

			Topic topic = session.createTopic(TOPIC_NAME);

			TopicPublisher publisher = session.createPublisher(topic);

			//This session is used for point-to-point communication
			QueueSession qsession = this.connection.createQueueSession(
				false, Session.AUTO_ACKNOWLEDGE);
	  
			Queue queue = qsession.createQueue("queue" + this.id);
	  
			QueueReceiver receiver = qsession.createReceiver(queue);

			// MessageProducer producer = qsession.createProducer(null);


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
				msg.setObject(new Request(this.id, requestType, queue));

				publisher.publish(msg);

				// Now, we wait for the votes
				// If the request was a read and we reached the read quorum, we sleep for READ_TIME ms
				// If the request was a write and we reached the write quorum, we sleep for WRITE_TIME ms

				Message receiveMessage = receiver.receive(1000);
				while(receiveMessage != null) {
					// voters.add(receiveMessage.getJMSReplyTo());

					// I received a vote, so increment its value
					votes++;

					receiveMessage = receiver.receive(1000);
				}
				
				// If we reach the quorum for the corresponding request made,
				// we simulate the action with a sleep
				if(requestType==Type.READ && votes >= readQuorum) {
					Thread.sleep(READ_TIME);

					// When I've finished, I send a RELEASE message to all my voters
					// for (Destination destination : voters) {
					// 	producer.send(destination, qsession.createTextMessage("RELEASE!"));
					// }

					//With this solution I release everybody, which is not correct
					// TODO: Implement the RELEASE phase
					msg = session.createObjectMessage();

					msg.setObject(new Request(this.id, Request.Type.RELEASE, queue));
		  
					publisher.publish(msg);

				}
				else if(requestType == Type.WRITE && votes >= writeQuorum) {
					Thread.sleep(WRITE_TIME);

					// When I've finished, I send a RELEASE message to all my voters
					// for (Destination destination : voters) {
					// 	producer.send(destination, qsession.createTextMessage("RELEASE!"));
					// }

					// With this solution I release everybody, which is not correct
					// TODO: Implement the RELEASE phase 
					msg = session.createObjectMessage();

					msg.setObject(new Request(this.id, Request.Type.RELEASE, queue));
		  
					publisher.publish(msg);
				} 

				votes = 0;
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
