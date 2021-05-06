package it.unipr.ds.A3;

import java.util.Arrays;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;

/**
 * Class that implements a coordinator.
 **/
public class Coordinator {
	private static final String BROKER_URL = "tcp://localhost:61616";
	private static final String TOPIC_NAME = "topic";
	private final int id;

	public Coordinator(int id) {
		this.id = id;
	}

	public void start() {
		System.out.println("Coordinator " + this.id + " running");

		ActiveMQConnection connection = null;

		try {
			ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory(Coordinator.BROKER_URL);

			connection = (ActiveMQConnection) cf.createConnection();

			// We want to receive an ObjectMessage, so we explicitly declare that we trust our package
			connection.setTrustedPackages(Arrays.asList("it.unipr.ds.A3"));

			connection.start();

			// Coordinators subscribe to Client's request, and then handle them (vote the request or not)
			TopicSession session = connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

			Topic topic = session.createTopic(TOPIC_NAME);

			TopicSubscriber subscriber = session.createSubscriber(topic);

			while (true) {
				Message msg = subscriber.receive();
				
				if (msg instanceof ObjectMessage) {
					ObjectMessage objMsg = (ObjectMessage) msg;
					Request req = (Request) objMsg.getObject();

					System.out.println("Received " + req.getType() + " request from client " + req.getSenderID());

					// Now, we decide whether to vote the request or not
				}
			}
		} catch (Exception e) {
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

	public static void main(final String[] args) {
		if (args.length != 1) {
			System.out.print("Usage: java Coordinator <COORDINATOR_ID>");
			System.exit(1);
		}

		int id = Integer.parseInt(args[0]);
		new Coordinator(id).start();
	}
}
