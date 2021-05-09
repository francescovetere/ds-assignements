package it.unipr.ds.A3;

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
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;
import javax.jms.MessageConsumer;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;

import it.unipr.ds.A3.Request.Type;

/**
 * Class that implements a coordinator.
 **/
public class Coordinator {
	private static final String BROKER_URL = "tcp://localhost:61616";
	private static final String TOPIC_NAME = "topic";
	private ActiveMQConnection connection = null;

	private final int id;

	public Coordinator(int id) {
		this.id = id;
	}

	public void start() {
		System.out.println("Coordinator " + this.id + " running");

		try {
			ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory(Coordinator.BROKER_URL);

			cf.setTrustAllPackages(true);

			this.connection = (ActiveMQConnection) cf.createConnection();

			// We want to receive an ObjectMessage, so we explicitly declare that we trust
			// our package
			// this.connection.setTrustedPackages(Arrays.asList("it.unipr.ds.A3"));

			connection.start();

			// Coordinators subscribe to Client's request, and then handle them (vote the
			// request or not)
			TopicSession session = connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

			Topic topic = session.createTopic(TOPIC_NAME);

			TopicSubscriber subscriber = session.createSubscriber(topic);

			QueueSession qsession = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);

			while (true) {
				Message msg = subscriber.receive();

				if (msg instanceof ObjectMessage) {
					ObjectMessage objMsg = (ObjectMessage) msg;
					Request req = (Request) objMsg.getObject();

					System.out.println("Received " + req.getType() + " request from client " + req.getSenderID());

					// If I receive a READ or WRITE request and I can handle it, I reply with a vote
					if (req.getType() == Type.READ || req.getType() == Type.WRITE) {
						System.out.println("I reply to " + req.getSenderID() + " with a Vote!");

						Queue senderQueue = req.getQueue();

						MessageProducer producer = qsession.createProducer(senderQueue);

						Destination tempDest = session.createTemporaryQueue();

						MessageConsumer consumer = qsession.createConsumer(tempDest);

						TextMessage vote = qsession.createTextMessage();

						vote.setJMSReplyTo(tempDest);
						// vote.setJMSCorrelationID(Integer.toString(this.id));
						vote.setText("I vote for you!");

						producer.send(vote);

						Message release = consumer.receive();

						ObjectMessage releaseObjMsg = (ObjectMessage) release;
						Request releaseReq = (Request) releaseObjMsg.getObject();

						System.out.println("Received " + releaseReq.getType() + " from client " + releaseReq.getSenderID());
					}

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
