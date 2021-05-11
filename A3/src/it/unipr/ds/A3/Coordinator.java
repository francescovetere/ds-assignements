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

	TopicSession session;
	Topic topic;
	TopicSubscriber subscriber;

	QueueSession qsession;

	int msgID = 0;

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
			session = connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
			topic = session.createTopic(TOPIC_NAME);
			subscriber = session.createSubscriber(topic);

			qsession = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);

			while (true) {
				Message msg = subscriber.receive();

				if (msg instanceof ObjectMessage) {
					ObjectMessage objMsg = (ObjectMessage) msg;
					Request req = (Request) objMsg.getObject();

					System.out.println("\tReceived " + req.getType() + " request from client " + req.getSenderID() + " (correlation id: " + msg.getJMSCorrelationID() + ")");

					// If I receive a READ or WRITE request and I can handle it, I reply with a vote
					if (req.getType() == Type.READ || req.getType() == Type.WRITE) {
						Queue senderQueue = req.getQueue();

						MessageProducer producer = qsession.createProducer(senderQueue);

						Destination tempDest = qsession.createTemporaryQueue();

						MessageConsumer consumer = qsession.createConsumer(tempDest);

						TextMessage vote = qsession.createTextMessage();
						vote.setJMSReplyTo(tempDest);

						String correlationID = "<" + this.id + ":" + this.msgID + ">";
						++msgID;
						
						vote.setJMSCorrelationID(correlationID);
						vote.setText("I vote for you!");

						System.out.println("*Sending vote to " + req.getSenderID() + " (correlation id = " + correlationID + ")");
						producer.send(vote);

						Message release = consumer.receive();

						ObjectMessage releaseObjMsg = (ObjectMessage) release;
						Request releaseReq = (Request) releaseObjMsg.getObject();

						if (releaseReq.getType() == Type.RELEASE)
							System.out.println("**Received release (correlation id = " + release.getJMSCorrelationID() + ")");
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
