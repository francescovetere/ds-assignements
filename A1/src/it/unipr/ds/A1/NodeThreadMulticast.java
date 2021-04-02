package it.unipr.ds.A1;

import java.net.Socket;
import java.util.Collections;
import java.util.List;

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

			// System.out.println("===" + " numMissing: " + node.numMissing + " numResent: " + node.numResent
			// 		+ " numLost: " + node.numLost + " numEnded: " + node.numEnded + " ===");

			synchronized (node.pool) {
				System.out.println("===" + " numMissing: " + node.numMissing + "===");
				System.out.println("===" + " queue: " + "===");
				for (int i : node.msgQueue.keySet()) {
					System.out.print(i + ": ");
					List<Message> queue = node.msgQueue.get(i);
					queue.forEach(m -> System.out.print(m.getMessageID() + " "));

					System.out.println();
				}
				System.out.println("=========");
			}

			Object obj = Utility.receive(nodeSocket);

			if (obj instanceof Message) {
				Message msg = (Message) obj;

				synchronized (node.pool) {
					System.out.println("** Received " + msg);

					if (msg.getSenderID() == node.NODE_ID)
						System.out.println("\t\tOPS\n\n");
				}

				if (msg.getBody().equals("request")) {
					Message resMsg = node.messages.get(msg.getMessageID());
					resMsg.setBody("response");

					System.out.println("************************\t\tMando: " + resMsg + "\n");

					Utility.send(nodeSocket, resMsg);
					continue;
				}

				List<Message> currentQueue = node.msgQueue.get(msg.getSenderID());
				currentQueue.add(msg);

				Collections.sort(currentQueue); // ovviamente quindi ordino, per garantire che l'ultimo elemento sia sempre il massimo

				if (msg.getBody().equals("response")) {
					synchronized (node.pool) {
						--node.numMissing;
					}

					continue;
				}

				int[] diff;

				diff = checkQueue(currentQueue, msg.getMessageID());

				if (diff != null) {
					// synchronized (node.pool) {

					for (int i = 0; i < diff.length; ++i) {
						Message reqMsg = new Message(node.NODE_ID, diff[i], "request");

						System.out.println("\t\t\t\n\n\n\n\n\n\n\n\n\n\n\n\n* Lost " + reqMsg.getMessageID() + " from "
								+ msg.getSenderID() + ": sending request for re-send: " + reqMsg
								+ "\n\n\n\n\n\n\n\n\n\n\n");

						Utility.send(nodeSocket, reqMsg);

						// Scopro che un messaggio che non mi è arrivato, quindi incremento i messaggi mancanti
						synchronized (node.pool) {
							++node.numMissing;
						}
					}
					// }

				}
			}
		}
	}

	private int[] checkQueue(List<Message> currentQueue, int currentId) {
		int[] diff;

		if (currentQueue.size() == 1) {
			if (currentId == 0) {
				System.out.println("1");
				return null;
			} else {
				System.out.println("2");
				diff = new int[currentId];
				for (int i = 0; i < diff.length; ++i)
					diff[i] = i;
			}
		}

		else {

			int lastId = currentQueue.get(currentQueue.size() - 2).getMessageID();
			diff = new int[currentId - lastId - 1];
			for (int i = 0; i < diff.length; ++i)
				diff[i] = lastId + 1;

		}

		System.out.print("Missing " + diff.length + " messages: [");
		for (int i = 0; i < diff.length; ++i)
			System.out.print(diff[i] + " ");
		System.out.print("]\n");

		return diff;
	}
}
