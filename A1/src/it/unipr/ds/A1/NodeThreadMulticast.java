package it.unipr.ds.A1;

import java.net.Socket;
import java.util.Comparator;
import java.util.List;

/**
 * Class that handles the reception of new node registrations
 */
public class NodeThreadMulticast implements Runnable {

	private Node node; // reference to the Node
	private Socket nodeSocket; // this thread's socket towards a Node that sent a multicast message
	boolean verbose = true; // toggle for logs

	public NodeThreadMulticast(final Node node, final Socket nodeSocket) {
		this.node = node;
		this.nodeSocket = nodeSocket;
	}

	@Override
	public void run() {
		while (true) {
			if (verbose)
				printLog();

			synchronized (node.lock) {
				if (node.numMissing == node.numReReceived && node.numLost == node.numResent
						&& node.numReceived == (node.M) * ((node.N) - 1)) {
					
					System.out.println("Exit condition for this thread became true");
					System.out.println("TODO: Synchronization point");
				}
			}

			Object obj = Utility.receive(nodeSocket);

			if (obj instanceof Message) {
				Message msg = (Message) obj;

				if (verbose)
					synchronized (node.lock) {
						System.out.println("**Received:\n" + msg);
					}

				// If the message was a request for resend some lost message, we simply resend that message
				if (msg.getBody().equals("request")) {
					Message resMsg = node.messages.get(msg.getMessageID());
					resMsg.setBody("response");

					if (verbose)
						synchronized (node.lock) {
							System.out.println("*Send response:\n" + resMsg);
						}

					Utility.send(nodeSocket, resMsg);

					synchronized (node.lock) {
						++node.numResent;
					}

					continue;
				}

				// In every other case, we received a normal message
				synchronized (node.lock) {
					++node.numReceived;
				}

				// We add the received message to its queue
				List<Message> currentQueue = node.msgQueue.get(msg.getSenderID());
				currentQueue.add(msg);

				// We sort the queue, because resent messages will be sent most likely 
				// after all the normal messages are sent: hence, their ID will be smaller than the ID of 
				// the last element in the queue
				// Sorting the queue, allows us to always have a queue of all received messages (received + resent)
				// ordered by their ID
				currentQueue.sort(new Comparator<Message>() {
					@Override
					public int compare(Message o1, Message o2) {
						if (o1.getMessageID() < o2.getMessageID())
							return -1;
						else if (o1.getMessageID() >= o2.getMessageID())
							return 1;
						return 0;
					}

				});

				// If the message received is a response to a request for lost message, we simply go on
				// with the next iteration (we do not want to apply checkQueue())
				if (msg.getBody().equals("response")) {
					synchronized (node.lock) {
						++node.numReReceived;
					}

					continue;
				}

				// If we arrive here, we received a normal message
				// So, we check its ID against the last ID in the queue
				// If the difference between them is > 1, we lost some messages, and so we send a request for resend
				int[] diff = checkQueue(currentQueue, msg.getMessageID());

				if (diff != null) {
					if (verbose)
						synchronized (node.lock) {
							System.out.print("Missing " + diff.length + " messages: [");
							for (int i = 0; i < diff.length; ++i)
								System.out.print(diff[i] + " ");
							System.out.print("]\n");
						}

					for (int i = 0; i < diff.length; ++i) {
						Message reqMsg = new Message(node.NODE_ID, diff[i], "request");

						if (verbose)
							synchronized (node.lock) {
								System.out.println("* Lost " + reqMsg.getMessageID() + " from " + msg.getSenderID()
										+ ": sending request for re-send: " + reqMsg);
							}

						Utility.send(nodeSocket, reqMsg);

						// Scopro che un messaggio che non mi è arrivato, quindi incremento i messaggi mancanti
						synchronized (node.lock) {
							++node.numMissing;
						}
					}

				}
			}
		}
		
	}

	/**
	 * Method for printing initial log containing statistics variables and all the queues
	 */
	private synchronized void printLog() {
		System.out.println("===" + " numMissing: " + node.numMissing + " numReReceived: " + node.numReReceived
				+ " ===\n" + "=== numLost: " + node.numLost + " numResent: " + node.numResent + " ===\n"
				+ "=== numReceived: " + node.numReceived + "===");

		System.out.println("===" + " queues " + "===");
		for (int i : node.msgQueue.keySet()) {
			System.out.print(i + ": ");
			List<Message> queue = node.msgQueue.get(i);
			queue.forEach(m -> System.out.print(m.getMessageID() + " "));

			System.out.println();
		}
		System.out.println("============");
	}

	/**
	 * Method that check the difference between this ID and the queue's last element ID
	 * @param currentQueue the queue against we want to perform this backward checked
	 * @param currentId the id of the message that has just arrived
	 * @return an array of all the messages that we lost
	 */
	private int[] checkQueue(List<Message> currentQueue, int currentId) {
		int[] diff;

		if (currentQueue.size() == 1) {
			if (currentId == 0) {
				return null;
			} else {
				diff = new int[currentId];
				for (int i = 0; i < diff.length; ++i)
					diff[i] = i;
			}
		}

		else {
			int lastId = currentQueue.get(currentQueue.size() - 2).getMessageID();
			diff = new int[currentId - lastId - 1];
			for (int i = 0; i < diff.length; ++i)
				diff[i] = lastId + i + 1;

		}

		return diff;
	}
}
