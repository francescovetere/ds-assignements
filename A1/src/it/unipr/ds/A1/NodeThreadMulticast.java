package it.unipr.ds.A1;

import java.net.Socket;

/**
 * Class that handles the reception of new node registrations
 */
public class NodeThreadMulticast implements Runnable {

	private Node node; // reference to the Node
	private Socket nodeSocket; // this thread's socket towards a Node that sent a multicast message

	public NodeThreadMulticast(final Node node, final Socket nodeSocket) {
		this.node = node;
		this.nodeSocket = nodeSocket;
	}

	@Override
	public void run() {

		while (true) {
			System.out.println("===" + " numMissing: " + node.numMissing + " numResent: " + node.numResent
					+ " numLost: " + node.numLost + " numEnded: " + node.numEnded + " ===");

			Object obj = Utility.receive(nodeSocket);

			if (obj instanceof Message) {
				Message msg = (Message) obj;

				// L'errore era qui: quando arrivava un messaggio di tipo "lost",
				// entrava comunque nell'if ma al momento dell'assegnamento di *lastMsgIdReceived*
				// generava un'eccezione

				// Check if the received message (except the first) is correct according to the last message in the queue
				// Also, handle the case if the queue is empty, so no message received, but the current message is != 0
				// (i.e.: we lost the first [0, ..., msg.getMessageID] messages)
				if (!node.msgQueue.get(msg.getSenderID()).isEmpty() || msg.getMessageID() != 0) {
					if (!msg.getBody().equals("lost") && !msg.getBody().equals("resent")) {

						int lastMsgIdReceived = (msg.getMessageID() == 0) ? 0
								: node.msgQueue.get(msg.getSenderID()).peekLast().getMessageID();
						int expectedMsgId = (msg.getMessageID() == 0) ? 0 : lastMsgIdReceived + 1;

						int senderExpectedMsg = (msg.getSenderID());

						// System.out.println("**********************************");
						// System.out.println("lastIDreceived: " + lastMsgIdReceived);
						// System.out.println("expectedIDreceived: " + expectedMsgId);
						// System.out.println("currentIDreceived: " + msg.getMessageID());
						// System.out.println("**********************************");

						if (msg.getMessageID() != expectedMsgId) {
							int diff = msg.getMessageID() - expectedMsgId;

							for (int i = diff; i >= 1; i--) {
								// Request message to ask which message resend
								Message reqMsg = new Message(senderExpectedMsg, msg.getMessageID() - diff, "lost");

								System.out.println("*Send request for lost message: " + reqMsg.getMessageID() + " via "
										+ nodeSocket);
								// + " to node " + reqMsg.getSenderID());

								Utility.send(nodeSocket, reqMsg);

								// Scopro che un messaggio che non mi è arrivato, quindi incremento i messaggi mancanti
								synchronized (this) {
									++node.numMissing;
								}

							}

						}
					}

				}

				/*
				//Handle the case if was arrived the first message but its ID isn't 0 
				else if (node.msgQueue.get(msg.getSenderID()).isEmpty() & msg.getMessageID() != 0) {
					if (!msg.getBody().equals("lost") && !msg.getBody().equals("resent")) {
				
						int senderExpectedMsg = (msg.getSenderID());
				
						for (int i = 0; i < msg.getMessageID(); i++) {
							// Request message to ask which message resend
							Message reqMsg = new Message(senderExpectedMsg, i, "lost");
				
							System.out.println(
									"*Send request for lost message: " + reqMsg.getMessageID() + " via " + nodeSocket);
							// + " to node " + reqMsg.getSenderID());
				
							Utility.send(nodeSocket, reqMsg);
				
							// Scopro che un messaggio che non mi è arrivato, quindi incremento i messaggi mancanti
							synchronized (this) {
								++node.numMissing;
							}
						}
					}
				}
				*/

				node.msgQueue.get(msg.getSenderID()).add(msg);

				if (msg.getBody().equals("lost")) { // TODO: Not working properly...

					// Immediately resend requested message
					// Invio un messaggio che non era arrivato ad un nodo, quindi incremento i messaggi reinviati
					synchronized (this) {
						++node.numResent;
					}

					System.out.println("**Received request to resend message: " + msg.getMessageID()
					// + " from node " + msg.getSenderID()
							+ " via " + nodeSocket + "\n*Send previously lost message: " + msg.getMessageID()
							+ " to node " + msg.getSenderID());

					msg.setBody("resent");

					Utility.send(nodeSocket, msg);

				} else if (msg.getBody().equals("end")) { // TODO: Not working properly... (?)
					System.out.println("**Received termination message: " + msg.getMessageID()
					// + " from node " + msg.getSenderID());
							+ " via " + nodeSocket);

					// Ricevo un messaggio di end, quindi incremento il numero di nodi che mi hanno segnalato di aver finito
					synchronized (this) {
						++node.numReceived;
						++node.numEnded;
					}

				} else if (msg.getBody().equals("resent")) { // TODO: Not working properly... (?)
					// Ricevo un messaggio che non mi era arrivato, quindi decremento i messaggi mancanti
					synchronized (this) {
						--node.numMissing;
					}

					System.out.println("**Received previously lost message: " + msg.getMessageID()
					// + " from node " + msg.getSenderID());
							+ " via " + nodeSocket);

				} else {
					synchronized (this) {
						++node.numReceived;
					}

					System.out.println("**Received message: " + msg.getMessageID() + " from node " + msg.getSenderID());
				}
			}

			// Se non mi manca alcun messaggio da ricevere dagli altri nodi
			// E se ho inviato reinviato agli altri nodi tutti i messaggi che avevo inzialmente perso
			// E se ho ricevuto N - 1 messaggi di end
			// ===> allora posso uscire dal while
			if ((node.numMissing == 0) && (node.numResent == node.numLost) && (node.numEnded == (node.N)/2)) {
				System.out.println("*** Per me si può chiudere ***");
				break;
			}
		}

		// System.out.println("Exited from while loop");

	}
}
