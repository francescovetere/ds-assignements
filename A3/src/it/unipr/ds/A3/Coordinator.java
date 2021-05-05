package it.unipr.ds.A3;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueReceiver;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerFactory;
import org.apache.activemq.broker.BrokerService;

/**
 *
 * Class providing an implementation of a receiver.
 *
 **/

public class Coordinator {
	private static final String BROKER_URL = "tcp://localhost:61616";
	private static final String BROKER_PROPS = "persistent=false&useJmx=false";
	private static final String QUEUE_NAME = "queue";

	/**
	 * Receives a sequence of messages.
	 *
	 **/
	public void receive() {
		ActiveMQConnection connection = null;
		try {
			BrokerService broker = BrokerFactory.createBroker("broker:(" + BROKER_URL + ")?" + BROKER_PROPS);

			broker.start();

			ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory(Coordinator.BROKER_URL);

			connection = (ActiveMQConnection) cf.createConnection();

			connection.start();

			QueueSession session = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);

			Queue queue = session.createQueue(Coordinator.QUEUE_NAME);

			QueueReceiver receiver = session.createReceiver(queue);

			while (true) {
				Message message = receiver.receive();

				if (message instanceof TextMessage) {
					System.out.println("Message: " + ((TextMessage) message).getText());
				} else {
					break;
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

	/**
	 * Starts the coordinator.
	 *
	 * @param args
	 *
	 *             It does not need arguments.
	 *
	 **/
	public static void main(final String[] args) {
		new Coordinator().receive();
	}
}
